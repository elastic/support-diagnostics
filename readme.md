## Support Diagnostics Utilities
The support diagnostic utilities consists of two self contained, executable Java jar files. One which will interrogate the cluster for settings and state information, and one that is run on the hosts the nodes are deployed on to collect configuration files, logs, and host system settings. Current features include:

* No runtime requirements ore dependencies other than a recent JRE
* OS specific versions are not required.
* Either application can be run from any directory on the machine.  They do not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts and write access to the chosen output directory.
* The diagnostics-stats application which retrieves cluster information can be run remotely from an administrator or developer desktop.
* The diagnostics collector application which retrieves file and system artifacts will process multiple nodes per host and is able to retrieve them from user specified locations that deviate from the defaults.
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
* Run the command *mvn clean install*
* The two jars will be installed into the */es-support-diagnostics/target* folder as well as the target folders in the individual submodules.

## Usage instructions
### diagnostics-stats overview
The diagnostic-stats application performs two functions.  First it will execute a series of HTTP or HTTPS REST requests to a specified node in the cluster to be diagnosed.  It will write the responses from each of these to a file in a temporary directory, archive the output, and then delete the temp directory.  The file will always be named <cluster name>.zip.  If the application is run twice, the second run will remove the previous zip file if it is present, so if you wish to preserve previous runs you will need to rename the existing file prior to running again.

It will also produce a file with the name format <cluster name>-manifest.json.  This file contains information about the where the diagnostic artifacts such as log and configuration files that must be obtained from the individual hosts the nodes are installed on.  This file will be used as an input for the diagnostics-collector, which should then be run on the host where the nodes for which you need logs and configuration files are installed.  You do not require separate manifests for each host.  One manifest will work for all the host/node combinations in the cluster.  It will have the format:
```
{
  "clusterName": "test",
  "collectionDate": "06/22/2015 01:57:37 PM -0400",
  "nodes": [
    {
      "conf": "",
      "config": "",
      "home": "/Users/someuser/elasticsearch/elasticsearch-1.5.2",
      "host": "myhost",
      "ip": "192.168.5.175",
      "logs": "/Users/someuser/elasticsearch/elasticsearch-1.5.2/logs",
      "name": "node1"
    }
  ]
}
```
For the nodes array, each node in the cluster will have an entry that contains the specifics of that nodes configuration.  Note that if you have not explicitly set these values and are running with defaults you may not see values here.  As with the zip archive, new runs will replace the old file.
### diagnostics-stats step by step
* Copy the diagnostics-stats.jar to the directory from which you intend to run the application.  You will need sufficient permissions to execute the application.
* Run the application with *java -jar diagnostic-stats.jar -n <host or IP address>*
* Executing the application with no options or an invalid one will display the help command output.
* The host name or IP address can be any node on the cluster that has HTTP access enabled.  Client, master, or data nodes will all succeed as long as they have a working HTTP listening port.
* The application will create two sets of output, a zipped archive file containing a collection of statistics acquired from the cluster and a manifest file that contains information about the specific location of node specific artifacts, such as log and configuration files.  This file will be used by the diagnostics-collector application to retrieve these in the second stage of diagnostics retrieval.
* Use -o, --out, --output, --outputDir to write these files to a directory other than the current working directory, which is the default. Make sure that you have write permissions to this target directory and that you give it an absolute path( /Users/someuser/elasticsearch, c:\Users\someuser\elasticsearch, etc.) or the application will fail.
* If the node you wish to query is using a different HTTP port you can use the -t or --port options to specify it manually.  Otherwise it will use the default 9200.
* When you are authenticating via Shield specify the user id and use one of the password flags: -p --password, --pwd.  Do not specify the password on the command line. You will receive a prompt to enter it after you hit the enter key and the entered text will be hidden.
* HTTPS connections are enabled using the -s, --ssl, or --https flags.  As with the password, use the option alone with no content.
* The commands used to retrieve the cluster stats are contained in the stats.yml in the root directory of the jar file.  If you wish to modify these commands or add additional ones you can specify an alternative configuration file using -c or --commandConfig along with the filename .  As with the output directory, please make sure this is an absolute path.
* Logback is used for logging, and the configuration is contained in the logback.xml file in the root directory of the jar file.  If you wish to modify these commands or add additional ones you can specify an alternative configuration file via -l or --logConfig along with the filename.  As with the output directory, please make sure this is an absolute path.
* Unless the log file is modified to change the output file, detailed logging and diagnostics will be written to <user home>/es-diags/logs/stats.log.
* If you have made changes to the cluster, such as adding or removing a node, changing a host name or ip, etc., you can regenerate just the manifest file by using -g, -gen, or -genManifest. Only the flag is used, no additional input is necessary.

### Help command content
``````
Usage: <main class> [options]
  Options:
    -h, -?, --help, -help
       Default: false
  * -n, --host, --name, --hostname, -ip
       Hostname, IP Address, or localhost if a node is present on this host that
       is part of the cluster.  Required.
    -s, --ssl, --https
       Use SSL?  No value required, only the option.
       Default: false
    -t, --port, --listen
       HTTP or HTTPS listening port.
       Default: 9200
    -l, --logConfig
       Alternative log configuration file in logback format. Be sure to enter
       with a fully qualified path name.
    -o, --out,  --output, --outputDir
       Fully qualified path to output directory or cwd for current working
       directory.
       Default: cwd
    -p, --password, --pwd
       Prompt for a password?  No password value required, only the option.
       Hidden from the command line on entry.
    -u, --user
       Username
    -c, --commandConfig
       Use this context configuration file instead of the default. Be sure to
       enter with a fully qualified path name.
    -g, --gen, --genManfiest
       Generate only the cluster manifest for log and configuration collection.
       No value required, only the option.
       Default: false
``````

### diagnostics-collector
### diagnostics-collector overview
The diagnostics-collector application uses the manifest file generated by the diagnostic-stats application to retrieve and package log and config files, and in addition collects the output from additional sources such as top and netstat. The elasticsearch.yml or the alternative config file specified, the last created log, named <cluster name>.log, and the two slow logs will be retrieved. The same generated manifest file will work for all host/node installations.  It will check the possible host names and IP addresses on the system where it is being run and if one of the node entries in the manfiest corresponds it will process that node.  If multiple nodes are installed on a single host it will process all of them in the same run.  Each node will have its artifacts contained in a subdirectory named for the node.  No OS specific flags are necessary - it will detect the OS at runtime. Configuration files that have been installed to locations other than <elassticsearch-home>/config and /etc/elasticsearch will be retrieved since the exact location will be contained in the manifest.

Similar to diagnostics-stats, it will write the responses from each of these to a file in a temporary directory, archive the output, and then delete the temp directory.  The file name will always be named diagnostic-artifacts-<cluster name>-<host name>.zip.  If the application is run twice, the second run will remove the previous output if it is present, so if you wish to preserve previous runs you will need to rename the file prior to running again.
### diagnostics-collector step by step
* Copy the diagnostics-collector.jar to the directory from which you intend to run the application.  You will need sufficient permissions to execute the application.
* Run the application with *java -jar diagnostic-collector.jar -m manifest file>*
* Executing the application with no options or an invalid one will display the help command output.
* The manifest file must be specified with an absolute path.
* The application will create a zipped archive file containing a collection of artifacts acquired from any node from the cluster installed on this host.  Multiple nodes
* Use -o, --out, --output, --outputDir to write these files to a directory other than the current working directory, which is the default. Make sure that you have write permissions to this target directory and that you give it an absolute path( /Users/someuser/elasticsearch, c:\Users\someuser\elasticsearch, etc.) or the application will fail.
* Unless the log file is modified to change the output file, detailed logging and diagnostics will be written to <user home>/es-diags/logs/collector.log.
* The commands used to retrieve the system related stats are contained in the cmds.yml in the root directory of the jar file.  If you wish to modify these commands or add additional ones you can specify an alternative configuration file using -c or --commandConfig along with the filename .  As with the output directory, please make sure this is an absolute path.
* Logback is used for logging, and the configuration is contained in the logback.xml file in the root directory of the jar file.  If you wish to modify these commands or add additional ones you can specify an alternative configuration file via -l or --logConfig along with the filename.  As with the output directory, please make sure this is an absolute path.

### Help command content
```
Usage: <main class> [options]
  Options:
    -c, --configFile
       Use this configuration file instead of the default. Be sure to enter with
       a fully qualified path name.
    -h, -?, --help, -help
       Default: false
    -n, --host, --name, --hostname, -ip
       Hostname or IP Address used by the cluster and present in the manifest
       file.  Required.
    -l, --logConfig
       Alternative log configuration file in logback format. Be sure to enter
       with a fully qualified path name.
  * -m, --manifestFile, --manifest
       Manifest file generated by the diagnostics-stats application for this
       cluster. Be sure to enter with a fully qualified path name. Required
    -o, --out,  --output, --outputDir
       Fully qualified path to output directory or c for current working
       directory.
       Default: cwd
```
