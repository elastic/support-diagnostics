# Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
#  or more contributor license agreements. Licensed under the Elastic License
#  2.0; you may not use this file except in compliance with the Elastic License
#  2.0.

import argparse
import json
import os
from pathlib import Path
import re
import tarfile
from datetime import datetime
from getpass import getpass
from typing import Any, Dict, Iterator, List, Optional, Set, Tuple

import urllib3
from elasticsearch import ApiError, Elasticsearch, TransportError, helpers
from loguru import logger
from tqdm import tqdm

# Disable noisy warning about missing certificate verification
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# Constants
KNOWN_OPERATORS = {
    "query",
    "bool",
    "must",
    "should",
    "filter",
    "must_not",
    "match",
    "term",
    "terms",
    "range",
    "exists",
    "missing",
    "wildcard",
    "regexp",
    "fuzzy",
    "prefix",
    "multi_match",
    "match_all",
    "match_phrase",
    "match_phrase_prefix",
    "simple_query_string",
    "common",
    "ids",
    "constant_score",
    "dis_max",
    "function_score",
    "nested",
    "has_child",
    "has_parent",
    "more_like_this",
    "script",
    "percolate",
    "geo_shape",
    "geo_bounding_box",
    "geo_distance",
    "geo_polygon",
    "shape",
    "parent_id",
    "boosting",
    "indices",
    "span_term",
    "span_multi",
    "span_first",
    "span_near",
    "span_or",
    "span_not",
    "span_containing",
    "span_within",
    "span_field_masking",
}


def sanitize_filename(name: str) -> str:
    """Sanitize the filename to prevent directory traversal and other security issues."""
    return re.sub(r"[^a-zA-Z0-9_-]", "_", name)


def validate_date(date_text: str) -> datetime:
    """Validate and parse date string in the format YYYY-MM-DDTHH:MM:SS."""
    try:
        return datetime.strptime(date_text, "%Y-%m-%dT%H:%M:%S")
    except ValueError as e:
        raise argparse.ArgumentTypeError(
            f"Invalid date format: '{date_text}'. Expected format: YYYY-MM-DDTHH:MM:SS"
        ) from e


def save_snapshots(
    job_id: str, snapshot_id: str, es_client: Elasticsearch, snapshot_doc_count: int
) -> Optional[str]:
    """
    Extract the model state from Elasticsearch based on the given parameters.

    Args:
        job_id (str): The ID of the job.
        snapshot_id (str): The ID of the snapshot.
        es_client (Elasticsearch): The Elasticsearch client.
        snapshot_doc_count (int): The number of snapshot documents to extract.

    Returns:
        Optional[str]: The filename of the saved snapshot documents, or None if failed.
    """
    index_pattern = ".ml-state-*"
    safe_job_id = sanitize_filename(job_id)
    filename = f"{safe_job_id}_snapshot_docs.ndjson"
    logger.info(f"Writing the compressed model state to {filename}")
    all_ids = [
        f"{job_id}_model_state_{snapshot_id}#{i + 1}" for i in range(snapshot_doc_count)
    ]
    num_docs = 0

    try:
        with open(filename, "w", encoding="utf-8") as f:
            for doc in tqdm(
                helpers.scan(
                    es_client,
                    index=index_pattern,
                    query={"query": {"terms": {"_id": all_ids}}},
                    size=1000,
                ),
                total=snapshot_doc_count,
                desc="Saving snapshots",
            ):
                action = {"index": {"_index": doc["_index"], "_id": doc["_id"]}}
                f.write(json.dumps(action) + "\n")
                f.write(json.dumps(doc["_source"]) + "\n")
                num_docs += 1
        logger.info(
            f"{num_docs} snapshot documents for job {job_id} stored in {filename}"
        )
        return filename
    except (ApiError, TransportError, IOError) as e:
        logger.error(f"Failed to save snapshots: {e}")
        return None


def save_snapshot_stats(
    job_id: str, snapshot_id: str, es_client: Elasticsearch
) -> Optional[str]:
    """
    Retrieves the total number of snapshot documents for the given job and snapshot ID.

    Args:
        job_id (str): The ID of the job.
        snapshot_id (str): The ID of the snapshot.
        es_client (Elasticsearch): The Elasticsearch client.

    Returns:
        Optional[str]: The filename containing the snapshot statistics, or None if failed.
    """
    index = ".ml-anomalies-shared"
    search_query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"job_id": job_id}},
                    {"term": {"snapshot_id": snapshot_id}},
                ]
            }
        }
    }
    try:
        response = es_client.search(index=index, body=search_query)
        hits = response.get("hits", {}).get("hits", [])
        if hits:
            result_doc = hits[0]["_source"]
            id_ = sanitize_filename(hits[0]["_id"])
            file_name = f"ml-anomalies-snapshot_doc_{id_}.json"
            with open(file_name, "w", encoding="utf-8") as f:
                json.dump(result_doc, f, indent=4)
            logger.info(f"Snapshot document count stored in {file_name}")
            return file_name
        else:
            logger.error(
                "No snapshot document found for the given job_id and snapshot_id."
            )
            return None
    except (ApiError, TransportError) as e:
        logger.error(f"Error retrieving snapshot stats: {e}")
        return None


def save_job_config(
    job_id: str, es_client: Elasticsearch
) -> Tuple[Optional[str], Optional[Dict[str, Any]]]:
    """
    Retrieves the job configuration using the Elasticsearch anomaly detection job API.

    Args:
        job_id (str): The ID of the job.
        es_client (Elasticsearch): The Elasticsearch client.

    Returns:
        Tuple[Optional[str], Optional[Dict[str, Any]]]: The filename containing the job
        configuration and the job configuration dictionary, or (None, None) if failed.
    """
    try:
        response = es_client.ml.get_jobs(job_id=job_id)
        if response.get("count", 0) > 0:
            config = response["jobs"][0]
            safe_job_id = sanitize_filename(job_id)
            file_name = f"{safe_job_id}_config.json"
            with open(file_name, "w", encoding="utf-8") as f:
                json.dump(config, f, indent=4)
            logger.info(f"Job configuration for job {job_id} stored in {file_name}")
            return file_name, config
        else:
            logger.error(f"No job configuration found for job_id: {job_id}")
            return None, None
    except (ApiError, TransportError) as e:
        logger.error(
            f"Error retrieving job configuration for job_id: {job_id}. Error: {e}"
        )
        return None, None


def save_annotations(
    job_id: str,
    before_date: Optional[datetime],
    after_date: Optional[datetime],
    es_client: Elasticsearch,
) -> Optional[str]:
    """
    Retrieves annotations for the given job within the specified date range.

    Args:
        job_id (str): The ID of the job.
        before_date (Optional[datetime]): The upper bound for the create_time.
        after_date (Optional[datetime]): The lower bound for the create_time.
        es_client (Elasticsearch): The Elasticsearch client.

    Returns:
        Optional[str]: The filename containing the annotations, or None if failed.
    """
    index = ".ml-annotations-read"
    date_range = {}
    if before_date:
        date_range["lte"] = int(before_date.timestamp() * 1000)  # Convert to milliseconds
    if after_date:
        date_range["gte"] = int(after_date.timestamp() * 1000)  # Convert to milliseconds

    search_query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"job_id": job_id}},
                    {"range": {"create_time": date_range}},
                ]
            }
        },
        "size": 10000,
    }

    logger.debug(f"Searching annotations for job {job_id} with query: {search_query}")

    try:
        response = es_client.search(index=index, body=search_query)
        annotations = response.get("hits", {}).get("hits", [])

        safe_job_id = sanitize_filename(job_id)
        filename = f"{safe_job_id}_annotations.ndjson"
        with open(filename, "w", encoding="utf-8") as f:
            for annotation in annotations:
                f.write(json.dumps(annotation["_source"]) + "\n")
        logger.info(f"Annotations for job {job_id} stored in {filename}")
        return filename
    except (ApiError, TransportError, IOError) as e:
        logger.error(f"Failed to save annotations: {e}")
        return None

def save_notifications(
    job_id: str,
    before_date: Optional[datetime],
    after_date: Optional[datetime],
    es_client: Elasticsearch,
) -> Optional[str]:
    """
    Retrieves notifications for the given job within the specified date range.

    Args:
        job_id (str): The ID of the job.
        before_date (Optional[datetime]): The upper bound for the create_time.
        after_date (Optional[datetime]): The lower bound for the create_time.
        es_client (Elasticsearch): The Elasticsearch client.

    Returns:
        Optional[str]: The filename containing the notifications, or None if failed.
    """
    index = ".ml-notifications-*"
    date_range = {}
    if before_date:
        date_range["lte"] = int(before_date.timestamp() * 1000)  # Convert to milliseconds
    if after_date:
        date_range["gte"] = int(after_date.timestamp() * 1000)  # Convert to milliseconds

    search_query = {
        "query": {
            "bool": {
                "must": [
                    {"term": {"job_id": job_id}},
                    {"range": {"timestamp": date_range}},
                ]
            }
        },
        "size": 10000,
    }
    logger.debug(f"Searching for notifications for job {job_id} with query: {search_query}")

    try:
        response = es_client.search(index=index, body=search_query)
        notifications = response.get("hits", {}).get("hits", [])

        safe_job_id = sanitize_filename(job_id)
        filename = f"{safe_job_id}_notifications.ndjson"
        with open(filename, "w", encoding="utf-8") as f:
            for notification in notifications:
                f.write(json.dumps(notification["_source"]) + "\n")
        logger.info(f"Notifications for job {job_id} stored in {filename}")
        return filename
    except (ApiError, TransportError, IOError) as e:
        logger.error(f"Failed to save notifications: {e}")
        return None

def extract_field_names_from_json(
    query_json: Dict[str, Any], known_operators: Set[str]
) -> Set[str]:
    """Recursively extract field names from a JSON query."""
    field_names = set()

    def recurse(obj: Any) -> None:
        if isinstance(obj, dict):
            for key, value in obj.items():
                if key not in known_operators:
                    field_names.add(key)
                recurse(value)
        elif isinstance(obj, list):
            for item in obj:
                recurse(item)

    recurse(query_json)
    return field_names


def extract_possible_field_names(query: Dict[str, Any]) -> Set[str]:
    """Extract possible field names from the Elasticsearch query."""
    field_names = extract_field_names_from_json(query, KNOWN_OPERATORS)
    # Drop the '.keyword' suffix
    field_names = {field.split(".keyword")[0] for field in field_names}
    logger.info(f"Extracted field names: {field_names}")
    return field_names


MAX_DOCS_PER_FILE = 10_000
PAGE_SIZE = 2_000
PIT_KEEP_ALIVE = "1m"

def extract_source_fields(job_config: Dict[str, Any], query: Dict[str, Any]) -> List[str]:
    """
    Collects and deduplicates all required _source fields from the job configuration and query.
    """
    field_keys = [
        "field_name",
        "partition_field_name",
        "categorization_field_name",
        "by_field_name",
        "over_field_name",
        "summary_count_field_name",
    ]

    fields = {
        detector[key]
        for detector in job_config["analysis_config"]["detectors"]
        for key in field_keys
        if key in detector
    }
    influencers = job_config["analysis_config"].get("influencers", [])
    fields.update(influencers)

    time_field = job_config["data_description"]["time_field"]
    if time_field == "timestamp":
        time_field = "@timestamp"
    fields.add(time_field)
    fields.update(extract_possible_field_names(query))

    # Strip any '.keyword' suffix
    cleaned = {f.split(".keyword")[0] for f in fields}
    return sorted(cleaned)


def build_date_range(before: Optional[datetime], after: Optional[datetime]) -> Dict[str, str]:
    """
    Constructs an Elasticsearch range clause for the given dates.
    """
    rng: Dict[str, str] = {}
    if before:
        rng["lte"] = before.isoformat()
    if after:
        rng["gte"] = after.isoformat()
    return rng


def scroll_batches(
    es_client: Elasticsearch,
    indices: List[str],
    query: Dict[str, Any],
    source_fields: List[str],
    time_field: str,
    date_range: Dict[str, str]
) -> Iterator[Dict[str, Any]]:
    """
    Yields search hits using PIT + search_after, sorted by time and sequence number.
    """
    pit_resp = es_client.open_point_in_time(index=indices, keep_alive=PIT_KEEP_ALIVE)
    pit_id = pit_resp["id"]
    body = {
        "_source": source_fields,
        "pit": {"id": pit_id, "keep_alive": PIT_KEEP_ALIVE},
        "query": {"bool": {"must": [query, {"range": {time_field: date_range}}]}},
        "sort": [{time_field: "asc"}, {"_seq_no": "asc"}],
        "size": PAGE_SIZE
    }
    search_after = None

    try:
        while True:
            if search_after:
                body["search_after"] = search_after
            resp = es_client.search(body=body)
            hits = resp.get("hits", {}).get("hits", [])
            if not hits:
                break
            for hit in hits:
                yield hit
            search_after = hits[-1]["sort"]
            body["pit"]["keep_alive"] = PIT_KEEP_ALIVE
    finally:
        es_client.close_point_in_time(body={"id": pit_id})


def write_to_ndjson(
    hits: Iterator[Dict[str, Any]],
    output_dir: Path,
    base_name: str
) -> List[str]:
    """
    Writes hits to chunked NDJSON files, returning a list of file paths.
    """
    output_dir.mkdir(parents=True, exist_ok=True)
    filenames: List[str] = []
    file_count = 1
    docs_written = 0
    total_docs = 0

    def new_file() -> Any:
        nonlocal file_count, docs_written
        path = output_dir / f"{base_name}_{file_count}.ndjson"
        f = path.open("w", encoding="utf-8")
        filenames.append(str(path))
        docs_written = 0
        return f

    writer = new_file()
    try:
        for hit in hits:
            if docs_written >= MAX_DOCS_PER_FILE:
                writer.close()
                file_count += 1
                writer = new_file()

            action = {"index": {"_index": hit["_index"], "_id": hit["_id"]}}
            writer.write(json.dumps(action) + "\n")
            writer.write(json.dumps(hit["_source"]) + "\n")

            docs_written += 1
            total_docs += 1
    finally:
        writer.close()
    logger.info(f"Wrote {total_docs} docs across {len(filenames)} files.")
    return filenames


def save_inputs(
    job_config: Dict[str, Any],
    before_date: Optional[datetime],
    after_date: Optional[datetime],
    es_client: Elasticsearch,
) -> Optional[List[str]]:
    """
    Orchestrates extraction of data via Elasticsearch PIT + search_after,
    then writes results into chunked NDJSON files.
    """
    indices = job_config["datafeed_config"]["indices"]
    base_name = sanitize_filename(job_config["job_id"]) + "_input"
    query = job_config["datafeed_config"].get("query", {"match_all": {}})

    source_fields = extract_source_fields(job_config, query)
    date_range = build_date_range(before_date, after_date)
    time_field = job_config["data_description"]["time_field"]
    if time_field == "timestamp":
        time_field = "@timestamp"

    try:
        hits = scroll_batches(es_client, indices, query, source_fields, time_field, date_range)
        return write_to_ndjson(hits, output_dir=Path("."), base_name=base_name)
    except Exception as exc:
        logger.error(f"Failed to save inputs: {exc}")
        return None

def create_archive(job_id: str, files: List[Optional[str]]) -> None:
    """
    Creates a tar.gz archive containing the specified files.

    Args:
        job_id (str): The ID of the job.
        files (List[Optional[str]]): List of file paths to include in the archive.
    """
    safe_job_id = sanitize_filename(job_id)
    archive_name = f"{safe_job_id}_state.tar.gz"
    try:
        with tarfile.open(archive_name, "w:gz") as tar:
            for file in files:
                if file and os.path.exists(file):
                    tar.add(file, arcname=os.path.basename(file))
                    logger.info(f"Added {file} to archive {archive_name}")
                else:
                    logger.warning(f"File {file} not found, skipping.")
        logger.info(f"Archive {archive_name} created successfully.")
    except IOError as e:
        logger.error(f"Failed to create archive: {e}")
    finally:
        # Remove the archived files
        logger.info("Removing temporary files")
        for file in files:
            if file and os.path.exists(file):
                os.remove(file)


def get_snapshot_info(
    es_client: Elasticsearch, job_id: str, before_date: Optional[datetime] = None
) -> Optional[Tuple[str, int]]:
    """
    Retrieves the latest snapshot information for a given job.

    Args:
        es_client (Elasticsearch): The Elasticsearch client.
        job_id (str): The ID of the job.
        before_date (Optional[datetime]): The date before which to retrieve the snapshot.

    Returns:
        Optional[Tuple[str, int]]: The snapshot ID and snapshot document count, or None if failed.
    """
    try:
        snapshot_response = es_client.ml.get_model_snapshots(
            job_id=job_id,
            end=before_date.isoformat() if before_date else None,
            desc=True,
        )
        if snapshot_response.get("count", 0) > 0:
            latest_snapshot = snapshot_response["model_snapshots"][0]
            snapshot_id = latest_snapshot["snapshot_id"]
            snapshot_doc_count = latest_snapshot["snapshot_doc_count"]
            logger.info(f"Latest snapshot ID for job {job_id}: {snapshot_id}")
            return snapshot_id, snapshot_doc_count
        else:
            logger.error("No snapshots found before the given date.")
            return None
    except (ApiError, TransportError) as e:
        logger.error(f"Error retrieving snapshot info: {e}")
        return None


def main() -> None:
    """
    Main function to extract model state from Elasticsearch.

    Example usage:
    python export_model_snapshot.py --url https://localhost:9200 --username user --job_id <job_id> --snapshot_before_date 2023-05-10T00:00:00 --input_after_date 2023-01-01T00:00:00 --input_before_date 2023-06-01T00:00:00 --include_inputs

    Output:
    <job_id>_state.tar.gz archive with the following files:
    - ml-anomalies-snapshot_doc_<id>.json snapshot stats document
    - <job_id>_config.json job configuration
    - <job_id>_snapshot_docs.ndjson snapshot documents
    - <job_id>_annotations.ndjson annotations
    - <job_id>_notifications.ndjson notifications
    - <job_id>_input.ndjson input data (if --include_inputs flag is set)
    """
    parser = argparse.ArgumentParser(
        description=(
            "Extract model state from Elasticsearch. WARNING: This operation will extract data that may include PII."
        )
    )
    parser.add_argument(
        "--url",
        type=str,
        required=False,
        default="https://localhost:9200",
        help="Elasticsearch URL",
    )
    parser.add_argument(
        "--username",
        type=str,
        required=True,
        help="Username for Elasticsearch authentication",
    )
    parser.add_argument(
        "--password",
        type=str,
        required=False,
        help="Password for Elasticsearch authentication",
    )
    parser.add_argument(
        "--job_id", type=str, required=True, help="Job ID to extract model state"
    )
    parser.add_argument(
        "--cloud_id", type=str, required=False, help="Cloud ID for Elasticsearch"
    )
    parser.add_argument(
        "--snapshot_before_date",
        type=validate_date,
        required=False,
        help="Search for the latest snapshot CREATED before the given date (format: YYYY-MM-DDTHH:MM:SS)",
    )
    parser.add_argument(
        "--annotations_after_date", 
        type=validate_date,
        required=False,
        help="Search for annotations and notifications after the specified date (format: YYYY-MM-DDTHH:MM:SS). Defaults to the job creation date.",
    )
    parser.add_argument(
        "--inputs_after_date",
        type=validate_date,
        required=False,
        help="Search for input data after the specified date (format: YYYY-MM-DDTHH:MM:SS)",
    )
    parser.add_argument(
        "--inputs_before_date",
        type=validate_date,
        required=False,
        help="Search for input data before the specified date (format: YYYY-MM-DDTHH:MM:SS)",
    )
    parser.add_argument(
        "--include_inputs",
        action="store_true",
        help="Include input data in the archive",
    )
    parser.add_argument(
        "--ignore_certs",
        action="store_true",
        help="Disable SSL certificate verification",
    )
    args = parser.parse_args()


    # Handle password securely
    if not args.password:
        args.password = getpass(prompt="Enter Elasticsearch password: ")

    # Warn about PII data
    logger.warning("This operation will extract data that may include PII.")
    confirm = input("Do you wish to continue? (yes/no): ")
    if confirm.lower() != "yes":
        logger.info("Operation aborted by the user.")
        return
    
    # If --include_inputs is set, but no input dates are provided, warn the user
    if args.include_inputs and (not args.inputs_after_date or not args.inputs_before_date):
        logger.warning(
            "Input data will be included, but no date range is specified. "
            "This may result in a large amount of data being extracted."
        )
        confirm = input("Do you wish to continue? (yes/no): ")
        if confirm.lower() != "yes":
            logger.info("Operation aborted by the user.")
            return
        
    

    logger.info("Connecting to Elasticsearch")
    # Connect to an Elasticsearch instance
    try:
        if args.cloud_id:
            logger.info("Connecting to Elasticsearch cloud using cloud_id")
            es_client = Elasticsearch(
                cloud_id=args.cloud_id,
                basic_auth=(args.username, args.password),
                verify_certs=(not args.ignore_certs),
            )
        else:
            logger.info("Connecting to Elasticsearch using URL")
            es_client = Elasticsearch(
                [args.url],
                basic_auth=(args.username, args.password),
                verify_certs=(not args.ignore_certs),
            )
    except (ApiError, TransportError) as e:
        logger.error(f"Failed to connect to Elasticsearch: {e}")
        return

    snapshot_info = get_snapshot_info(es_client, args.job_id, args.snapshot_before_date)
    if snapshot_info is None:
        logger.error("Failed to retrieve snapshot info.")
        return
    snapshot_id, snapshot_doc_count = snapshot_info

    # Get the snapshot document count and store the result
    file_name_ml_anomalies = save_snapshot_stats(args.job_id, snapshot_id, es_client)

    # Get the job configuration and store it
    job_config_result = save_job_config(args.job_id, es_client)
    if job_config_result:
        file_name_job_config, job_configuration = job_config_result
    else:
        logger.error("Failed to retrieve job configuration.")
        return

    # Get the annotations and store them
    if args.annotations_after_date:
        annotations_after_date = args.annotations_after_date
    else:
        # If no annotations_after_date is provided, use the job creation date
        job_creation_timestamp = job_configuration.get("create_time")
        logger.info(
            f"Job configuration creation date: {job_creation_timestamp}"
        )
        if job_creation_timestamp:
            # date from timestamp
            annotations_after_date = datetime.fromtimestamp(job_creation_timestamp / 1000.0)
            logger.info(
                f"Using job creation date {annotations_after_date} as annotations_after_date."
            )
        else:
            logger.error("Job creation date not found in job configuration.")
            return
    file_name_annotations = save_annotations(
        args.job_id, args.snapshot_before_date, annotations_after_date, es_client
    )

    file_name_notifications = save_notifications(
        args.job_id, args.snapshot_before_date, annotations_after_date, es_client
    )

    # Get the input data and store it
    if args.include_inputs:
        file_name_inputs = (
            save_inputs(job_configuration, args.inputs_before_date, args.inputs_after_date, es_client)
            if job_configuration
            else None
        )
    else:
        logger.info("Input data will not be included in the archive.")
        file_name_inputs = None

    # Call the function to extract model state
    filename_snapshots = save_snapshots(
        args.job_id, snapshot_id, es_client, snapshot_doc_count
    )

    # Create an archive with all generated files
    files_to_archive = [
        file_name_ml_anomalies,
        file_name_job_config,
        filename_snapshots,
        file_name_annotations,
        file_name_notifications,
    ] + file_name_inputs if file_name_inputs else []

    create_archive(args.job_id, files_to_archive)


if __name__ == "__main__":
    main()
