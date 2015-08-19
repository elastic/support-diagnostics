## Support Diagnostics Utilities
The support diagnostic utilities consists of two self contained, executable Java jar files. One which will interrogate the cluster for settings and state information, and one that is run on the hosts the nodes are deployed on to collect configuration files, logs, and host system settings. Current features include:

* No runtime requirements ore dependencies other than a recent JRE
* OS specific versions are not required.
* The application can be run from any directory on the machine.  It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts and write access to the chosen output directory.
* Supports multiple network interfaces per host.
* Shield authentication, SSL and cookies are supported for REST calls to the cluster.

## Build Requirements
* Maven 3.x
* JDK - Oracle or OpenJDK, 1.7 or 1.8

## Run Requirements
* JRE - Oracle or OpenJDK, 1.7 or 1.8
* For the diagnostics collector Linux, Windows, and Mac OSX are supported.

## Instructions For Building
* The project structure consists of a main parent module with two enclosed submodules, one for each application.
* Check out the project from github to your sandbox directory.  For instructions on how to do this consult the documentation on Github.
* Install Maven and ensure that both the JAVA_HOME and MAVEN_HOME environment variables are set correctly.
* Open a terminal window and navigate to the *es-support-diagnostics folder.
* Run the command *mvn clean package*
* A zip containing the complete distribution will be placed into the */es-support-diags/target* folder.

## Usage instructions
### diagnostics-stats overview
The diagnostic application performs three functions.  First it will execute a series of HTTP or HTTPS REST requests to a specified node in the cluster to be diagnosed.  It will write the responses from each of these to a file in a temporary directory, archive the output, and then delete the temp directory.  The file will always be named <cluster name>.zip.  If the application is run twice, the second run will remove the previous zip file if it is present, so if you wish to preserve previous runs you will need to rename the existing file prior to running again.

### diagnostics-stats step by step
* Unzip the support-diagnostics-dist.zip to the directory from which you intend to run the application.  You will need sufficient permissions to execute the application.
* Run the application via the diagnostics.sh or diagnostics.bat script.
* Executing the application with no options or an invalid one will display the help command output.
* The host name or IP address can be any node on the cluster that has HTTP access enabled.  Client, master, or data nodes will all succeed as long as they have a working HTTP listening port.
* The application will create a zipped archive file containing a collection of statistics acquired from the cluster and any specific logging or configuration information for the nodes(s) residing on the host from which it is run.
* When you are authenticating via Shield specify the user id and use one of the password flags: -p --password, --pwd.  Do not specify the password on the command line. You will receive a prompt to enter it after you hit the enter key and the entered text will be hidden.
* HTTPS connections are enabled using the -s, --ssl, or --https flags.  As with the password, use the option alone with no content.
* The commands used to retrieve the cluster stats are contained in the diags.yml in the installation directory.  If you wish to modify these commands simply customize the settings to your needs.
* Logback is used for logging, and the configuration is contained in the logback.xml file in the installation directory.  If you wish to modify these commands simply customize the settings to your needs.
* Unless the log file is modified to change the output file, detailed logging and diagnostics will be written to <working directory>/diag-logs/logs/diagnostics.log.

### Help command content
``````
  Options:
    -h, -?, --help
       Default: false
        --host
       Hostname, IP Address, or localhost if a node is present on this host that
       is part of the cluster and that has HTTP access enabled.
       Default: localhost
    -s, --ssl, --https
       Use SSL?  No value required, only the option.
       Default: false
    --port, --listen
       HTTP or HTTPS listening port.
       Default: 9200
    -o, --out,  --output, --outputDir
       Fully qualified path to output directory or c for current working
       directory.
       Default: cwd
    -p, --password, --pwd
       Prompt for a password?  No password value required, only the option.
       Hidden from the command line on entry.
    -u, --user
       Username

``````
