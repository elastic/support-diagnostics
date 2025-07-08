# Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
# or more contributor license agreements. Licensed under the Elastic License
# 2.0; you may not use this file except in compliance with the Elastic License
# 2.0.

import argparse
import json
import os
import tarfile
from datetime import datetime
from pathlib import Path
from unittest import mock

import pytest

from .. import export_model_snapshot as ems


# --------------------------------------------------------------------------- #
#  Fixtures & helpers
# --------------------------------------------------------------------------- #
@pytest.fixture(autouse=True)
def _set_cwd(tmp_path, monkeypatch):
    """Run each test inside an isolated temporary working directory."""
    monkeypatch.chdir(tmp_path)
    return tmp_path


@pytest.fixture
def dummy_es():
    """
    Return a MagicMock that behaves enough like an Elasticsearch client
    for the functions under test.
    """
    es = mock.MagicMock(name="Elasticsearch")
    es.ml = mock.MagicMock()
    return es


@pytest.fixture
def passthrough_tqdm(monkeypatch):
    """Turn tqdm.tqdm into a no‑op generator for speed & determinism."""
    monkeypatch.setattr(ems, "tqdm", lambda x, **_: x)


# --------------------------------------------------------------------------- #
#  Pure‑function tests (no I/O, no ES)
# --------------------------------------------------------------------------- #
def test_sanitize_filename():
    # three leading "bad" chars (., ., /) → 3 “_”;  * and ? → “_” each; “.” before txt → “_”
    assert ems.sanitize_filename("../weird*name?.txt") == "___weird_name__txt"


def test_validate_date_roundtrip():
    ts = "2025-01-02T03:04:05"
    dt = ems.validate_date(ts)
    assert dt == datetime(2025, 1, 2, 3, 4, 5)
    # should keep tz‑naïve; no implicit UTC/local conversions
    assert dt.tzinfo is None


@pytest.mark.parametrize("bad", ["2025/01/02", "not‑a‑date", "2025-13-01T00:00:00"])
def test_validate_date_bad(bad):
    # calling the helper directly raises the underlying ArgumentTypeError
    with pytest.raises(argparse.ArgumentTypeError):
        ems.validate_date(bad)


def test_extract_possible_field_names_strips_keyword_suffix():
    query = {
        "bool": {
            "must": [
                {"match": {"user.keyword": "bob"}},
                {"range": {"@timestamp": {"gte": "now-1d"}}},
            ]
        }
    }
    names = ems.extract_possible_field_names(query)
    # the helper treats the range‑operator keys (gte/lte/…) as “fields”, so include them
    assert names == {"user", "@timestamp", "gte"}


# --------------------------------------------------------------------------- #
#  File/ES‑interacting helpers
# --------------------------------------------------------------------------- #
def _make_doc(idx, _id, **source):
    return {"_index": idx, "_id": _id, "_source": source or {"foo": "bar"}}


def test_save_snapshots_creates_file(tmp_path, dummy_es, passthrough_tqdm, monkeypatch):
    docs = [_make_doc("idx", f"id{i}") for i in range(2)]

    monkeypatch.setattr(ems.helpers, "scan", lambda *_, **__: docs)

    filename = ems.save_snapshots(
        job_id="jobA",
        snapshot_id="snap1",
        es_client=dummy_es,
        snapshot_doc_count=len(docs),
    )

    assert filename is not None
    out = Path(filename)
    assert out.is_file()

    # Two lines per doc (action line + source line)
    lines = out.read_text(encoding="utf-8").splitlines()
    assert len(lines) == 2 * len(docs)
    # First action line should reference the stubbed doc ID (“id0”)
    assert '"_id": "id0"' in lines[0]


def test_save_snapshot_stats_writes_json(tmp_path, dummy_es):
    dummy_es.search.return_value = {
        "hits": {"hits": [{"_id": "abc123", "_source": {"count": 3}}]}
    }

    filename = ems.save_snapshot_stats("jobA", "snap1", dummy_es)
    assert filename and Path(filename).is_file()

    data = json.loads(Path(filename).read_text())
    assert data == {"count": 3}
    dummy_es.search.assert_called_once()


def test_save_job_config_success(tmp_path, dummy_es):
    dummy_es.ml.get_jobs.return_value = {
        "count": 1,
        "jobs": [{"job_id": "jobA", "analysis_config": {}, "datafeed_config": {}}],
    }

    filename, cfg = ems.save_job_config("jobA", dummy_es)
    assert filename and Path(filename).is_file()
    assert cfg["job_id"] == "jobA"
    dummy_es.ml.get_jobs.assert_called_once_with(job_id="jobA")


def test_save_annotations(tmp_path, dummy_es):
    hits = [{"_source": {"note": f"n{i}"}} for i in range(3)]
    dummy_es.search.return_value = {"hits": {"hits": hits}}

    filename = ems.save_annotations(
        job_id="jobA",
        before_date=datetime(2025, 1, 1),
        after_date=datetime(2024, 12, 31),
        es_client=dummy_es,
    )

    assert filename and Path(filename).is_file()
    lines = Path(filename).read_text().splitlines()
    assert len(lines) == len(hits)
    assert json.loads(lines[0]) == {"note": "n0"}


def _minimal_job_config():
    return {
        "job_id": "jobA",
        "datafeed_config": {"indices": "idx‑*", "query": {"match_all": {}}},
        "data_description": {"time_field": "@timestamp"},
        "analysis_config": {
            "detectors": [{"function": "count", "field_name": "value"}],
            "influencers": ["user"],
        },
    }


def test_save_inputs(tmp_path, dummy_es, monkeypatch, passthrough_tqdm):
    """Test saving of input data for an ML job to validate input file creation."""
    cfg = _minimal_job_config()
    docs = [
        _make_doc("idx‑1", f"id{i}", value=i, user="me", **{"@timestamp": 0})
        for i in range(2)
    ]
    
    # Mock the PIT API calls
    dummy_es.open_point_in_time.return_value = {"id": "test-pit-id"}
    dummy_es.close_point_in_time.return_value = {"succeeded": True}
    
    # First search response with sort values for search_after
    dummy_es.search.side_effect = [
        {
            "hits": {
                "hits": [
                    {
                        "_index": doc["_index"],
                        "_id": doc["_id"],
                        "_source": doc["_source"],
                        "sort": [0, i]  # [timestamp_val, seq_no]
                    } for i, doc in enumerate(docs)
                ]
            }
        },
        # Empty response to end the loop
        {"hits": {"hits": []}}
    ]

    filenames = ems.save_inputs(
        job_config=cfg,
        before_date=None,
        after_date=None,
        es_client=dummy_es,
    )

    # Handle case where filenames might be None
    if filenames is None:
        pytest.fail("Expected save_inputs to return a list of filenames, got None")
    
    # Verify the PIT was opened and closed
    dummy_es.open_point_in_time.assert_called_once()
    dummy_es.close_point_in_time.assert_called_once()
    
    # Verify search was called with appropriate parameters
    assert dummy_es.search.call_count == 2
    # First call should include PIT ID
    first_call_body = dummy_es.search.call_args_list[0][1]["body"]
    assert first_call_body["pit"]["id"] == "test-pit-id"
    
    # There should be two calls to search, one for the initial fetch and one for the continuation
    assert len(dummy_es.search.call_args_list) == 2
        
    for filename in filenames:
        assert filename and Path(filename).is_file()
        assert len(Path(filename).read_text().splitlines()) == 2 * len(
            docs
        )  # action+source


def test_create_archive_captures_and_removes_files(tmp_path):
    # Prepare three temp files
    files = []
    for i in range(3):
        f = tmp_path / f"f{i}.txt"
        f.write_text(f"hello {i}")
        files.append(str(f))

    ems.create_archive("jobA", files)

    tar_file = tmp_path / "jobA_state.tar.gz"
    assert tar_file.is_file()

    with tarfile.open(tar_file, "r:gz") as tar:
        assert sorted(m.name for m in tar.getmembers()) == sorted(
            os.path.basename(f) for f in files
        )

    # Original files should have been removed
    for f in files:
        assert not Path(f).exists()


def test_get_snapshot_info_success(dummy_es):
    dummy_es.ml.get_model_snapshots.return_value = {
        "count": 1,
        "model_snapshots": [
            {"snapshot_id": "snap1", "snapshot_doc_count": 42},
        ],
    }
    result = ems.get_snapshot_info(dummy_es, "jobA")
    assert result is not None
    snap_id, doc_count = result
    assert snap_id == "snap1" and doc_count == 42


def test_get_snapshot_info_none(dummy_es):
    dummy_es.ml.get_model_snapshots.return_value = {"count": 0, "model_snapshots": []}
    assert ems.get_snapshot_info(dummy_es, "jobA") is None
