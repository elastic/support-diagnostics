## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the the host it is running on to obtain data and statistics on the running cluster.  There are a number of changes from the previous script based version:

* No runtime requirements or dependencies other than a recent JRE
* OS specific versions are not required.
* The application can be run from any directory on the machine.  It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts, write access to the chosen output directory, and sufficient disk space for the generated archive.
* Detects multiple nodes and network interfaces per host.
* Shield authentication, SSL and cookies are supported for REST calls to the cluster.

## Run Requirements
* JRE - Oracle or OpenJDK, 1.7 or 1.8
* It is recommended that you set the JAVA_HOME environment variable.  It should point to the Java installation directory.  If JAVA_HOME is not found, the utility will attempt to locate a distribution but if errors occur it may be necessary to set this manually.
* The system account running the utility must have read access to the Elasticsearch files and write access to the output location.
* If you are using Shield the supplied user id must have permission to execute the diagnostic URL's.
* Linux, Windows, and Mac OSX are supported.
* Docker installations may have issues depending on the individual configuration.

## Installation And Usage Instructions
* Download [support-diagnostics-current.zip](https://github.com/elastic/elasticsearch-support-diagnostics/releases/latest) from the Github release area.
* Unzip the support-diagnostics-<version>-dist.zip into the directory from which you intend to run the application.
* Switch to the diagnostics distribution directory.
* Run the application via the diagnostics.sh or diagnostics.bat script. The host name or IP address used by the HTTP connector of the node is required.
* A hostname or IP address must now be specified via the --host parameter. This is due to the changes in default port binding what were introduced starting with version 2.
* The utility will still attempt to use a default listening port of 9200 if you do not specify one.
* If prompted, you will also need to enter the port, even if it is set for the default of 9200.
* If the utility cannot find a running ES version for that host/port combination the utility will exit and you will need to run it again.
* Input parameters may be specified in any order.
* When using Shield authentication, do not specify a password.  Using the -p option will bring up a prompt for you to type one that will not be displayed on the command line.
* To get help for input options run the diagnostic with the --help option
* An archive with the format <cluster name>-cluster-diagnostic-<Date Time Stamp>.tar.gz will be created in the working or output directory.
* You can specify additional java options such as a higher -Xmx value by setting the environment variable DIAG_JAVA_OPTS.
* A diagnostic.log file will be generated in the installation directory of the diagnostic utility - the output of the console, which will include both progress and error messages, will be replicated in that file.  It will be appended for each run and rolled over daily if not removed.
* Additional compression can be obtained by running with the --bzip option.
* To include all logs, not just today's use the --archivedLogs option.
* To script the utility when using Shield, use the --ptp option to allow the addition of a plain text password via the command line.  Note that this is inherently insecure so use at your own risk.
* --noVerify will bypass hostname verification with SSL. Again, this is a security hole so use at your own risk.
* --keystore and --keystorePass allow you to specify client side certificates for authentication.
## Examples
 *NOTE:* Windows users use diagnostics instead of ./diagnostics.sh

## Getting Command Line Help
 * /diagnostics.sh --help

## Basic Runs
  * ./diagnostics.sh --host 192.168.137.10
  * ./diagnostics.sh --host 192.168.137.10 --port 9201

## Running remotely - does not collect logs, configs or run system commands.  Can be executed from a desktop without ES installed.
  * ./diagnostics.sh --host 192.168.137.10 --type remote

## Specifying a custom output directory
  *  ./diagnostics.sh --host 192.168.137.10 -o <full path to output directory>

## Using Shield Authentication
  * ./diagnostics.sh --host 192.168.137.10 -u <your username> -p
  * ./diagnostics.sh --host 192.168.137.10 --user <your username> -p
  * ./diagnostics.sh --host 192.168.137.10 --user <your username> --password

  * Do not specify a password on the command line, only the flag.  You will be prompted for the password and it will be hidden.

## Using Shield Authentication And SSL
  * ./diagnostics.sh --host 192.168.137.10 -u <your username> -p -s

# Troubleshooting
  * Make sure the account you are running from has read access to all the Elasticsearch log and config directories.  This account must have write access to any directory you are using for output.
  * Make sure you have a valid Java installation that the JAVA_HOME environment variable is pointing to.
  * If you are not in the installation directory CD in and run it from there.
  * If you encounter OutOfMemoryExceptions, use the DIAG_JAVA_OPTS environment variable to set an -Xmx value greater than the standard 2g.  Start with -Xmx4g and move up from there if necessary.
  * All errors are logged to diagnostics.log and will be written to the working directory.  If reporting an issue make sure to include that.
