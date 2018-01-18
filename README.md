## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the the host it is running on to obtain data and statistics on the running cluster. 
It will execute a series of REST API calls to the running cluster, run a number of OS related commands(such as top, netstat, etc.), and collect logs, then bundle them into one or more archives.

* Compatible with versions 6.x, 5.x, 2.x, 1.x
  * Note: the version of this tool is independant of the version of Elasticsearch/Logstash targeted.
* No runtime requirements or dependencies other than a recent JRE
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
* JDK **strongly recommended** - Oracle or OpenJDK, 1.7 or 1.8
* A JRE may be used, however certain functionality such as jstack generated thread dumps will not be available.
* If you are running a package installation under Linux you MUST run the command with elevated sudo privileges. Otherwise the utility will not be able to run the system queries.
* It is recommended that you set the JAVA_HOME environment variable.  It should point to the Java installation directory.  If JAVA_HOME is not found, the utility will attempt to locate a distribution but if errors occur it may be necessary to set this manually.
* The system account running the utility must have read access to the Elasticsearch files and write access to the output location.
* If you are using Shield/Security the supplied user id must have permission to execute the diagnostic URL's.
* Linux, Windows, and Mac OSX are supported.
* Docker installations should use the --type remote option. See below for examples. 

## Installation
* Download [support-diagnostics-latest-dist.zip](https://github.com/elastic/elasticsearch-support-diagnostics/releases/latest) from the Github release area.
* Unzip the support-diagnostics-`<version>`-dist.zip into the directory from which you intend to run the application.
* Switch to the diagnostics distribution directory.

## Usage - Simplest Case
* Run the application via the diagnostics.sh or diagnostics.bat script. The host name or IP address used by the HTTP connector of the node is required.
* In order to assure that all artifacts are collected it is recommended that you run the tool with elevated privileges. This means sudo on Linux type platforms and via an Administor Prompt in Windows.
* A hostname or IP address must now be specified via the --host parameter. This is due to the changes in default port binding what were introduced starting with version 2. You must supply this even if you are running as localhost.
* The utility will use default listening port of 9200 if you do not specify one.
* If the utility cannot find a running ES version for that host/port combination the utility will exit and you will need to run it again.
* Input parameters may be specified in any order.
* An archive with the format diagnostics-`<DateTimeStamp>`.tar.gz will be created in the utility directory. If you wish to specify a specific output folder you may do so by using the -o `<Full path to custom output folder>` option.


#### Basic Usage Examples
    * NOTE: Windows users use `diagnostics` instead of `./diagnostics.sh`
    * sudo ./diagnostics.sh --host localhost
    * sudo ./diagnostics.sh --host 10.0.0.20
    * sudo ./diagnostics.sh --host myhost.mycompany.com
    * sudo ./diagnostics.sh --host 10.0.0.20 --port 9201
    * sudo ./diagnostics.sh --host localhost -o /home/myusername/diag-out

#### Getting Command Line Help
    * /diagnostics.sh --help

## Using With Shield/Security
* a truststore does not need to be specified - it's assumed you are running this against a node that you set up and if you didn't trust it you wouldn't be running this.
* When using Shield authentication, do not specify a password and the -p option.  Using the -p option will bring up a prompt for you to type an obfuscated value that will not be displayed on the command history.
* --noVerify will bypass hostname verification with SSL.
* --keystore and --keystorePass allow you to specify client side certificates for authentication.
* To script the utility when using Shield/Security, you may use the --ptp option to allow you to pass a plain text password to the command line rather than use -p and get a prompt.  Note that this is inherently insecure - use at your own risk.

#### Examples - Without SSL
    * sudo ./diagnostics.sh --host localhost -u elastic -p
    * sudo ./diagnostics.sh --host 10.0.0.20 -u elastic -p
#### Example - With SSL
    * sudo ./diagnostics.sh --host 10.0.0.20 -u <your username> -p --ssl

## Additional Options
* You can specify additional java options such as a higher -Xmx value by setting the environment variable DIAG_JAVA_OPTS.
* To include all logs, not just today's use the --archivedLogs option.
* To suppress all log file collection use the --skipLogs option.
* Because of the potential size access logs are no longer collected by default. If you need these use the --accessLogs option to have them copied.
* If you have sensitive information in your logs and wish to scrub them, you will need to make sure your last three logs are unzipped before running the utility. Configure the scrub.yml to designate the sensitive character data you wish to modify and the value you wish to substitute. You can the trigger the replacement process with the --scrub option.

## Alternate Usages
### Remote
* If you do not wish to run the utility on the host the node to be queried resides on, and wish to run it from a different host such as your workstation, you may use the --type remote option.
* This will execute only the REST API calls and will not attempt to execute local system calls or collect log files. 
#### Remote Example
    * ./diagnostics.sh --host 10.0.0.20 --type remote

### Logstash Diagnostics
* Use the --type logstash argument to get diagnostic information from a running Logstash process. It will query the process in a manner similar to the Elasticsearch REST API calls.
* The default port will be 9600. This can be modified at startup, or will be automatically incremented if you start multiple Logstash processes on the same host. You can connect to these other Logstash processes with the --port option.
* Available for 5.0 or greater.
#### Logstash Examples
    * sudo ./diagnostics.sh --host localhost --type logstash
    * sudo ./diagnostics.sh --host localhost --type logstash --port 9610

### Multiple Runs At Timed Intervals
* If the cluster is not running X-Pack Monitoring you may find it beneficial to see how some statistics change over time. You can accomplish this by using the --interval x (in seconds) and --reps (times to repeat)to take a diagnostic.
* You run the diagnostic once and it will execute a run, sleep for the interval duration, and then take another diagnostic. 
* Each run will get it's own archive with the same DateTime stamp and with run-`<run number>` appended.
* Logs will only be collected in the archive of the final run. If you are running in standard rather than remote mode, however, all the system level calls will be executed.
* This can be used for either Elasticsearch or Logstash
#### Examples - 6 runs with 20 seconds separating each run
    * sudo ./diagnostics.sh --host localhost -u elastic -p --interval 20 --reps 6
    * sudo ./diagnostics.sh --host localhost -u elastic -p --interval 20 --reps 6 --type remote
    * sudo ./diagnostics.sh --host localhost -u elastic -p --interval 20 --reps 6 --type logstash 

### Timed Thread Dumps
* If you wish to take thread dumps at timed intervals without running the full gamut of API calls use the --type elastic-threads option. 
* For each run it will collect output from the Hot Threads API call, as well as running a full thread dump against the process using jstack.
* This **must** be run on the physical host of the node you will to check.
* You do not need to supply a process id, only the host/port. It will query the node for the information on the one running on that host and use it in the call.
* Since there are fewer calls for Logstash it does not have a separate type. Use the standard repitition options and both hot threads and jstack will be included.
#### Elasticseach Timed Thread Dumps Example
    * sudo ./diagnostics.sh --host localhost -u elastic -p --interval 20 --reps 6 --type elastic-threads    

### Heap Dumps
* If you wish to take a heap dump of a running process use the --heapdump --nodename processoption

# Troubleshooting
  * The file: diagnostic.log file will be generated in the installation directory of the diagnostic utility - the output of the console, which will include both progress and error messages, will be replicated in that file.  It will be appended for each run and rolled over daily if not removed.
  * Make sure the account you are running from has read access to all the Elasticsearch log directories.  This account must have write access to any directory you are using for output.
  * Make sure you have a valid Java installation that the JAVA_HOME environment variable is pointing to.
  * If you are not in the installation directory CD in and run it from there.
  * If you encounter OutOfMemoryExceptions, use the DIAG_JAVA_OPTS environment variable to set an -Xmx value greater than the standard 2g.  Start with -Xmx4g and move up from there if necessary.
  * All errors are logged to diagnostics.log and will be written to the working directory.  If reporting an issue make sure to include that.
