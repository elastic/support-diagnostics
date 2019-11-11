## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the host it is running on to obtain data and statistics on the running cluster. 
It will execute a series of REST API calls to the running cluster, run a number of OS related commands(such as top, netstat, etc.), and collect logs, then bundle them into one or more archives.

* Compatible with all versions of Elasticsearch, including beta and release candidates. If it cannot match the targeted version, it will attempt to use the calls from the latest release.
  * Note: the version of this tool is independent of the version of Elasticsearch/Logstash targeted.
* OS specific versions are not required.
* The application can be run from any directory on the machine.  It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts, write access to the chosen output directory, and sufficient disk space for the generated archive.
* Detects multiple nodes and network interfaces per host.
* Shield authentication, SSL and cookies are supported for REST calls to the cluster.

## Building From Source
* Clone the github repo.
* Make sure a recent version of Maven is installed on the build machine. 
* Create a `MAVEN_HOME` directory pointing to the location you've unzipped it to.
* `cd` to the top level repo directory and type `mvn package`.

## Run Requirements
* JDK - Oracle or OpenJDK, 1.8-10 +
  * The IBM JDK is not supported due to JSSE related issues that cause TLS errors.
  * **Important Note For Version 7:** Elasticsearch now includes a bundled JVM that is used by default. For the diagnostic to be able to retrieve thread dumps via Jstack it must be executed with the same JVM that was used to run Elasticsearch. Therefore you must:
    * Set the Elasticsearch supplied JVM as as the default to be executed. On Linux distributions this would be the one returned by `which java`.
    * Set JAVA_HOME to the directory containing the /bin directory for the included JDK. For example, <path to Elasticsearch 7 deployment>/jdk/Contents/Home.
* If you are running a package installation under Linux you MUST run the command with elevated sudo privileges. Otherwise the utility will not be able to run the system queries.
* The system account running the utility must have read access to the Elasticsearch files and write access to the output location.
* If you are using Shield/Security the supplied user id must have permission to execute the diagnostic URL's.
* Linux, Windows, and Mac OSX are supported.
* For Docker installations see the section below for details. 

## Installation
* Download [support-diagnostics-latest-dist.zip](https://github.com/elastic/elasticsearch-support-diagnostics/releases/latest) from the Github release area.
* Unzip the support-diagnostics-`<version>`-dist.zip into the directory from which you intend to run the application.
* Switch to the diagnostics distribution directory.

## Version check
As a first step the diagnostic will check the Github repo for the current released version, and if not the same as the one running will provide the URL for the current release and then stop. The user will then be prompted to to press Enter before the diag continues.
* For air gapped environments this can be bypassed by adding `--bypassDiagVerify` to the command line.

## Usage - Simplest Case
* Run the application via the diagnostics.sh or diagnostics.bat script. The host name or IP address used by the HTTP connector of the node is required. IP address will generally ensure the best result.
* In order to assure that all artifacts are collected it is recommended that you run the tool with elevated privileges. This means sudo on Linux type platforms and via an Administor Prompt in Windows.
* A hostname or IP address must now be specified via the `--host` parameter. This is due to the changes in default port binding what were introduced starting with version 2. You must supply this even if you are running as localhost.
* The utility will use default listening port of `9200` if you do not specify one.
* If the utility cannot find a running ES version for that host/port combination the utility will exit and you will need to run it again.
* Input parameters may be specified in any order.
* An archive with the format diagnostics-`<DateTimeStamp>`.tar.gz will be created in the utility directory. If you wish to specify a specific output folder you may do so by using the -o `<Full path to custom output folder>` option.

#### Basic Usage Examples
    NOTE: Windows users use .\diagnostics.bat instead of ./diagnostics.sh
    sudo ./diagnostics.sh --host localhost
    sudo ./diagnostics.sh --host 10.0.0.20
    sudo ./diagnostics.sh --host myhost.mycompany.com
    sudo ./diagnostics.sh --host 10.0.0.20 --port 9201
    sudo ./diagnostics.sh --host localhost -o /home/myusername/diag-out

#### Getting Command Line Help
    ./diagnostics.sh --help
    .\diagnostics.bat --help

## Using With Security
* a truststore does not need to be specified - it's assumed you are running this against a node that you set up and if you didn't trust it you wouldn't be running this.
* When using authentication, do not specify both a password and the `-p` option.  Using the `-p` option will bring up a prompt for you to type an obfuscated value that will not be displayed on the command history.
* `--noVerify` will bypass hostname verification with SSL.
* `--keystore` and `--keystorePass` allow you to specify client side certificates for authentication.
* To script the utility when using Shield/Security, you may use the `--ptp` option to allow you to pass a plain text password to the command line rather than use `-p` and get a prompt.  Note that this is inherently insecure - use at your own risk.

#### Examples - Without SSL
    sudo ./diagnostics.sh --host localhost -u elastic -p
    sudo ./diagnostics.sh --host 10.0.0.20 -u elastic -p
#### Example - With SSL
    sudo ./diagnostics.sh --host 10.0.0.20 -u <your username> -p --ssl

## Additional Options
* You can specify additional java options such as a higher `-Xmx` value by setting the environment variable `DIAG_JAVA_OPTS`.
* Because of the potential size access logs are no longer collected by default. If you need these use the `--accessLogs` option to have them copied.

## Alternate Usages

### Proxy Servers
If the presence of a proxy server prevents the diagnostic from being run(generally due to running with the remote option on a client workstation) you can specify the settings with the parameters: 
* --proxyHost
* --proxyPort
* --proxyUser
* --proxyPassword 

### Customizing the output
The `diag.yml` file in the `/lib/support-diagnostics-x.x.x` contains all the REST and system commands that will be run. These are tied to a particular version. You can extract this file and remove commands you do not wish to run as well as adding any that may not be currently included. Place this revised file in the directory containing the diagnostics.sh script and it will override the settings contained in the jar.

### Remote
* If you do not wish to run the utility on the host the node to be queried resides on, and wish to run it from a different host such as your workstation, you may use the `--type remote` option.
* This will execute only the REST API calls and will not attempt to execute local system calls or collect log files. 
#### Remote Example
     ./diagnostics.sh --host 10.0.0.20 --type remote
    
### Docker Containers
* The diagnostic will examine the nodes output for process id's that have a value of `1`. If it finds any it will assume that all nodes are running in Docker containers and bypass normal system calls and log collection.
* If issues are encountered, consider bypassing any local system calls by using _--type remote_
* By default, once the diagnostic detects Docker containers, it will generate system calls for all running containers. If you wish to limit output to a single container, you may do this by using the --dockerId option.  The output for each container will be stored in a subfolder named for the container id.  
####### To run the docker calls only for the container elasticsearch2:
```$xslt
$ docker ps
CONTAINER ID        IMAGE                                                 COMMAND                  
4577fe120750        docker.elastic.co/elasticsearch/elasticsearch:6.6.2   "/usr/local/bin/dock…"   elasticsearch2
e29d2b491f8d        docker.elastic.co/elasticsearch/elasticsearch:6.6.2   "/usr/local/bin/dock…"   elasticsearch

$ sudo .diagnostics.sh --host 10.0.0.20 --dockerId 4577fe120750
```    
### Logstash Diagnostics
* Use the `--type logstash` argument to get diagnostic information from a running Logstash process. It will query the process in a manner similar to the Elasticsearch REST API calls.
* The default port will be `9600`. This can be modified at startup, or will be automatically incremented if you start multiple Logstash processes on the same host. You can connect to these other Logstash processes with the `--port` option.
* Available for 5.0 or greater.
* System statistics relevant for the platform will also be collected, similar to the standard diagnostic type

#### Logstash Examples
    sudo ./diagnostics.sh --host localhost --type logstash
    sudo ./diagnostics.sh --host localhost --type logstash --port 9610

## File Sanitization Utility

#### Description
* Works on diagnostics produced by version 6.4 and later. Please don't turn in a ticket if you run it on an older one.
* Runs as a separate application from the diagnostic. It does not need to be run on the same host the diagnostic utility ran on.
* Inputs an archive produced via a normal diagnostic run or a single file such as a log.
* Goes through each line in each file in that archive line by line and does the following:
  * Automatically obfuscates all IPv4 and IPv6 addresses. These will be consistent throughout all files in the archive. In other words, it encounters 10.0.0.5 in one file, the obfuscated value will be used for all occurrences of the IP in other files. These will not, however be consistent from run to run.
  * Obfuscates MAC addresses.
  * If you include a configuration file of supplied string tokens, any occurrence of that token will be replaced with a generated replacement. As with IP's this will be consistent from file to file but not between runs. Literal strings or regex's may be used.
* If running against a standard diagnostic package, re-archives the file with "scrubbed-" prepended to the  name. An example file (`scrub.yml`) is included as an example. Single files will be written to the output directiory prepended with _scrubbed-_.
* If you are processing a large cluster's diagnostic, this may take a while to run, and you may need to use the `DIAG_JAVA_OPTS` environment variable to bump up the Java Heap if processing is extremely slow or you see OutOfMemoryExceptions.

#### How to run
* Run the diagnostic utility to get an archive.
* Add any tokens for text you wish to conceal to a config file. By default the utility will look for scrub.yml in the working directory.
* Run the utility with the necessary and optional diagnosticInputs. It is a different script execution than the diagnostic with different arguments.
  * `-a, --archive` &nbsp;&nbsp;&nbsp; An absolute path to the archive file you wish to sanitize(required if single file not specified).
  * `-i, --infile` &nbsp;&nbsp;&nbsp; An absolute path to the individual file you wish to sanitize(required if diagnostic archive file not specified).
  * `-o, --out, --output, --outputDir` &nbsp;&nbsp;&nbsp; A target directory where you want the revised archive written. If not supplied it will be written to the same folder as the diagnostic archive it processed.
  * `-c, --config` &nbsp;&nbsp;&nbsp; The configuration file containing any text tokens you wish to conceal. These can be literals or regex's. The default is the scrub.yml contained in the jar distribution.

#### Examples:

#### With no tokens specified, writing the same directory as the diagnostic:
```$xslt
./scrub.sh -a /home/adminuser/diagoutput/diagnostics-20180621-161231.tar.gz
```
#### With a token file writing to a specific output directory:
```$xslt
./scrub.sh -a /Users/rdavies/diagoutput/diagnostics-20180621-161231.tar.gz -o /home/adminuser/sanitized-diags -c /home/adminuser/sanitized-diags/scrub.yml
```
#### With a token file processing a single log file:
```$xslt
./scrub.sh -i /home/adminuser/elasticsearch.log -o /home/adminuser/sanitized-diags -c /home/adminuser/sanitized-diags/scrub.yml
```
#### Sample token scrub file entries:
```$xslt
tokens:
  - 'node-[\d?]*'
  - 'cluster-630'
  - 'disk1'
  - 'data-one'
```
# Experimental - Monitoring Data Extraction

While the standard diagnostic is often useful in providing the background necessary to solve an issue, it is also limited in that it shows a strictly one dimensional view of the cluster's state. 
The view is restricted to whatever was available at the time the diagnostic was run. So a diagnostic run subsequent to an issue will not always provide a clear indication of what caused it.

If you have set up Elasticsearch monitoring there is time series data avaialble, but in order to view it anywhere other than locally you would need to snapshot the relevant monitoring indices or have the person wishing to view it do so via a screen sharing session. 
Neither of these may be optimal if there is an urgent issue or multiple individuals need to be involved.

You can now extract an interval of up to 12 hours of monitoring data at a time from your monitoring cluster. It will package this into a tar.gz file, much like the current diagnostic. 
After it is uploaded, a support engineer can import that data into their own monitoring cluster so it can be investigated outside of a screen share, and be easily viewed by other engineers and developers.
It has the advantage of providing a view of the cluster state prior to when an issue occurred so that a better idea of what led up to the issue can be gained.

It does not need to be run on an Elasticsearch host. A local workstation with network access to the monitoring cluster is sufficient.

You can collect statistics for only one cluster at a time, and it is necessary to specify a cluster id when running the utility.
If you are not sure of the cluster id, running with only the host, login, and --list parameter will display a listing
of the clusters being monitored in the format: `cluster name`  &nbsp;&nbsp;&nbsp; `cluster id` &nbsp;&nbsp;&nbsp; `display name`.

#### Running the extraction:

* The host or IP address of a monitoring cluster node is required, same as for the standard diagnostic. In addition, any
authentication or encryption options( --ssl, --bypassHostNameVerify, etc.) necessary to log into that cluster are required. A superuser role is recommended.
* The cluster_id of the cluster you are extracting monitoring data for is also required. If you are unsure of what this is
you can obtain id's for all the monitored clusters by using the --list parameter along with the host and auth inputs.
* To select the range of data extracted use the --start and --interval parameters to set the point at which to start collecting events and how 
many hours of events to collect.
* The start date/time will be converted from the current machine's time zone to UTC before querying.
* The date, time and interval parameters are not required - if a parameter is not supplied the utility will use defaults to generate one. If no date, time or interval
are specified the collection will start 6 hours back from when the utility is run and cover up to the present. 
* The monitoring indices types being collected are as follows: cluster_stats, node_stats, indices_stats, index_stats, shards, job_stats, ccr_stats, 
and ccr_auto_follow_stats.
* Notice: not all the information contained in the standard diagnostic is going to be available in the monitoring extraction. That is because it 
does not collect as much data. If you don't see what you want in the monitoring interface or via your own Dashboards, please file a Github issue
with the Elasticsearch Monitoring team.
* Metricbeat information is not available yet: possibly next release.
* For now, stats will not be collected for system indices. That may be added in the future as an option if shows up as needed.

The additional parameters:
  * `--id` _REQUIRED_ &nbsp;&nbsp;&nbsp;  The cluster_id of the cluster you wish to retrieve data for. Because multiple clusters may be monitored this is necessary to retrieve the correct subset of data. If you are not sure, see the --list option example below to see which clusters are available.
  * `--interval` &nbsp;&nbsp;&nbsp; The number of hours of statistics you wish to collect. Defaults to the current date and time minus the default interval. Default value of 6. Minimum value of 1, maximum value of 12.
  * `--start`  &nbsp;&nbsp;&nbsp;  Required format: 'yyyy-MM-dd HH:mm'&nbsp;&nbsp;&nbsp;  The combined date and time for the earliest point to be extracted. Must be enclosed in quotes due to the space. Time should be in 24 hour format. 
  Defaults to the current date and time, minus the default interval.
  * `--list` &nbsp;&nbsp;&nbsp; Lists the clusters available data extraction on this montioring server. It will provide the cluster_name, the cluster_id. If this is
   a cloud cluster and the metadata.display_name was set it will be displayed as well.

#### Examples 

##### Simple case using defaults - data from the last 6 hours will be collected:
```$xslt
    sudo ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843
```
##### Specifies a specific date, time and uses default interval 6 hours:
```$xslt
    sudo ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --start '2019-08-25 08:30'
```
##### Specifies the last 8 hours of data.
```$xslt
    sudo ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --interval 8
```
##### Specifies a specific date, time and interval:
```$xslt
        sudo ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --start '2019-08-25 17:45' --interval 10
```
##### Lists the clusters availble in this monitoring cluster
```$xslt
    sudo ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --list    
```
    
# Experimental - Monitoring Data Import

Once you have an archive of exported monitoring data, you can import this into an ES 7 instance that has monitoring enabled. Only ES 7 is supported as a target cluster.
* You will need an installed instance of the diagnostic utility. This does not need to be on the same 
host as the ES monitoring instance, but it does need to be on the same host as the archive you wish to import since it will need to read the archive file.
As with all other diag functions, a recent Java runtime must be installed.
* This will only work with a monitoring export archive produced by the diagnostic utility. It will not work with a standard diagnostic bundle or a custom archive.
* The only required parameters are the host/login information for the monitoring cluster and the absolute path to the archive you wish to import.
  * `--input` _REQUIRED_ &nbsp;&nbsp;&nbsp;  Absolute path to the archive you wish to import. No symlinks, please. The name format will
  be similar to a standard diagnostic: `monitoring-export-<Datestamp>-<Timestamp>`.
  * `--clusterName` &nbsp;&nbsp;&nbsp; If you wish to change the name of the imported cluster to display something relevant to your purpose, you can use this parameter. Otherwise it will be added under the same cluster name
  it had at export time. No spaces allowed.
  * `--indexName` &nbsp;&nbsp;&nbsp; If you wish to change the name of the imported monitoring index you can use this parameter and 
  maintain more partitioning and maintenance control. if set, whatever value you use will be appended to `.monitoring-es-7-`. If you do not specify this, the imported data will be indexed into
  the standard monitoring index name format with the current date appended. No spaces allowed.
* Once the data is imported you should be able to view the newly imported data via monitoring pages right away. IMPORTANT: Make sure to set the date 
range to reflect the period that was collected so that it displays and is in a usable format. If you don't see your cluster or data is missing/truncated, try expanding the range.
 
#### Examples 

##### Uses the default cluster_id, index_name:
```$xslt
    sudo ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip 
```
##### Uses the generated index name but gives the cluster a different name:
```$xslt
    sudo ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip -clusterName messed_up_cluster
```
##### Uses a custom index and cluster name:
```$xslt
    sudo ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip  -clusterName big_cluster --indexName big_cluster_2019_10_01    
```
# Standard Diagnostic Troubleshooting
  * The file: diagnostic.log file will be generated  and included in the archive. In all but the worst case an archive will be created. Some messages will be written to the console output but granualar errors and stack traces will only be written to this log.
  * If you get a message saying that it can't find a class file, you probably downloaded the src zip instead of the one with "-dist" in the name. Download that and try it again.
  * If you get a message saying that it can't locate the diagnostic node, it usually means you are running the diagnostic on a host containing a different node than the one you are pointing to. Try running in remote node or changing the host you are executing on.
  * Make sure the account you are running from has read access to all the Elasticsearch log directories.  This account must have write access to any directory you are using for output.
  * Make sure you have a valid Java installation that the JAVA_HOME environment variable is pointing to.
  * IBM JDK's have proven to be problematic when using SSL. If you see an error with _com.ibm.jsse2_ in the stack trace please obtain a recent Oracle or OpenJDK release and try again.
  * If you are not in the installation directory CD in and run it from there.
  * If you encounter OutOfMemoryExceptions, use the `DIAG_JAVA_OPTS` environment variable to set an `-Xmx` value greater than the standard `2g`.  Start with `-Xmx4g` and move up from there if necessary.
  * If reporting an issue make sure to include that.
  * And if the message tells you that you are running an outdated diagnostic, do not ignore it. Upgrade and see if the issue persists.
