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

PowerShell 4.0 and .NET Framework 4.5 are required in order to run the `support-diagnostics.ps1` script, which come built into Windows Server 2012 and Windows 8.1.  If you are running anything earlier than Windows Server 2012 or Windows 8.1, then you will need to install them manually.  See [How to Install Windows PowerShell 4.0](http://social.technet.microsoft.com/wiki/contents/articles/21016.how-to-install-windows-powershell-4-0.aspx) for more information.