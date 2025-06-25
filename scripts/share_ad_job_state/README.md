# Elasticsearch Anomaly Detection Job State Export and Import

This project provides two Python scripts for exporting and importing anomaly detection job states in Elasticsearch:

- **Export Script**: extracts job states, job configurations, annotations, and input data from Elasticsearch and packages them into a tar.gz archive.
- **Import Script**: imports the archived data back into Elasticsearch, recreating the job and restoring its state.

Both scripts are designed with security and best practices in mind, ensuring safe handling of sensitive data and secure connections to Elasticsearch.

The export script extracts the following data from the customer's Elasticsearch instance:
- **Job state**: the encoded model snapshot that contains the model state. It can include values for the categorical fields defined in the job configuration: `by_field_name`, `over_field_name`, `partition_field_name`, and `influencers` fields.
- **Job configuration**: This is the result of `GET /_ml/anomaly_detectors/{job_id}` query
- **Annotations**: Annotations contain information about model changes, such as detected trends and seasonalities. If the user manually annotates some anomaly detection results, it will also include those annotations.
- **Input data**: Optionally, the user can provide a sample of the input data required for the job to run. To export input data, the flag `—include_inputs` must be explicitly specified, and only the fields required by the job configuration will be exported. The input data is stored in the file `{job_id}_input.ndjson` inside the archive to be easily inspected.

**Disclaimer**

The model **import** scripts are intended solely for use by Elastic developers for the purpose of reproducing issues or environments. These scripts are not designed for migrating machine learning job states between clusters.

Please refrain from running the model **import** scripts in a production environment, as doing so may compromise system stability or lead to unintended disruptions.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
  - [Export Script](#export-script)
  - [Import Script](#import-script)
- [Configuration](#configuration)
- [Security Considerations](#security-considerations)

---

## Prerequisites

- Python 3.12 or higher
- Elasticsearch cluster with appropriate permissions
- [Poetry](https://python-poetry.org/) for dependency management

## Installation

1. **Clone the Repository**

   ```bash
   git clone https://github.com/elastic/support-diagnostics.git
   cd support-diagnostics/scripts/share_ad_job_state
   ```

2. **Install Dependencies with Poetry**

   Ensure you have Poetry installed. If not, install it using the official instructions [here](https://python-poetry.org/docs/#installation).

   ```bash
   poetry install
   ```

   This command will create a virtual environment and install all the dependencies specified in `pyproject.toml` and `poetry.lock`.

## Usage

### Export Script

The export script extracts model states, job configurations, annotations, and input data from Elasticsearch and packages them into a tar.gz archive.
It accepts either `--url` or `--cloud_id` for Elasticsearch destination. 

#### Command

```bash
poetry run python export_model_snapshot.py --url <ELASTICSEARCH_URL> --username <USERNAME> --job_id <JOB_ID> [OPTIONS]
```

#### Options

- `--url`: Elasticsearch URL (default: `https://localhost:9200`)
- `--username`: Username for Elasticsearch authentication (required)
- `--password`: Password for Elasticsearch authentication (will prompt if not provided)
- `--job_id`: Job ID to extract model state (required)
- `--cloud_id`: Cloud ID for Elasticsearch
- `--before_date`: Search for the latest snapshot created before the given date (format: `YYYY-MM-DDTHH:MM:SS`)
- `--after_date`: Search for input data and annotations after the specified date (format: `YYYY-MM-DDTHH:MM:SS`)
- `--include_inputs`: Include input data in the archive (flag)
- `--ignore_certs`: Ignore SSL certificate verification (flag)

#### Example

```bash
poetry run python export_model_snapshot.py \
  --url https://my-elasticsearch-cluster:9200 \
  --username elastic \
  --job_id my-ml-job \
  --before_date 2023-05-10T00:00:00 \
  --after_date 2023-01-01T00:00:00 \
  --include_inputs
```

#### Output

The script creates an archive named `<job_id>_state.tar.gz` containing:

- `ml-anomalies-snapshot_doc_<id>.json`: Snapshot statistics document
- `<job_id>_config.json`: Job configuration
- `<job_id>_snapshot_docs.ndjson`: Snapshot documents
- `<job_id>_annotations.ndjson`: Annotations
- `<job_id>_input.ndjson`: Input data (if `--include_inputs` is set)

### Import Script

The import script reads the archived data and restores the model state and job configuration in Elasticsearch.

#### Command

```bash
poetry run python import_model_state.py --url <ELASTICSEARCH_URL> --username <USERNAME> --job_id <JOB_ID> --archive_path <ARCHIVE_PATH> [OPTIONS]
```

#### Options

- `--url`: Elasticsearch URL (default: `https://localhost:9200`)
- `--username`: Username for Elasticsearch authentication (required)
- `--password`: Password for Elasticsearch authentication (will prompt if not provided)
- `--job_id`: Job ID to import (required)
- `--archive_path`: Path to the archive file (required)
- `--cloud_id`: Cloud ID for Elasticsearch
- `--ignore_certs`: Ignore SSL certificate verification (flag)

#### Example

```bash
poetry run python import_model_state.py \
  --url https://localhost:9200 \
  --username elastic \
  --job_id my-ml-job \
  --archive_path my-ml-job_state.tar.gz \
  --ignore_certs
```

#### Process

1. **Extracts** the archive to a temporary directory.
2. **Imports** input data (if available) into a new index.
3. **Uploads** the job configuration to Elasticsearch.
4. **Indexes** the snapshot documents and statistics.
5. **Reverts** the job to the imported snapshot.
6. **Cleans Up** temporary files and directories.

## Configuration

Both scripts can be configured using command-line arguments as detailed in the Usage section. Ensure that you have the necessary permissions and correct URLs when connecting to your Elasticsearch cluster.

## Security Considerations

- **SSL Verification**: SSL certificate verification is enabled by default to ensure secure connections. Do not disable SSL verification unless absolutely necessary and you understand the risks.
- **Password Handling**: Passwords are securely prompted if not provided via command-line arguments.
- **PII Data Warning**: Export scripts warns about potentially sensitive data (e.g., Personally Identifiable Information) in the input data. 
