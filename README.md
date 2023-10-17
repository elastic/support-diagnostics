- [Support Diagnostics Utility](#support-diagnostics-utility)
  - [Overview - What It Does](#overview---what-it-does)
  - [License](#license)
  - [Installation And Setup](#installation-and-setup)
    - [Run Requirements](#run-requirements)
    - [Downloading And Installing](#downloading-and-installing)
    - [Building From Source](#building-from-source)
    - [Creating A Docker Image](#creating-a-docker-image)
  - [Running The Diagnostic Utility](#running-the-diagnostic-utility)
    - [Interactive Mode - For Those Who Don't Like To Read Documentation](#interactive-mode---for-those-who-dont-like-to-read-documentation)
    - [Running From The Command Line](#running-from-the-command-line)
      - [Diagnostic Types](#diagnostic-types)
      - [Standard Options](#standard-options)
      - [PKI Authentication Options](#pki-authentication-options)
      - [Http Proxy Options](#http-proxy-options)
      - [Remote Execution Options](#remote-execution-options)
      - [Usage Examples](#usage-examples)
      - [Customizing What Is Collected](#customizing-what-is-collected)
        - [The Config Directory](#the-config-directory)
        - [Removing Or Modifying Calls](#removing-or-modifying-calls)
        - [Preventing Retries](#preventing-retries)
      - [Executing Scripted Runs](#executing-scripted-runs)
  - [Docker](#docker)
    - [Elasticsearch Deployed In Docker Containers](#elasticsearch-deployed-in-docker-containers)
    - [Running From A Docker Container](#running-from-a-docker-container)
  - [File Sanitization Utility](#file-sanitization-utility)
    - [Overview - How Sanitization Works](#overview---how-sanitization-works)
    - [Running The Santizer](#running-the-santizer)
      - [Sanitization Options](#sanitization-options)
      - [Sanitization Examples](#sanitization-examples)
  - [Extracting Time Series Diagnostics From Monitoring](#extracting-time-series-diagnostics-from-monitoring)
    - [Monitoring Extract Overview](#monitoring-extract-overview)
    - [Running The Extract](#running-the-extract)
    - [Monitoring Extract Options](#monitoring-extract-options)
    - [Monitoring Extract Examples](#monitoring-extract-examples)
  - [Importing Extracted Monitoring Data](#importing-extracted-monitoring-data)
    - [Monitoring Import Overview](#monitoring-import-overview)
    - [Running The Monitoring Data Import](#running-the-monitoring-data-import)
    - [Monitoring Data Import Options](#monitoring-data-import-options)
    - [Monitoring Import Examples](#monitoring-import-examples)
  - [Standard Diagnostic Troubleshooting](#standard-diagnostic-troubleshooting)

# Support Diagnostics Utility

[Click here for the latest version of the Support Diagnostics Utility](https://github.com/elastic/support-diagnostics/releases/latest)

The support diagnostic utility is a Java application that can interrogate a running Elasticsearch cluster or Logstash process to obtain data about the state of the cluster at that point in time. It is compatible with all versions of Elasticsearch (including alpha, beta and release candidates), and for Logstash versions greater than 5.0, and for Kibana v6.5+. The release version of the diagnostic is independent of the Elasticsearch, Kibana or Logstash version it is being run against. If it cannot match the targeted version it will attempt to run calls from the latest configured release. Linux, OSX, or Windows platforms are all supported, and it can be run as a standalone utility or from within a Docker container.

## Overview - What It Does

When the utility runs it will first check to see if there is a more current version and display an alert message if it is out of date. From there it will connect to the host input in the command line parameters, authenticate if necessary, check the Elasticsearch version, and get a listing of available nodes and their configurations. From there it will run a series of REST API calls specific to the version that was found. If the node configuration info shows a master with an HTTP listener configured all the REST calls will be run against that master. Otherwise the node on the host that was input will be used.

Once the REST calls are complete, system calls such as top, netstat, iostat, etc. will be run against the specified host for the appropriate input diagnostic type selected. See specific documentation for more details on those type options. It will also collect logs from the node on the targeted host unless it is in REST API only mode.

The application can be run from any directory on the machine. It does not require installation to a specific location, and the only requirement is that the user has read access to the Elasticsearch artifacts, write access to the chosen output directory, and sufficient disk space for the generated archive.

## License

This software is licensed under [Elastic License v2](https://www.elastic.co/licensing/elastic-license).

## Installation And Setup

### Run Requirements

- JDK - Oracle or OpenJDK, 1.8-13.
  - The IBM JDK is not supported due to JSSE related issues that can cause TLS errors.
  - **Important Note For Elasticsearch Version 7:** Elasticsearch now includes a bundled JVM that is used by default. For the diagnostic to retrieve thread dumps via `Jstack` it must be executed with the same JVM that was used to run Elasticsearch. The diagnostic utility will attempt to find the location of the JVM that was used to run the process it is interrogating. If it is unable to do so, you may need to manually configure the location by setting `JAVA_HOME` to the directory containing the `/bin` directory for the included JDK. For example, `<path to Elasticsearch 7 deployment>/jdk/Contents/Home`.
- The system user account for that host(not the elasticsearch login) must have sufficient authorization to run these commands and access the logs (usually in `/var/log/elasticsearch`) in order to obtain a full collection of diagnostics.
- If you are authenticating using the built in Security, the supplied user id must have permission to execute the diagnostic URL's. The superuser role is recommended unless you are familar enough with the calls being made to tailor your own accounts and roles.

### Downloading And Installing

- Locate the [latest release](https://github.com/elastic/support-diagnostics/releases/latest)
- Select the zip file labeled diagnostics-XX.XX.XX-dist.zip to download the binary files. **_Do not select the zip or tar files labeled: 'Source code'._** These do not contain compiled runtimes and will generate errors if you attempt to use the scripts contained in them.
- Unzip the downloaded file into the directory you intend to run from. This can be on the same host as the as the Elasticsearch, Kibana or Logstash host you wish to interrogate, or on a remote server or workstation. You can also run it from within a Docker container(see further instructions down for generating an image).

### Building From Source

- Clone or download the Github repo. In order to clone the repo you must have Git installed and running. See the instructions appropriate for your operating system.
- Make sure you have a 1.8 JDK or greater. It **must** be a JDK, not a JRE or you will not be able to compile.
- Set the `JAVA_HOME` environment variable to point to your JDK installation.
- Make sure a recent version of Maven is installed on the build machine.
- Create a `MAVEN_HOME` directory pointing to the location you've unzipped it to.
- `cd` to the top level repo directory and type `mvn package`.
- The release artifacts will be contained in the `target` directory.

### Creating A Docker Image

- This procedure is currently only available on the Linux and OSX platform.
- You must have Docker installed on the same host as the downloaded utility.
- From the directory created by unarchiving the utility execute `docker-build.sh` This will create the Docker image - see run instructions for more information on running the utility from a container.

## Running The Diagnostic Utility

### Interactive Mode - For Those Who Don't Like To Read Documentation

If you are in a rush and don't mind going through a Q&A process you can execute the diagnostic with no options. It will then enter interactive mode and walk you through the process of executing with the proper options. Simply execute `./diagnostics.sh` or `diagnostics.bat`. Previous versions of the diagnostic required you to be in the installation directory but you should now be able to run it from anywhere on the installed host. Assuming of course that the proper permissions exist. Symlinks are **not** currently supported however, so keep that in mind when setting up your installation.

### Running From The Command Line

- Input parameters may be specified in any order.
- As previously stated, to ensure that all artifacts are collected it is recommended that you run the tool with elevated privileges. This means sudo on Linux type platforms and via an Administor Prompt in Windows. This is not set in stone, and is entirely dependent upon the privileges of the account running the diagnostic. Logs can be especially problematic to collect on Linux systems where Elasticsearch was installed via a package manager. When determining how to run, it is suggested you try copying one or more log files from the configured log directory to the user home of the running account. If that works you probably have sufficient authority to run without sudo or the administrative role.
- An archive with the format `<diagnostic type>-diagnostics`-`<DateTimeStamp>`.zip will be created in the working directory or an output directory you have specified.
- A truststore does not need to be specified - it's assumed you are running this against a node that you set up and if you didn't trust it you wouldn't be running this.
- You can specify additional Java options such as a higher `-Xmx` value by setting them via the environment variable: `DIAG_JAVA_OPTS`.

#### Diagnostic Types

Elasticseach, Kibana, and Logstash each have three distinct execution modes available when running the diagnostic.

 <table>
 <thead>
   <tr>
     <th>Type</th>
     <th>Description</th>
   </tr>
 </thead>

 <tr>
   <td width="30%" align="left" valign="top">local</td>
   <td width="70%" align="left" valign="top">
   Used when the node you are targeting with the host parameter is on the same host as the diagnostic is installed on. Collects REST API calls from the Elasticsearch cluster, runs system calls such as top, iostat, and netstat, as well as a thread dump. Collects current and the most recent archived Elasticsearch and gc logs.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">remote</td>
   <td width="70%" align="left" valign="top">
   Use this type when the diagnostic utility is installed on a server host or workstation that does not have one of the nodes in the target installed. You will need to provide credentials to establish an ssh session to the host containing the targeted Elasticsearch node, but it will collect the same artifacts as the local type.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">api</td>
   <td width="70%" align="left" valign="top">
   This type collects only the REST API calls for the targeted cluster without retriving system information and logs from the targeted host. This option will run a bit more quickly than the previous two, and the only privileges required are an Elasticsearch login of sufficient authority to execute the calls. The simplest option to run from a workstation.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">logstash-local</td>
   <td width="70%" align="left" valign="top">Similar to Elasticsearch local mode, this runs against a logstash process running on the same host as the installed diagnostic utility. Retrieves Logstash REST API dignostic information as well as the output from the same system calls as the Elasticsearch type.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">logstash-remote</td>
   <td width="70%" align="left" valign="top">Queries a logstash processes running on a different host than the utility. Similar to the Elasticsearch remote option. Collects the same artifacts as the logstash-local option.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">logstash-api</td>
   <td width="70%" align="left" valign="top">Collects the REST API information only from a running Logstash process. Similar to the Elasticsearch type.
   </td>
 </tr>
 <tr>
   <td width="30%" align="left" valign="top">kibana-local</td>
   <td width="70%" align="left" valign="top">Similar to Elasticsearch local mode, this runs against a Kibana process running on the same host as the installed diagnostic utility. Retrieves Kibana REST API dignostic information as well as the output from the same system calls and the logs if stored in the default path `var/log/kibana` or in the `journalctl` for linux and mac.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">kibana-remote</td>
   <td width="70%" align="left" valign="top">Queries a Kibana processes running on a different host than the utility. Similar to the Elasticsearch remote option. Collects the same artifacts as the kibana-local option.
   </td>
 </tr>

 <tr>
   <td width="30%" align="left" valign="top">kibana-api</td>
   <td width="70%" align="left" valign="top">Collects the REST API information only from a running Kibana process. Similar to the Elasticsearch type (This is the method that need to be used when collecting the data for Kibana in **Elastic cloud**).
   </td>
 </tr>
 </table>

#### Standard Options

 <table>
 <thead>
   <tr>
     <th>Option</th>
     <th>Description</th>
     <th>Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">-?<br>--help</td>
   <td width="50%" align="left" valign="top">Display help for the command line options.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-h<br>--host</td>
   <td width="50%" align="left" valign="top">The hostname or IP address of the target node. Defaults to localhost. IP address will generally produce the most consistent results.<br>
   This should  <strong>NOT</strong> be in the form of a URL containing http:// or https://. </td>
   <td width="30%" align="left" valign="top"> --host myhost.somplace.com<br>-h 10.75.0.50 </td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--port</td>
   <td width="50%" align="left" valign="top">The HTTP listening port for the target node if set to a different value than the default 9200. Not required if the node is listening on the default.
    The target node <strong>MUST</strong> have an HTTP listener in order to run the diagnostic. When running against a Logstash process the default value will be 9600.</td>
   <td width="30%" align="left" valign="top" >--port 9205</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top" >--type  </td>
   <td width="50%" align="left" valign="top"> The diagnostic mode to execute. Valid types are local, remote, api, logstash-remote, logstash-local, or logstash-api, kibana-remote, kibana-local, or kibana-api. See the documentation for additional descriptions of the diagnostic modes. Default value is local.</td>
   <td width="30%" align="left" valign="top">--type local<br/> --type remote<br/> --type api<br/> --type logstash-local<br/> --type logstash-remote<br/>--type logstash-api<br/>--type kibana-remote<br/>--type kibana-local<br/>--type kibana-api</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-s<br/>--ssl</td>
   <td width="50%" align="left" valign="top">Cluster is configured for TLS (SSL). Use this if you access your cluster with an https:// url from the browser or curl. Default is false</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-u<br/>--user</td>
   <td width="50%" align="left" valign="top">The login id for the Elasticsearch cluster when set up for user/password authentication.This account should have sufficient authority to read system indices so an account with a superuser role is recommended. exampleswise output may be incomplete depending on the authorization level configured. </td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-p<br/>--password</td>
   <td width="50%" align="left" valign="top">Generates obfuscated prompt for the elasticsearch password. Passing of a plain text password for automated processes is possible but not encouraged given it cannot be concealed from the history. See documentation for details. All other password prompts function in a similar fashion. </td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--mode</td>
   <td width="50%" align="left" valign="top">Designates whether to collect a minimal or full set of data. Enter light or full. Defaults to full.</td>
   <td width="30%" align="left" valign="top">--mode light</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--noVerify</td>
   <td width="50%" align="left" valign="top">Bypass hostname verification for the certificate when using the --ssl option.  This can be unsafe in some cases, but can be used to bypass issues with an incorrect or missing hostname in the certificate. Default value is false.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-o<br/>--output</td>
   <td width="50%" align="left" valign="top">Absolute path to the output directory, or if running in a container the configured volume. Temp files and the final archive will be
   written to this location. Quotes must be used for paths with spaces. If not supplied, the working directory will be used unless it is running in a container, in which case
   the configured volume name will be used.</td>
   <td width="30%" align="left" valign="top">-o "/User/someuser/diagnostics" <br/> -o "C:\temp\My Diagnostics"</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--bypassDiagVerify</td>
   <td width="50%" align="left" valign="top">Turn off the internal check where the diagnostic queries Github to see if there is a newer version available. Useful in air gapped environments with no internet access. Default value is false</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 </table>

#### PKI Authentication Options

If you use a PKI store to authenticate to your Elasticsearch cluster you may use these options in lieu of login/password Basic authentication.

 <table>
 <thead>
   <tr >
     <th width="20%" align="left" valign="top">Option</th>
     <th width="50%" align="left" valign="top">Description</th>
     <th width="30%" align="left" valign="top">Examples</th>
   </tr>
 </thead>
 <tr>
   <td width="20%" align="left" valign="top">--pkiKeystore</td>
   <td width="50%" align="left" valign="top">When using PKI Authentication the store containing the certificates. Quotes must be used for paths with spaces. </td>
   <td width="30%" align="left" valign="top">--pkiKeystore ~/auth.jks</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--pkiPass</td>
   <td width="50%" align="left" valign="top">Prompt for a password if the PKI keystore is secured.  Note that this password will be used for both the secured keystore and the secured key. </td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 </table>

#### HTTP Proxy Options

When running the diagnostic from a workstation you may encounter issues with HTTP proxies used to shield internal machines from the internet. In most cases you will probably not require more than a hostname/IP and a port.

 <table>
 <thead>
   <tr >
     <th width="20%" align="left" valign="top">Option</th>
     <th width="50%" align="left" valign="top">Description</th>
     <th width="30%" align="left" valign="top">Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">--proxyHost</td>
   <td width="50%" align="left" valign="top">The hostname or IP address of the host in the proxy url. <br>
   This should  <strong>NOT</strong> be in the form of a URL containing http:// or https://. </td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--proxyPort</td>
   <td width="50%" align="left" valign="top">Port used by the http proxy.</td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--proxyUser</td>
   <td width="50%" align="left" valign="top">User account if http proxy requires authentication.</td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--proxyPass</td>
   <td width="50%" align="left" valign="top">Prompt for password if required by the http proxy.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>
 </table>

#### Remote Execution Options

The remote type works exactly like its local counterpart for REST API calls. When collecting system calls and logs however, it will use the credentials input for the remote host to establish an ssh session and run the same calls via the ssh shell. Because there's no elevated option when using SFTP to bring over the logs it will attempt to copy the Elasticsearch logs from the configured Elasticsearch log directory to a temp directory in the home of the user account running the diagnostic. When it is done copying it will bring the logs over and then delete the temp directory.

Because there is no native equivalent of ssh or sftp on Windows, this functionality is not supported for clusters installed on Windows hosts. If you have an installation where there is a third party ssh/sftp server running on Windows and are open to sharing details of your installation feel free to open a ticket for future support.

 <table>
 <thead>
   <tr >
     <th>Option</th>
     <th>Description</th>
     <th>Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">--remoteUser</td>
   <td width="50%" align="left" valign="top">User account to be used for running system commands and obtaining logs. This account should have sufficient authority to run the system commands and access the logs. It will still be necessary when using key file authentication.</td>
   <td width="30%" align="left" valign="top">--remoteUser imstressed</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--remotePass</td>
   <td width="50%" align="left" valign="top">Prompts for the remote user's password for the remote host being accessed.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--keyFile</td>
   <td width="50%" align="left" valign="top">An ssh public key file to be used as for authenticating to the remote host.  Quotes must be used for paths with spaces.</td>
   <td width="30%" align="left" valign="top">--keyFile "~./ssh/rsa_id"</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--keyFilePass</td>
   <td width="50%" align="left" valign="top">Prompt for a pass phrase if the public key file is secured.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--trustRemote</td>
   <td width="50%" align="left" valign="top">Forces the diagnostic to trust the remote host if no entry in a known hosts file exists. Default is false. Use with hosts you can ascertain are yours.</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--knownHostsFile</td>
   <td width="50%" align="left" valign="top">Location of a known hosts file if you wish to verify the host you are executing the remote session against.  Quotes must be used for paths with spaces.</td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--sudo</td>
   <td width="50%" align="left" valign="top">Attempt to run the commands in the remote host via sudo. Only necessary if the account being used for remote access does not have sufficient authority to view the Elasticsearch log files(usually under /var/log/elasticseach). Defaults to false. If no remote password exists and public key was used it will attempt to use the command with no password. </td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--remotePort</td>
   <td width="50%" align="left" valign="top">Use when the ssh port of the remote host is set to something other than 22. Usually not necessary.</td>
   <td width="30%" align="left" valign="top"></td>
 </tr>

 </table>

#### Usage Examples

_NOTE: Windows users use .\diagnostics.bat instead of ./diagnostics.sh_

Local or remote host, default port, no security or TLS

```$xslt
sudo ./diagnostics.sh --host localhost
sudo ./diagnostics.sh --host 10.0.0.20
```

Basic Auth with and without TLS

```$xslt
sudo ./diagnostics.sh --host myhost.mycompany.com -u someuser -p
sudo ./diagnostics.sh --host 10.0.0.20 -u someuser --password --ssl
```

Running the api type to suppress system call and log collection and explicitly configuring an output directory.

```$xslt
sudo ./diagnostics.sh --host localhost --type api -o /home/user1/diag-out
```

Executing Logstash diagnostics with a non-default port

```$xslt
sudo ./diagnostics.sh --host 10.0.0.20 --type logstash-local --port 9607
```

Executing Kibana diagnostics locally from the same server where Kibana is running

```$xslt
sudo ./diagnostics.sh --host localhost --port 5601 --type kibana-local
```

Running the `kibana-api` type to suppress system call and log collection and explicitly configuring an output directory (this is also the option that needs to be used when collecting the diagnostic for Kibana in **Elastic Cloud**).

```$xslt
sudo ./diagnostics.sh --host 2775abprd8230d55d11e5edc86752260dd.us-east-1.aws.found.io --port 9243 --type kibana-api -u elastic --password --ssl -o /home/user1/diag-out
```

Executing against a remote host with full collection, using sudo, and enabling trust where there's no known host entry. Note that the diagnostic is not executed via sudo because all the privileged access is on a different host.

```$xslt
./diagnostics.sh --host 10.0.0.20 --type remote -u someuser --password --ssl --remoteUser someuser --remotePass --trustRemote --sudo`
```

Executing against a remote host, full collection, using an ssh public key file and bypassing the diagnostics version check.

```$xslt
./diagnostics.sh --host 10.0.0.20 --type remote -u someuser --password --ssl --remoteUser someuser --keyFile "~.ssh/es_rsa" --bypassDiagVerify
```

Executing against a Cloud, ECE, or ECK cluster. Note that in this case we use 9243 for the port, disable host name verification and force the type to strictly api calls.

```$xslt
./diagnostics.sh --host 2775abprd8230d55d11e5edc86752260dd.us-east-1.aws.found.io -u elastic -p --port 9243 --ssl --type api --noVerify
```

#### Customizing What Is Collected

##### The Config Directory

All configuration used by the utility is located on the `/config` under the folder created when the diagnostic utility was unzipped. These can be modified to change some behaviors in the diagnostic utility.

The `*-rest.yml` files all contain queries that are executed against the cluster being diagnosed. They are versioned and the Elasticsearch calls have additional modifiers that can be used to further customize the retrievals. The `diags.yml` file has generalized configuration information and `scrub.yml` can be used to drive the sanitization (scrub) function.

##### Removing Or Modifying Calls

To prevent a call from being executed or modify the results via the syntax, simple comment out, remove or change the entry. You can also add a completely different entry. Make sure that the key
you use for that call does not overlap with another one already used. The file name of the output that will be packaged in the diag will be derived from that key.

##### Preventing Retries

At times you may want to compress the time frames for a diagnostic run and do not want multiple retry attempts if the first one fails. These will only be executed if a REST call within the
configuration file has a _retry: true_ parameter in its configuration. If this setting exists simply comment it out or set it to false to disable the retry.

#### Executing Scripted Runs

Executing the diagnostic via a script passing in all parameters at a time but passwords must currently be sent in via plain text so it is not recommended unless you have the proper security mechanisms in place to safeguard your credentials. The parameters:<br/> --passwordText, --pkiPassText, --proxyPassText, --pkiPassText, --remotePassText, and --keyFilePassText can be used instead of their switch parameter equivalents to send in a value rather than prompt for a masked password. These are not displayed via the help or on the command line options table because we do not encourage their use unless you absolutely need to have this functionality.

## Docker

### Elasticsearch Deployed In Docker Containers

During execution, the diagnostic will attempt to determine whether any of the nodes in the cluster are running within Docker containers, particularly the node targeted via the host name. If one or more nodes on that targeted host are running in Docker containers, an additional set of Docker specific diagnostics such as inspect, top, and info, as well as obtaining the logs. This will be done for every discovered container on the host(not just ones containing Elasticsearch). In addition, when it is possible to determine if the calls are valid, the utility will also attempt to make the usual system calls to the host OS running the containers.

If errors occur when attempting to obtain diagnostics from Elasticsearch nodes, Kibana, or Logstash processes running within Docker containers, consider running with the `--type` set to `api`, `logstash-api`, or `kibana-api` to verify that the configuration is not causing issues with the system call or log extraction modules in the diagnostic. This should allow the REST API subset to be successfully collected.

### Running From A Docker Container

When the diagnostic is deployed within a Docker container it will recognize the enclosing environment and disable the types `local`, `local-kibana`, and `local-logstash`. These modes of operation require the diagnostic to verify that it is running on the same host as the process it is investigating because of the ways in which system calls and file operations are handled. Docker containers muddy the waters, so to speak, in this case making this difficult if not impossible. So for the sake of reliability, once the diagnostic is deployed within Docker it will always function as if it were a remote component. The only options available will be `kibana-remote`, `logstash-remote`, `remote`, and `api`.

There are a number of options for interacting with applications running within Docker containers. The easiest way to run the diagnostic is simply to perform a `docker run -it` which opens a pseudo TTY. At that point you can interface with the diagnostic in the same way as you would when it was directly installed on the host. If you look in the _/docker_ directory in the diagnostic distribution you will find a sample script named `diagnostic-container-exec.sh` that contains an example of how to do this.

```$xslt
docker run -it -v ~/docker-diagnostic-output:/diagnostic-output support-diagnostics-app  bash
```

For the diagnostic to work seamlessly from within a container, there must be a consistent location where files can be written. The default location when the diagnostic detects that it is deployed in Docker will be a volume named _diagnostic-output_. If you examine the above script you will notice that it mounts that volume to a local directory on the host where the diagnostic loaded Docker container resides. In this case it is a folder named _docker-diagnostic-output_ in the home directory of the user account running the script. Temp files and the eventual diagnostic archive will be written to this location. You may change the volume if you adjust the explicit output directory whenever you run the diagnostic, but given that you are mapping the volume to local storage that creates a possible failure point. Therefore it's recommended you leave the _diagnostic-output_ volume name _as is_ and simply adjust the local mapping.

## File Sanitization Utility

### Overview - How Sanitization Works

In some cases the information collected by the diagnostic may have content that cannot be viewed by those outside the organization. IP addresses and host names, for instance. The diagnostic contains functionality that allows one to replace this content with values they choose contained in a configuration file. It will process a diagnostic archive file by file, replacing the entries in the config with a configured substitute value.

It is run via a separate execution script, and can process any valid Elasticsearch cluster diagnostic archive produced by Support Diagnostics 6.4 or greater. It can also process a single file. It does not need to be run on the same host that produced the diagnostic. Or by the same version number that produced the archive as long as it is a supported version. Kibana and Logstash diagnostics are not supported at this time, although you may process those using the single file by file functionality for each entry.

It will go through each file line by line checking the content. If you are only concerned about IP addresses, you do not have to configure anything. **_The utility will automatically obfuscate all node id's node names, IPv4, IPv6 and MAC addresses._** It is important to note this because as it does this, it will generate a new random IP value and cache it to use every time it encounters that same IP later on. So that the same obfuscated value will be consistent across diagnostic files. This ensures that you can differentiate between occurrences of discrete nodes in the cluster. If you replace all the IP addresses with a global `XXX.XXX.XXX.XXX` mask you will lose the ability to see which node did what.

After it has checked for IP and MAC addresses it will use any configured tokens. If you include a configuration file of supplied string tokens, any occurrence of that token will be replaced with a generated replacement. As with IP's this will be consistent from file to file but not between runs. It supports explicit string literal replacement or regexes that match a broader set of criteria. An example configuration file (`scrub.yml`) is included in the root installation directory as an example for creating your own tokens.

### Running The Sanitizer

- Start with a generated diagnostic archive from Support Diagnostics 6.4 or later and an installation of the latest diagnostic utility.
- Add any tokens for text you wish to conceal to your config file. The utility will look for a file named `scrub.yml` located in the `/config` directory within the unzipped utility directory. It **must** reside in this location.
- Run the `scrub` utility (`scrub.sh` or `scrub.bat`), providing the full absolute path for the archive, directory, or single file you wish to process. Options are described below.
- The sanitization process will check for the number of processors on the host it is run on and create a worker per processor to distribute the load. If you wish to override this it can be done via the command line `--workers` option.
- If you are processing a large cluster's diagnostic, this may take a while to run, and you may need to use the `DIAG_JAVA_OPTS` environment variable to increase the size of the Java heap if processing is extremely slow or you see OutOfMemoryExceptions.
- You can bypass specified files from processing, remove specified files from the sanitized archive altogether, and include or exclude certain file types from sanitization on a token by token basis. See the `scrub` file for examples.
- When running against a standard diagnostic package, it will re-archive the file with `scrubbed-` prepended to the name. Single files and directories will be enclosed within a new archive .

#### Sanitization Options

 <table>
 <thead>
   <tr >
     <th width="20%" align="left" valign="top">Option</th>
     <th width="50%" align="left" valign="top">Description</th>
     <th width="30%" align="left" valign="top">Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">-i<br/>--input</td>
   <td width="50%" align="left" valign="top">An absolute path to the diagnostic archive, directory, or individual file you wish to sanitize. All contents of the archive or directory are examined by default. Use quotes if there are spaces in the directory name.</td>
   <td width="30%" align="left" valign="top">--input /home/admin/diags/diagnostics-20191014-172051/logs/elasticsearch.log <br> -i /home/admin/local-diagnostics-2020-06-06.zip< <br> --input "/home/admin/collected diags/local-diagnostics-2020-06-06"/td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">-o<br/>--output</td>
   <td width="50%" align="left" valign="top">Absolute path to a target directory where you want the revised archive written. If not supplied it will be written to the working directory. Use quotes if there are spaces in the directory name.</td>
   <td width="30%" align="left" valign="top">--output /home/cwalker/diagnostics </td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--workers</td>
   <td width="50%" align="left" valign="top">The utility will check the host it is being run on for number of processors and create an equal number of workers to parallelize the processing. This parameter allows you to increase or reduce this number.</td>
   <td width="30%" align="left" valign="top">-- workers 20</td>
 </tr>
 </table>

See `./scrub.sh --help` for other options.

#### Sanitization Examples

Writing output from a diagnostic zip file to the working directory with the workers determined dynamically:

```$xslt
./scrub.sh -i /home/adminuser/diagoutput/diagnostics-20180621-161231.zip
```

Writing output from a diagnostic zip file in a directory with spaces to a specific directory with the workers determined dynamically:

```$xslt
./scrub.sh -i '/Users/adminuser/diagnostic output/diagnostics-20180621-161231.zip' -o /home/adminuser/sanitized-diags -c /home/adminuser/sanitized-diags/scrub.yml
```

Processing a single log file and using a single worker:

```$xslt
./scrub.sh -i /home/adminuser/elasticsearch.log -o /home/adminuser/sanitized-diags --workers 1
```

Processing a directory and using specific number of workers:

```$xslt
./scrub.sh -i /home/adminuser/log-files -o /home/adminuser/sanitized-diags --workers 6
```

## Extracting Time Series Diagnostics From Monitoring

### Monitoring Extract Overview

While the standard diagnostic is often useful in providing the background necessary to solve an issue, it is also limited in that it shows a strictly one dimensional view of the cluster's state. The view is restricted to whatever was available at the time the diagnostic was run. So a diagnostic run subsequent to an issue will not always provide a clear indication of what caused it.

Time series data will be availalble if Elasticsearch Monitoring is enabled, but in order to view it anywhere other than locally you would need to snapshot the relevant monitoring indices or have the person wishing to view it do so via a screen sharing session. Both of these have issues of scale and utility if there is an urgent issue or multiple individuals need to be involved.

This utility allows you to extract a subset of monitoring data for interval of up to 12 hours at a time. It will package this into a zip file, much like the current diagnostic. After it is uploaded, a support engineer can import that data into their own monitoring cluster so it can be investigated outside of a screen share, and be easily viewed by other engineers and developers. It has the advantage of providing a view of the cluster state prior to when an issue occurred so that a better idea of what led up to the issue can be gained.

Not all the information contained in the standard diagnostic is going to be available in the monitoring extraction. That is because it does not collect the same quantity of data. But what it does have should be sufficient to see a number of important trends, particularly when investigating peformance related issues.

It does not need to be run on a host with Elasticsearch installed. A local workstation with network access to the monitoring cluster is sufficient. It can either be installed directory or run from a Docker container.

You can collect statistics for only one cluster at a time, and it is necessary to specify a cluster id when running the utility. If you are not sure of the cluster id, running with only the target host, login credentials, and `--list` parameter will display a listing of availble clusters that are being monitored in that instance.

### Running The Extract

To extract monitoring data you need to connect to a monitoring cluster in exactly the same way you do with a normal cluster. Therefore all the same standard and extended authentication parameters from running a standard diagnostic also apply here with some additional parameters required to determine what data to extract and how much. A cluster_id is required. If you don't know the one for the cluster you wish to extract data from run the extract scrtipt with the `--list` parameter and it will display a list of clusters available. The range of data is determined via the cutoffDate, cutoffTime and interval parameters. The cutoff date and time will designate the end of a time segment you wish to view the monitoring data for. The utility will take that cuttof date and time, subtract supplied interval hours, and then use that generated start date/time and the input end date/time to determine the start and stop points of the monitoring extract.

As with a standard diagnostics the superuser role for Elasticsearch authentication is recommended. Sudo execution of the utility should not be necessary.

The monitoring indices types being collected are as follows: cluster_stats, node_stats, indices_stats, index_stats, shards, job_stats, ccr_stats, and ccr_auto_follow_stats. If Logstash monitoring
information exists for the specified cluster it will also be collected.

Metricbeat system information can also be collected by specifying the input type as _metric_ or collecting monitoring data as well with _all_.

### Monitoring Extract Options

 <table>
 <thead>
   <tr>
     <th>Option</th>
     <th>Description</th>
     <th>Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">--id</td>
   <td width="50%" align="left" valign="top">The cluster_id of the cluster you wish to retrieve data for. Because multiple clusters may be monitored this is necessary to retrieve the correct subset of data. If you are not sure, see the --list option example below to see which clusters are available.</td>
   <td width="30%" align="left" valign="top">--id gELr3Yv1RvuW4v4fZq73Dg</td>
 </tr>

  <tr>
    <td width="20%" align="left" valign="top">--type</td>
    <td width="50%" align="left" valign="top">What kind of information to collect. Valid options are monitoring, metric, or all. Default is monitoring only.</td>
    <td width="30%" align="left" valign="top">--type monitoring</td>
  </tr>

 <tr>
   <td width="20%" align="left" valign="top">--interval</td>
   <td width="50%" align="left" valign="top">The number of hours of statistics you wish to collect. Default value of 6. Whole integer values only. Minimum value of 1, maximum value of 12.</td>
   <td width="30%" align="left" valign="top"> --interval 10 </td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--cutoffDate</td>
   <td width="50%" align="left" valign="top">The date for the stop point of the collected statistics. The default will be the current date. Must be in the yyyy-MM-DD format.</td>
   <td width="30%" align="left" valign="top" >--cutoffDate 2020-02-25</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top" >--cutoffTime  </td>
   <td width="50%" align="left" valign="top">The stop point for the collected statistics. The start will be calculated by subtracting 6 hours from this time. It should be in UTC, and in the 24 hour format HH:mm.</td>
   <td width="30%" align="left" valign="top">--cutoffTime 08:30</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--list</td>
   <td width="50%" align="left" valign="top">Display a list of monitored clusters, id's and if a metadata.display_name has been set(often used for cloud clusters with randomly generated id's).</td>
   <td width="30%" align="left" valign="top">Option only - no value.</td>
 </tr>

 </table>

See `./export-monitoring.sh --help` for other options.

### Monitoring Extract Examples

Simple case using defaults - data from the last 6 hours will be collected:

```$xslt
    ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843
```

Specifies a specific date, time and uses default interval 6 hours:

```$xslt
    ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --cutoffDate 2019-08-25 --cutoffTime 08:30
```

Specifies the last 8 hours of data.

```$xslt
    ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --interval 8
```

Specifies a specific date, time and interval and gets metricbeat as well:

```$xslt
    ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --id 37G473XV7843 --cutoffDate 2019-08-25 --cutoffTime 08:30 --interval 10 --type all
```

Lists the clusters available in this monitoring cluster

```$xslt
    ./export-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl --list
```

Get data from a monitoring cluster in the elastic cloud, with the port that is different from default and the last 8 hours of data:

```$xslt
    ./export-monitoring.sh --host 2775abprd8230d55d11e5edc86752260dd.us-east-1.aws.found.io  -u elastic -p --port 9243 --ssl --id 37G473XV7843 --interval 8
```

This will provide a listing in the following format:

 <table>
 <thead>
   <tr >
     <th width="33%" align="left" valign="top">cluster name</th>
     <th width="33%" align="left" valign="top">cluster id</th>
     <th width="34%" align="left" valign="top">display name</th>
   </tr>
 </thead>

 <tr>
   <td width="33%" align="left" valign="top">daily_ingest</td>
   <td width="33%" align="left" valign="top"> gELr3Yv1RvuW4v4fZq73Dg</td>
   <td width="34%" align="left" valign="top">Daily Ingest Cluster</td>
 </tr>

 </table>

## Importing Extracted Monitoring Data

### Monitoring Import Overview

Once you have an archive of exported monitoring data, you can import this into an version 7 or greater Elasticsearch cluster that has monitoring enabled. Earlier versions are not supported.

An installed instance of the diagnostic utility or a Docker container containing the it is required. This does not need to be on the same host as the ES monitoring instance, but it does need to be on the same host as the archive you wish to import since it will need to read the archive file.

Only a monitoring export archive produced by the diagnostic utility is supported. It will not work with a standard diagnostic bundle or a custom archive.

A specialized template will be used to make sure the indexed data is usuable by Kibana. If you've adjusted the monitoring index patterns to something other than .monitoring-es-7, .monitoring-logstash-7, or metricbeat- you will need to adjust the index template name in the
diags.yml file, as well as the indexing templates contained in the monitoring-extract/templates directory.

### Running The Monitoring Data Import

Similar to the extract, you must provide a target host and authentication parameters for the Elasticsearch cluster that will receive the monitoring data. The only required additional parameter is the path to the archive you wish to import. If you have multiple clusters you may designate a unique index name. For instance, if you wish to see two separate weeks. of extracts separately you could give each a unique cluster name. Such as logging-cluster-05-01 and logging cluster 05-08. You can also override the actual monitoring index name used if that assists in managing separate imports. Whatever value you use will be appended to `.monitoring-es-7-`. If you do not specify this parameter, the imported data will be indexed into the standard monitoring index name format with the current date appended. No spaces in the cluster or index names are allowed.

Once the data is imported you should be able to view the new data via monitoring interface right away. **IMPORTANT:** Make sure to set the date range in the upper right hand corner of Kibanba to reflect the period that was collected so that it displays and is in a usable format. You should generally be using the absolute time selector and select a range that starts prior to the beginning of your extract period and ends subsequent to it. You may also need to make adjustments depending on whether you are working with local time or UTC. If you don't see your cluster or data is missing/truncated, try expanding the range.

### Monitoring Data Import Options

 <table>
 <thead>
   <tr >
     <th width="20%" align="left" valign="top">Option</th>
     <th width="50%" align="left" valign="top">Description</th>
     <th width="30%" align="left" valign="top">Examples</th>
   </tr>
 </thead>

 <tr>
   <td width="20%" align="left" valign="top">-i <br></br>--input</td>
   <td width="50%" align="left" valign="top">The absolute path the to archive containing extracted monitoring data. Paths with spaces should be contained in quotes.</td>
   <td width="30%" align="left" valign="top">--input /data/monitoring-export-20200106-161558.zip</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--clustername</td>
   <td width="50%" align="left" valign="top">An alternative cluster name to be used when displaying the cluster data in monitoring. Default is the existing clusterName. No spaces allowed.</td>
   <td width="30%" align="left" valign="top">--clustername testCluster</td>
 </tr>

 <tr>
   <td width="20%" align="left" valign="top">--targetsuffix</td>
   <td width="50%" align="left" valign="top">An alternative suffix to be used when ingesting documents to target such as `.monitoring-es-{{version}}-diag-import-{{suffix}}`. Default is `yyyy-MM-dd`. Must be lowercase.</td>
   <td width="30%" align="left" valign="top">--targetsuffix test-cluster-20200106</td>
 </tr>

 </table>

See `./import-monitoring.sh --help` for other options.

### Monitoring Import Examples

Uses the default cluster_id, index_name:

```$xslt
    ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip
```

Uses the generated index name but gives the cluster a different name:

```$xslt
    ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip --clustername messed_up_cluster
```

Uses a custom index and cluster name:

```$xslt
   ./import-monitoring.sh --host 10.0.0.20 -u elastic -p --ssl -i /Users/joe_user/temp/export-20190801-150615.zip  --clustername big_cluster --targetsuffix big_cluster_2019_10_01
```

## Standard Diagnostic Troubleshooting

- If you get a message telling you that the Elasticsearch version could not be obtained it indicates that an initial connection to the node could not be obtained. This always indicates an issue with the connection parameters you have provided. Please verify, host, port, credentials, etc.
- If you receive 400 errors from the allocation explain API's it just means there weren't any usassigned shards to analyze.
- The file: diagnostic.log file will be generated and included in the archive. In all but the worst case an archive will be created. Some messages will be written to the console output but granualar errors and stack traces will only be written to this log.
- If you get a message saying that it can't find a class file, you probably downloaded the src zip instead of the one with "-dist" in the name. Download that and try it again.
- If you get a message saying that it can't locate the diagnostic node, it usually means you are running the diagnostic on a host containing a different node than the one you are pointing to. Try running in remote node or changing the host you are executing on.
- Make sure the account you are running from has read access to all the Elasticsearch log directories. This account must have write access to any directory you are using for output.
- Make sure you have a valid Java installation that the JAVA_HOME environment variable is pointing to.
- IBM JDK's have proven to be problematic when using SSL. If you see an error with _com.ibm.jsse2_ in the stack trace please obtain a recent Oracle or OpenJDK release and try again.
- If you are not in the installation directory CD in and run it from there.
- If you encounter OutOfMemoryExceptions, use the `DIAG_JAVA_OPTS` environment variable to set an `-Xmx` value greater than the standard `2g`. Start with `-Xmx4g` and move up from there if necessary.
- If reporting an issue make sure to include that.
- And if the message tells you that you are running an outdated diagnostic, do not ignore it. Upgrade and see if the issue persists.
