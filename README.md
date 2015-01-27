elasticsearch-support-diagnostics
=================================

elasticsearch-support-diagnostics is an elasticsearch plugin designed
to assist users with gathering diagnostic data about their cluster and nodes.

Installation:

	./bin/plugin --install elasticsearch/elasticsearch-support-diagnostics
	

### Support Diagnostics Script

The diagnostic script is installed in `./bin/support-diagnostics/support-diagnostics.sh` (or `.\bin\support-diagnostics\support-diagnostics.bat` on Windows).

In order to gather the elasticsearch config and logs you must run the script on a node within your elasticsearch cluster.  If local data, such as top or netstat are required, it should be run on each node in the cluster.  Otherwise, running the script on a single node is sufficient.

### Usage

    -h  This help message
    -H  Elasticsearch hostname:port (defaults to localhost:9200)
    -n  On a host with multiple nodes, specify the node name to gather data for. Value should match node.name as defined in elasticsearch.yml
    -o  Script output directory (optional, defaults to support-diagnostics.[timestamp].[hostname])
    -nc Disable compression (optional)
    -r  Collect stats r times (optional, in conjunction with -i , defaults to 1)
    -i  Interval in seconds between stats collections (optional, in conjunction with -r , defaults to 30 secs)
    -a  Authentication type. Either 'basic' or 'cookie' (optional)
    -c  Authentication credentials. Either a path to the auth cookie file or the basic auth usename. You will be prompted for the password unless you specify -p.
    -p  Password for authentication. To be used with -c if having this script prompt for a password is undesiarable.


### Running on Windows

PowerShell 4.0 and .NET Framework 4.5 are required in order to run the script on Windows, both which come built into Windows Server 2012 and Windows 8.1.  If you are running anything earlier than Windows Server 2012 or Windows 8.1, then you may need to install them manually.  See [How to Install Windows PowerShell 4.0](http://social.technet.microsoft.com/wiki/contents/articles/21016.how-to-install-windows-powershell-4-0.aspx) for more information.

The script can be executed on Windows by calling either `support-diagnostics.bat` (preferred), or calling the `support-diagnostics.ps1` PowerShell script directly.  The purpose of the batch file is to enable the script to be run from a PowerShell console as well as the regular Windows command prompt (cmd.exe).  It also passes the necessary arguments to bypass the Windows PowerShell execution policy.
