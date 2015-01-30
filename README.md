elasticsearch-support-diagnostics
=================================

_Note:_ This branch is for the java rewrite of the support plugin. 

support-diagnostics is an elasticsearch plugin designed
to assist users with gathering diagnostic data about their cluster and nodes.

### Installation:

	./bin/plugin --install elasticsearch/support-diagnostics
	

### Usage

	Linux:	/bin/plugin/support-diagnostics/support-diagnostics
	Windows: /bin/plugin/support-diagnostics/support-diagnostics.bat
	
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


### Building

##### Making and Installing
If you wish to build the plugin you need Gradle >= 2.1 

Within the directory, run the installApp task to compile the code and generate runnable scripts for it.

	$ gradle installApp
	
	
This will build and create an easily runnable version of the jar with Unix (extensionless) and Windows (.bat) compatible scripts under:

	build/install/support-diagnostics/bin/support-diagnostics
	

Alternatively, you could just run it via Gradle directly (which will not expose the generated scripts in a user friendly way).

	$ gradle run


##### Testing

	$ gradle clean test