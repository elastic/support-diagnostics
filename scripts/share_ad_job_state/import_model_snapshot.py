import argparse
import json
import os
import re
import tarfile
import time
from typing import Any, Dict, List, Optional

from elasticsearch import Elasticsearch, helpers, ApiError, TransportError
from getpass import getpass
from loguru import logger

# Disable noisy warning about missing certificate verification
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def sanitize_filename(name: str) -> str:
    """Sanitize filenames to prevent directory traversal and other security issues."""
    return re.sub(r"[^a-zA-Z0-9_-]", "_", name)


def is_within_directory(directory: str, target: str) -> bool:
    """Check if the target path is within the given directory."""
    abs_directory = os.path.abspath(directory)
    abs_target = os.path.abspath(target)
    return os.path.commonpath([abs_directory]) == os.path.commonpath(
        [abs_directory, abs_target]
    )


def safe_extract(tar: tarfile.TarFile, path: str = ".") -> None:
    """Safely extract tar files to prevent path traversal attacks."""
    for member in tar.getmembers():
        member_path = os.path.join(path, member.name)
        if not is_within_directory(path, member_path):
            raise Exception(f"Attempted Path Traversal in Tar File: {member.name}")
    tar.extractall(path=path)


def extract_archive(archive_path: str, output_dir: str) -> List[str]:
    """
    Extracts a tar.gz archive to the specified output directory.

    Args:
        archive_path (str): The path to the tar.gz archive.
        output_dir (str): The directory to extract files to.

    Returns:
        List[str]: A list of extracted file paths.
    """
    logger.info(f"Extracting archive {archive_path} to {output_dir}")
    try:
        with tarfile.open(archive_path, "r:gz") as tar:
            safe_extract(tar, path=output_dir)
            extracted_files = tar.getnames()
        return [os.path.join(output_dir, file) for file in extracted_files]
    except (tarfile.TarError, Exception) as e:
        logger.error(f"Error extracting archive: {e}")
        return []


def generate_actions(file_path: str, new_index: str):
    """
    Generator function that reads a .ndjson file and yields actions.
    It yields two lines at a time (action + document).

    Args:
        file_path (str): Path to the .ndjson file.
        new_index (str): The new index name to replace in actions.

    Yields:
        Dict[str, Any]: Action dictionaries for bulk upload.
    """
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            while True:
                action_line = f.readline().strip()
                document_line = f.readline().strip()
                if not action_line or not document_line:
                    break
                # Replace the index name in the action
                action = json.loads(action_line)
                document = json.loads(document_line)

                yield {
                    "_op_type": "index",
                    "_index": new_index,
                    "_id": action["index"]["_id"],
                    "_source": document,
                }
    except IOError as e:
        logger.error(f"Error reading file {file_path}: {e}")


def upload_data(es_client: Elasticsearch, index: str, file_path: str) -> None:
    """
    Loads JSON data into the specified Elasticsearch index.

    Args:
        es_client (Elasticsearch): The Elasticsearch client.
        index (str): The index to load data into.
        file_path (str): Path to the data file.
    """
    try:
        response = helpers.bulk(
            es_client, generate_actions(file_path, index), index=index, chunk_size=1000
        )
        logger.info(f"{response[0]} documents uploaded to index {index}")
    except (ApiError, TransportError, Exception) as e:
        logger.error(f"Error uploading data: {e}")


def create_input_index(es_client: Elasticsearch, index: str) -> None:
    """
    Creates an Elasticsearch index for input data. If it exists, deletes and recreates it.

    Args:
        es_client (Elasticsearch): The Elasticsearch client.
        index (str): The index to create.
    """
    try:
        if es_client.indices.exists(index=index):
            logger.info(f"Deleting existing index {index}")
            es_client.indices.delete(index=index)
        logger.info(f"Creating index {index}")
        es_client.indices.create(index=index)
    except (ApiError, TransportError) as e:
        logger.error(f"Error creating index {index}: {e}")


def create_job_config(
    es_client: Elasticsearch,
    job_config: Dict[str, Any],
    new_index: Optional[str] = None,
) -> Optional[str]:
    """
    Uploads the job configuration using the put_job API.

    Args:
        es_client (Elasticsearch): The Elasticsearch client.
        job_config (Dict[str, Any]): The job configuration to upload.
        new_index (Optional[str]): New index name for the datafeed config.

    Returns:
        Optional[str]: The job ID if successful, None otherwise.
    """
    job_fields = [
        "job_id",
        "description",
        "analysis_config",
        "data_description",
        "model_snapshot_retention_days",
        "results_index_name",
        "analysis_limits",
        "custom_settings",
        "allow_lazy_open",
        "datafeed_config",
    ]
    filtered_config = {key: job_config[key] for key in job_fields if key in job_config}
    job_id = filtered_config.get("job_id")
    if not job_id:
        logger.error("Job ID not found in job configuration.")
        return None

    # Remove sensitive or irrelevant fields
    datafeed_config = filtered_config.get("datafeed_config", {})
    datafeed_config.pop("authorization", None)
    datafeed_config.pop("job_id", None)
    if new_index:
        datafeed_config["indices"] = [new_index]
    filtered_config["datafeed_config"] = datafeed_config

    # Check if the job already exists and delete it
    try:
        if es_client.ml.get_jobs(job_id=job_id):
            es_client.ml.delete_job(job_id=job_id, force=True)
            logger.info(f"Deleted existing job with ID: {job_id}")
    except (ApiError, TransportError):
        logger.info(f"Job with ID {job_id} does not exist, proceeding to create.")

    try:
        response = es_client.ml.put_job(job_id=job_id, body=filtered_config)
        logger.info(f"Job configuration uploaded with ID: {response['job_id']}")
        return job_id
    except (ApiError, TransportError) as e:
        logger.error(f"Error uploading job configuration: {e}")
        return None


def load_snapshot_stats(es_client: Elasticsearch, file_path: str) -> Optional[str]:
    """
    Loads snapshot statistics from the given file and indexes it in Elasticsearch.

    Args:
        es_client (Elasticsearch): The Elasticsearch client.
        file_path (str): The path to the snapshot statistics file.

    Returns:
        Optional[str]: The snapshot ID if successful, None otherwise.
    """
    logger.info(f"Loading snapshot statistics from {file_path}")
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            snapshot_stats = json.load(f)
            snapshot_id = snapshot_stats.get("snapshot_id")
        if not snapshot_id:
            logger.error("Snapshot ID not found in snapshot statistics.")
            return None

        index_name = ".ml-anomalies-shared"
        # Extract ID from file name
        id_ = os.path.splitext(os.path.basename(file_path))[0].replace(
            "ml-anomalies-snapshot_doc_", ""
        )
        response = es_client.index(index=index_name, body=snapshot_stats, id=id_)
        logger.info(f"Snapshot statistics indexed with ID: {response['_id']}")
        return snapshot_id
    except (ApiError, TransportError, IOError, json.JSONDecodeError) as e:
        logger.error(f"Error indexing snapshot statistics: {e}")
        return None


def find_file(file_name: str, extracted_files: List[str]) -> Optional[str]:
    """Find a file in the extracted files list."""
    for file in extracted_files:
        if file_name in file:
            return file
    return None


def import_model_state(
    job_id: str, es_client: Elasticsearch, archive_path: str
) -> None:
    """
    Imports the model state, job configuration, annotations, and input data from an archive to Elasticsearch.

    Args:
        job_id (str): The ID of the job to import.
        es_client (Elasticsearch): The Elasticsearch client.
        archive_path (str): The path to the tar.gz archive.
    """
    safe_job_id = sanitize_filename(job_id)
    output_dir = f"extracted_{safe_job_id}"
    extracted_files = extract_archive(archive_path, output_dir)
    if not extracted_files:
        logger.error("No files extracted. Aborting import.")
        return

    input_index = None
    job_config_file = f"{safe_job_id}_config.json"
    snapshot_docs_file = f"{safe_job_id}_snapshot_docs.ndjson"
    input_file = f"{safe_job_id}_input.ndjson"
    snapshot_stats_file = None

    # Find snapshot statistics file
    for file in extracted_files:
        if "ml-anomalies-snapshot_doc" in file:
            snapshot_stats_file = file
            break

    # Load the input data if available
    file = find_file(input_file, extracted_files)
    if file:
        logger.info(f"Importing input data from {file}")
        input_index = f"{safe_job_id}-input"
        create_input_index(es_client, input_index)
        upload_data(es_client, input_index, file)
    else:
        logger.warning(f"Input data file {input_file} not found in the archive.")

    # Load the job configuration
    file = find_file(job_config_file, extracted_files)
    if file:
        logger.info(f"Importing job configuration from {file}")
        try:
            with open(file, "r", encoding="utf-8") as f:
                job_config = json.load(f)
            job_id = create_job_config(es_client, job_config, input_index)
            if not job_id:
                logger.error("Failed to create job configuration.")
                return
        except (IOError, json.JSONDecodeError) as e:
            logger.error(f"Error reading job configuration: {e}")
            return
    else:
        logger.error(
            f"Job configuration file {job_config_file} not found in the archive."
        )
        return

    # Load the job snapshot docs
    file = find_file(snapshot_docs_file, extracted_files)
    if file:
        logger.info(f"Importing snapshots from {file}")
        upload_data(es_client, ".ml-state-write", file)
    else:
        logger.error(
            f"Snapshot documents file {snapshot_docs_file} not found in the archive."
        )
        return

    # Load the snapshot stats
    if snapshot_stats_file:
        logger.info(f"Importing snapshot statistics from {snapshot_stats_file}")
        snapshot_id = load_snapshot_stats(es_client, snapshot_stats_file)
        if not snapshot_id:
            logger.error("Failed to load snapshot statistics.")
            return
    else:
        logger.error("Snapshot statistics file not found in the archive.")
        return

    # Revert the job to the snapshot
    time.sleep(2)  # Wait for the snapshot to be indexed
    try:
        es_client.ml.revert_model_snapshot(job_id=job_id, snapshot_id=snapshot_id)
        logger.info(f"Reverted job {job_id} to snapshot {snapshot_id}")
    except (ApiError, TransportError) as e:
        logger.error(f"Error reverting job to snapshot: {e}")
    finally:
        # Clean up extracted files and directory
        try:
            for file in extracted_files:
                os.remove(file)
            os.rmdir(output_dir)
        except OSError as e:
            logger.warning(f"Error cleaning up extracted files: {e}")

    logger.info(f"Import of job {job_id} completed successfully.")


def main() -> None:
    """Main function to import model state to Elasticsearch."""
    parser = argparse.ArgumentParser(description="Import model state to Elasticsearch.")
    parser.add_argument(
        "--url", type=str, default="https://localhost:9200", help="Elasticsearch URL"
    )
    parser.add_argument(
        "--username",
        type=str,
        required=True,
        help="Username for Elasticsearch authentication",
    )
    parser.add_argument(
        "--password", type=str, help="Password for Elasticsearch authentication"
    )
    parser.add_argument("--job_id", type=str, required=True, help="Job ID to import")
    parser.add_argument(
        "--archive_path", type=str, required=True, help="Path to the archive file"
    )
    parser.add_argument("--cloud_id", type=str, help="Cloud ID for Elasticsearch")
    parser.add_argument(
        "--ignore_certs",
        action="store_true",
        help="Ignore SSL certificate verification",
    )
    args = parser.parse_args()

    # Handle password securely
    if not args.password:
        args.password = getpass(prompt="Enter Elasticsearch password: ")

    # Validate archive_path
    if not os.path.isfile(args.archive_path):
        logger.error(f"Archive file {args.archive_path} does not exist.")
        return

    logger.info("Connecting to Elasticsearch")
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

    import_model_state(args.job_id, es_client, args.archive_path)


if __name__ == "__main__":
    main()
