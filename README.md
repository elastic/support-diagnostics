## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the host it is running on to obtain data and statistics on the running cluster. 
It will execute a series of REST API calls to the running cluster, run a number of OS related commands(such as top, netstat, etc.), and collect logs, then bundle them into one or more archives.

* Compatible with versions 6.x, 5.x, 2.x, 1.x
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
* JDK - Oracle or OpenJDK, 1.8-10
  * **Important Note:** The 1.7 version of the JDK is no longer supported. If you are running a 1.7 JRE/JDK you must upgrade or run the 6.3 version of the diagnostic.
* A JRE may be used, however certain functionality such as jstack generated thread dumps will not be available.
* If you are running a package installation under Linux you MUST run the command with elevated sudo privileges. Otherwise the utility will not be able to run the system queries.
* It is recommended that you set the `JAVA_HOME` environment variable.  It should point to the Java installation directory.  If `JAVA_HOME` is not found, the utility will attempt to locate a distribution but if errors occur it may be necessary to set this manually.
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
* Run the application via the diagnostics.sh or diagnostics.bat script. The host name or IP address used by the HTTP connector of the node is required.
* In order to assure that all artifacts are collected it is recommended that you run the tool with elevated privileges. This means sudo on Linux type platforms and via an Administor Prompt in Windows.
* A hostname or IP address must now be specified via the `--host` parameter. This is due to the changes in default port binding what were introduced starting with version 2. You must supply this even if you are running as localhost.
* The utility will use default listening port of `9200` if you do not specify one.
* If the utility cannot find a running ES version for that host/port combination the utility will exit and you will need to run it again.
* Input parameters may be specified in any order.
* An archive with the format diagnostics-`<DateTimeStamp>`.tar.gz will be created in the utility directory. If you wish to specify a specific output folder you may do so by using the -o `<Full path to custom output folder>` option.


#### Basic Usage Examples
    NOTE: Windows users use `diagnostics` instead of `./diagnostics.sh`
    sudo ./diagnostics.sh --host localhost
    sudo ./diagnostics.sh --host 10.0.0.20
    sudo ./diagnostics.sh --host myhost.mycompany.com
    sudo ./diagnostics.sh --host 10.0.0.20 --port 9201
    sudo ./diagnostics.sh --host localhost -o /home/myusername/diag-out

#### Getting Command Line Help
    /diagnostics.sh --help

## Using With Shield/Security
* a truststore does not need to be specified - it's assumed you are running this against a node that you set up and if you didn't trust it you wouldn't be running this.
* When using Shield authentication, do not specify a password and the `-p` option.  Using the `-p` option will bring up a prompt for you to type an obfuscated value that will not be displayed on the command history.
* `--noVerify` will bypass hostname verification with SSL.
* `--keystore` and --keystorePass` allow you to specify client side certificates for authentication.
* To script the utility when using Shield/Security, you may use the `--ptp` option to allow you to pass a plain text password to the command line rather than use `-p` and get a prompt.  Note that this is inherently insecure - use at your own risk.

#### Examples - Without SSL
    sudo ./diagnostics.sh --host localhost -u elastic -p
    sudo ./diagnostics.sh --host 10.0.0.20 -u elastic -p
#### Example - With SSL
    sudo ./diagnostics.sh --host 10.0.0.20 -u <your username> -p --ssl

## Additional Options
* You can specify additional java options such as a higher `-Xmx` value by setting the environment variable `DIAG_JAVA_OPTS`.
* To suppress all log file collection use the `--skipLogs` option.
* Because of the potential size access logs are no longer collected by default. If you need these use the `--accessLogs` option to have them copied.

## Alternate Usages

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

### Multiple Runs At Timed Intervals
This option has been removed. If you used this feature or have a use case for this functionality, please open an issue.

#### Examples - 6 runs with 20 seconds separating each run
    sudo ./diagnostics.sh --host localhost -u elastic -p --interval 600 --reps 6
    sudo ./diagnostics.sh --host localhost -u elastic -p --interval 600 --reps 6 --type remote
    sudo ./diagnostics.sh --host localhost -u elastic -p --interval 600 --reps 6 --type logstash 

## File Sanitization Utility

#### Description
* Works on diagnostics produced by version 6.4 and later. Please don't turn in a ticket if you run it on an older one.
* Runs as a separate application from the diagnostic. It does not need to be run on the same host the diagnostic utility ran on.
* Inputs an archive produced via a normal diagnostic run.
* Goes through each file in that archive line by line and does the following:
  * Automatically obfuscates all IPv4 and IPv6 addresses. These will be consistent throughout all files in the archive. In other words, it encounters 10.0.0.5 in one file, the obfuscated value will be used for all occurrences of the IP in other files. These will not, however be consistent from run to run.
  * Obfuscates MAC addresses.
  * If you include a configuration file of supplied string tokens, any occurrence of that token will be replaced with a generated replacement. As with IP's this will be consistent from file to file but not between runs. Literal strings or regex's may be used.
* Re-archives the file with "scrubbed-" prepended to the  name. An example file (`scrub.yml`) is included as an example.
* If you are processing a large cluster's diagnostic, this may take a while to run, and you may need to use the `DIAG_JAVA_OPTS` environment variable to bump up the Java Heap if you see OutOfMemoryExceptions.

#### How to run
* Run the diagnostic utility to get an archive.
* Add any tokens for text you wish to conceal to a config file. By default the utility will look for scrub.yml in the working directory.
* Run the utility with the necessary and optional diagnosticInputs. It is a different script execution than the diagnostic with different arguments.
  * *-a* &nbsp;&nbsp;&nbsp; An absolute path to the archive file you wish to sanitize(required)
  * *-t* &nbsp;&nbsp;&nbsp; A target directory where you want the revised archive written. If not supplied it will be written to the same folder as the diagnostic archive it processed.
  * *-f* &nbsp;&nbsp;&nbsp; A file containing any text tokens you wish to conceal. These can be literals or regex's. 

#### Examples
#####With no tokens specified, writing the same directory as the diagnostic:
```$xslt
./scrub.sh -a diagnostics-20180621-161231.tar.gz
```
#####With a token file writing to a specific output directory:
```$xslt
./scrub.sh -a diagnostics-20180621-161231.tar.gz -t /home/adminuser/sanitized-diags -f /home/adminuser/sanitized-diags/scrub.yml
```
#####Sample token scrub file entries:
```$xslt
tokens:
  - node-[\d?]*
  - cluster-630
  - disk1
  - Typhoid

```
###  Local Artifacts Collection
##### Important - Please do not run this option when asked for an initial diagnostic unless _explicitly_ instructed to do so by support! It is also currently under review and may be removed in the future.
* This option will collect logs for the node on the current host and run the system statistics calls. It will allow the logs to be collected for a non-running or hung node without having to manually go to the log directory. It will follow the same rules as standard log collection. It will collect the current, as well as the last two rollovers, any slow logs, and three gc logs.
* It runs via a separate script than the standard diagnostic function.
* The standard REST API calls used for the normal and remote types will not be run.
* The user *must* specify the log location via the command line via the -l or --logDir option.
* If a full set of system calls is desired for a running Elasticsearch instance you must specify the process id with -p or --pid. Optional, will collect calls that do not require a pid otherweise.
* Output written to the diagnostics directory by default, otherwise specify a custom location with -o or --outputDir.
#### Examples
```
 ./log-collection.sh -l /var/log/elasticsearch -o /admin/storage/logs -p 444999
 ./log-collection.sh --logDir /var/log/elasticsearch --outpotDir /admin/storage/logs --pid 444999

```

# Troubleshooting
  * The file: diagnostic.log file will be generated  and included in the archive. In all but the worst case an archive will be created. Some messages will be written to the console output but granualar errors and stack traces will only be written to this log.
  * If you get a message saying that it can't find a class file, you probably downloaded the src zip instead of the one with "-dist" in the name. Download that and try it again.
  * If you get a message saying that it can't locate the diagnostic node, it usually means you are running the diagnostic on a host containing a different node than the one you are pointing to. Try running in remote node or changing the host you are executing on.
  * Make sure the account you are running from has read access to all the Elasticsearch log directories.  This account must have write access to any directory you are using for output.
  * Make sure you have a valid Java installation that the JAVA_HOME environment variable is pointing to.
  * If you are not in the installation directory CD in and run it from there.
  * If you encounter OutOfMemoryExceptions, use the `DIAG_JAVA_OPTS` environment variable to set an `-Xmx` value greater than the standard `2g`.  Start with `-Xmx4g` and move up from there if necessary.
  * If reporting an issue make sure to include that.
