# Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
# or more contributor license agreements. Licensed under the Elastic License
# 2.0; you may not use this file except in compliance with the Elastic License
# 2.0.

import io
import json
import tarfile
import time
from unittest import mock

import pytest

from .. import import_model_snapshot as ims


# --------------------------------------------------------------------------- #
#  Fixtures & helpers
# --------------------------------------------------------------------------- #
@pytest.fixture(autouse=True)
def _set_cwd(tmp_path, monkeypatch):
    """Run every test in its own temporary working directory."""
    monkeypatch.chdir(tmp_path)
    return tmp_path


@pytest.fixture
def dummy_es():
    """A stub Elasticsearch client that tracks calls."""
    es = mock.MagicMock(name="Elasticsearch")
    es.ml = mock.MagicMock()
    es.indices = mock.MagicMock()
    return es


# --------------------------------------------------------------------------- #
#  Pure helpers
# --------------------------------------------------------------------------- #
def test_sanitize_filename():
    # six leading bad chars (“.. / .. /”) → 6 underscores
    assert ims.sanitize_filename("../../abc*?") == "______abc__"


def test_is_within_directory_true_false(tmp_path):
    base = tmp_path / "base"
    base.mkdir()
    good = base / "file.txt"
    bad = tmp_path / "evil.txt"
    assert ims.is_within_directory(base, good)
    assert not ims.is_within_directory(base, bad)


# --------------------------------------------------------------------------- #
#  safe_extract & archive helpers
# --------------------------------------------------------------------------- #
def test_safe_extract_blocks_path_traversal(tmp_path):
    tar_path = tmp_path / "traversal.tar.gz"
    # Create tar with a file trying to escape via "../"
    with tarfile.open(tar_path, "w:gz") as tar:
        info = tarfile.TarInfo(name="../evil.txt")
        data = b"bad"
        info.size = len(data)
        tar.addfile(info, io.BytesIO(data))

    with tarfile.open(tar_path, "r:gz") as tar:
        with pytest.raises(Exception, match="Path Traversal"):
            ims.safe_extract(tar, path=tmp_path)


def test_extract_archive_success(tmp_path):
    tar_path = tmp_path / "good.tar.gz"
    inner_name = "hello.txt"
    data = b"hi"
    # Build a minimal archive
    with tarfile.open(tar_path, "w:gz") as tar:
        info = tarfile.TarInfo(name=inner_name)
        info.size = len(data)
        tar.addfile(info, io.BytesIO(data))

    out_dir = tmp_path / "out"
    out_dir.mkdir()
    extracted = ims.extract_archive(str(tar_path), str(out_dir))

    assert extracted == [str(out_dir / inner_name)]
    assert (out_dir / inner_name).read_bytes() == data


# --------------------------------------------------------------------------- #
#  generate_actions
# --------------------------------------------------------------------------- #
def test_generate_actions_reads_pairs(tmp_path):
    f = tmp_path / "sample.ndjson"
    # two docs -> 4 lines (action+src repeated)
    with f.open("w", encoding="utf-8") as fh:
        for i in range(2):
            fh.write(json.dumps({"index": {"_index": "old", "_id": f"id{i}"}}) + "\n")
            fh.write(json.dumps({"x": i}) + "\n")

    actions = list(ims.generate_actions(str(f), "new‑idx"))
    assert len(actions) == 2
    assert actions[0]["_id"] == "id0"
    assert actions[0]["_index"] == "new‑idx"
    assert actions[1]["_source"] == {"x": 1}


# --------------------------------------------------------------------------- #
#  upload_data & index helpers
# --------------------------------------------------------------------------- #
def test_upload_data_calls_bulk(tmp_path, dummy_es, monkeypatch):
    ndjson = tmp_path / "valid.ndjson"
    # write one *valid* action+doc pair so generate_actions does not raise
    ndjson.write_text(
        json.dumps({"index": {"_index": "old", "_id": "42"}})
        + "\n"
        + json.dumps({"foo": "bar"})
        + "\n"
    )

    mock_bulk = mock.Mock(return_value=(1, []))
    monkeypatch.setattr(ims.helpers, "bulk", mock_bulk)

    ims.upload_data(dummy_es, "idx", str(ndjson))

    mock_bulk.assert_called_once()
    # check that the kwargs we care about are forwarded
    _, kwargs = mock_bulk.call_args
    assert kwargs["index"] == "idx"
    assert kwargs["chunk_size"] == 1000


def test_create_input_index_delete_then_create(dummy_es):
    dummy_es.indices.exists.return_value = True
    ims.create_input_index(dummy_es, "idx")
    dummy_es.indices.delete.assert_called_once_with(index="idx")
    dummy_es.indices.create.assert_called_once_with(index="idx")


# --------------------------------------------------------------------------- #
#  create_job_config
# --------------------------------------------------------------------------- #
def _job_cfg():
    return {
        "job_id": "jobA",
        "analysis_config": {},
        "data_description": {},
        "datafeed_config": {"indices": ["orig"], "authorization": {}, "job_id": "jobA"},
    }


def test_create_job_config_success(dummy_es):
    # Simulate "job already exists" so delete path executes
    dummy_es.ml.get_jobs.return_value = {"count": 1}
    dummy_es.ml.put_job.return_value = {"job_id": "jobA"}

    job_id = ims.create_job_config(dummy_es, _job_cfg(), new_index="new‑idx")
    assert job_id == "jobA"

    # Authorization & job_id should be stripped; indices replaced
    body = dummy_es.ml.put_job.call_args.kwargs["body"]
    assert body["datafeed_config"]["indices"] == ["new‑idx"]
    assert "authorization" not in body["datafeed_config"]
    assert "job_id" not in body["datafeed_config"]


def test_create_job_config_missing_id_returns_none(dummy_es):
    cfg = _job_cfg()
    del cfg["job_id"]
    assert ims.create_job_config(dummy_es, cfg) is None


# --------------------------------------------------------------------------- #
#  load_snapshot_stats
# --------------------------------------------------------------------------- #
def test_load_snapshot_stats_indexes_doc(tmp_path, dummy_es):
    stats_file = tmp_path / "ml-anomalies-snapshot_doc_abc.json"
    stats_file.write_text(json.dumps({"snapshot_id": "snap1"}))

    dummy_es.index.return_value = {"_id": "abc"}
    snap_id = ims.load_snapshot_stats(dummy_es, str(stats_file))

    assert snap_id == "snap1"
    dummy_es.index.assert_called_once()
    args, kwargs = dummy_es.index.call_args
    assert kwargs["index"] == ".ml-anomalies-shared"


# --------------------------------------------------------------------------- #
#  find_file
# --------------------------------------------------------------------------- #
def test_find_file_locates(tmp_path):
    files = [str(tmp_path / f"f{i}.txt") for i in range(3)]
    assert ims.find_file("f1.txt", files) == files[1]
    assert ims.find_file("none.txt", files) is None


# --------------------------------------------------------------------------- #
#  import_model_state (happy path)
# --------------------------------------------------------------------------- #
def test_import_model_state_happy_path(tmp_path, dummy_es, monkeypatch):
    job_id = "jobA"
    safe_id = ims.sanitize_filename(job_id)
    out_dir = tmp_path / f"extracted_{safe_id}"
    out_dir.mkdir()

    # Prepare minimal files expected by the importer
    cfg_file = out_dir / f"{safe_id}_config.json"
    cfg_file.write_text(json.dumps(_job_cfg()))
    snap_docs = out_dir / f"{safe_id}_snapshot_docs.ndjson"
    snap_docs.write_text("{}\n{}\n")
    input_file = out_dir / f"{safe_id}_input.ndjson"
    input_file.write_text("{}\n{}\n")
    snap_stats = out_dir / "ml-anomalies-snapshot_doc_1.json"
    snap_stats.write_text(json.dumps({"snapshot_id": "snap1"}))

    extracted = list(map(str, [cfg_file, snap_docs, input_file, snap_stats]))

    # Patch helpers used inside import_model_state
    monkeypatch.setattr(ims, "extract_archive", lambda ap, od: extracted)
    monkeypatch.setattr(ims, "create_input_index", mock.Mock())
    monkeypatch.setattr(ims, "upload_data", mock.Mock())
    monkeypatch.setattr(ims, "create_job_config", mock.Mock(return_value=job_id))
    monkeypatch.setattr(ims, "load_snapshot_stats", mock.Mock(return_value="snap1"))
    monkeypatch.setattr(time, "sleep", lambda _: None)  # skip waiting

    ims.import_model_state(job_id, dummy_es, "dummy_archive.tar.gz")

    ims.create_input_index.assert_called_once()
    ims.upload_data.assert_any_call(dummy_es, f"{safe_id}-input", str(input_file))
    ims.upload_data.assert_any_call(dummy_es, ".ml-state-write", str(snap_docs))
    ims.load_snapshot_stats.assert_called_once_with(dummy_es, str(snap_stats))
    dummy_es.ml.revert_model_snapshot.assert_called_once_with(
        job_id=job_id, snapshot_id="snap1"
    )
