## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the the host it is running on to obtain data and statistics on the running cluster.  There are a number of changes from the previous script based version:

* No runtime requirements or dependencies other than a recent JRE
* OS specific versions are not required.
* The application can be run from any directory on the machine.  It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts, write access to the chosen output directory, an## Support Diagnostics Utility
The support diagnostic utility is a Java executable that will interrogate the node on the the host it is running on to obtain data and statistics on the running cluster.  There are a number of changes from the previous script based version:

* No runtime requirements or dependencies other than a recent JRE
* OS specific versions are not required.
* The application can be run from any directory on the machine.  It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts, write access to the chosen output directory, and sufficient disk space for the generated archive.
* Detects multiple nodes and network interfaces per host.
* Shield authentication, SSL and cookies are supported for REST calls to the cluster.

## Run Requirements
* JRE - Oracle or OpenJDK, 1.7 or 1.8
* JAVA_HOME environment variable should point to the Java installation directory.  If JAVA_HOME is not found, the utility will attempt to locate a distribution but if errors occur it may be necessary to set this manually.
* Account running the utility must have read access to the Elasticsearch files and write access to the output location.
* Linux, Windows, and Mac OSX are supported.

## Installation And Usage Instructions
* Unzip the support-diagnostics-<version>-dist.zip into the directory from which you intend to run the application.
* Switch to the diagnostics distribution directory.
* Run the application via the diagnostics.sh or diagnostics.bat script. The host name or IP address used by the HTTP connector of the node is required.
* If you omit the --host parameter you will be presented with a list of network interfaces to choose from.  You'll also be required to enter a port, even if you are running on the default.  If the selections do not specify a listening interface the utility will fail and you will need to run it again.
* Executing the application with --? or --help will display the help command output.
* Supplied arguments are not required to be in any particular order.

## Simple Examples
  * `./diagnostics.sh --host 192.168.137.1`
  * `./diagnostics.sh --host 192.168.137.1 -port 9201`

## Using Shield Authentication
  * `./diagnostics.sh --host 192.168.137.1 -u <your username> -p`
  * `./diagnostics.sh --host 192.168.137.1 --user <your username> -p`
  * `./diagnostics.sh --host 192.168.137.1 --user <your username> --password`
  * Do **not** specify a password on the command line, only the flag.  You will be prompted for the password and it will be hidden.

 ## Using Shield Authentication And SSL
  * `./diagnostics.sh --host 192.168.137.1 -u <your username> -p -s`

### Help command content
``````
  Options:
    -h, -?, --help
       Default: false
   --host
       Hostname, IP Address, or localhost if a node is present on this host that
       is part of the cluster and that has HTTP access enabled. Required.
    -s, --ssl, --https
       Use SSL?  No value required, only the option.
       Default: false
    --port, --listen
       HTTP or HTTPS listening port.
       Default: 9200
    -o, --out, --output, --outputDir
       Fully qualified path to output directory or c for current working
       directory.
       Default: current working directory
    -p, --password, --pwd
       Prompt for a password?  No password value required, only the option.
       Hidden from the command line on entry.
    -u, --user
       Username
    -r, --reps
       Number of times to execute the diagnostic. Use to create multiple runs at timed intervals.
    -i --interval
       Elapsed time in seconds between diangostic runs when in repeating mode.  Minimum value is 30.

