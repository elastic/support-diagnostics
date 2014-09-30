elasticsearch-support-diagnostics
=================================

elasticsearch-support-diagnostics is an elasticsearch plugin designed
to assist users with gathering diagnostic data about their cluster and nodes.

Installation:

	./bin/plugin --install elasticsearch/elasticsearch-support-diagnostics/1.0.0


### Support Diagnostics Script

The diagnostic script is installed in `./bin/support-diagnostics.sh` (or `./bin/support-diagnostics.ps1` on Windows).

In order to gather the elasticsearch config and logs you must run the script on a node within your elasticsearch cluster.  If local data, such as top or netstat are required, it should be run on each node in the cluster.  Otherwise, running the script on a single node is sufficient.

### Usage

    -h  This help message
    -H  Elasticsearch hostname:port (defaults to $eshost)
    -n  On a host with multiple nodes, specify the node name to gather data for. Value should match node.name as defined in elasticsearch.yml
    -o  Script output directory (optional, defaults to support-diagnostics.[timestamp].[hostname])
    -nc Disable compression (optional)

### Running on Windows

PowerShell 4.0 and the .NET Framework 4.5 are required in order to run the `support-diagnostics.ps1` script on Windows.
