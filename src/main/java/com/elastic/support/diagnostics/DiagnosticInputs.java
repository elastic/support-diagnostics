package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.Constants;
import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.*;
import java.util.*;
import java.util.List;

public class DiagnosticInputs extends ElasticRestClientInputs {

    
    public final static String[]
            diagnosticTypeValues = {
            Constants.local,
            Constants.remote,
            Constants.api,
            Constants.logstashLocal,
            Constants.logstashRemote,
            Constants.logstashApi,
            Constants.kibanaApi,
            Constants.kibanaLocal,
            Constants.kibanaRemote};

    public static final String localDesc = "Node on the same host as the diagnostic utility.";
    public static final String remoteDesc = "Node on a different host than the diagnostic utility";
    public static final String apiDesc = "Elasticsearch REST API calls, no system calls or logs.";
    public static final String logstashLocalDesc = "Logstash process on the same host as the diagnostic utility.";
    public static final String logstashRemoteDesc = "Logstash on a different host than the diagnostic utility.";
    public static final String logstashApiDesc = "Logstash REST calls. No system calls. \t\t";
    public static final String kibanaLocalDesc = "Kibana process on the same host as the diagnostic utility.";
    public static final String kibanaRemoteDesc = "Kibana on a different host than the diagnostic utility.";
    public static final String kibanaApiDesc = "Kibana REST calls. No system calls. \t\t";


    public static final String[]
            diagnosticTypeEntries = {
            Constants.local + " - " + localDesc,
            Constants.remote + " - " + remoteDesc,
            Constants.api + " - " + apiDesc,
            Constants.logstashLocal + " - " + logstashLocalDesc,
            Constants.logstashRemote + " - " + logstashRemoteDesc,
            Constants.logstashApi + " - " + logstashApiDesc,
            Constants.kibanaApi + " - " + kibanaApiDesc,
            Constants.kibanaRemote + " - " + kibanaRemoteDesc,
            Constants.kibanaLocal + " - " + kibanaLocalDesc};

    public static final String[]
            diagnosticTypeEntriesDocker = {
            Constants.remote + " - " + remoteDesc,
            Constants.api + " - " + apiDesc,
            Constants.logstashRemote + " - " + logstashRemoteDesc,
            Constants.logstashApi + " - " + logstashApiDesc,
            Constants.kibanaApi + " - " + kibanaApiDesc,
            Constants.kibanaRemote + " - " + kibanaRemoteDesc};


    public static final String remoteAccessMessage =
            SystemProperties.lineSeparator
                    + "You are running the diagnostic against a remote host."
                    + SystemProperties.lineSeparator
                    + "You must authenticate either with a username/password combination"
                    + SystemProperties.lineSeparator
                    + "or a public keyfile. Keep in mind that in order to collect the logs"
                    + SystemProperties.lineSeparator
                    + "from the remote host one of the following MUST be true:"
                    + SystemProperties.lineSeparator
                    + SystemProperties.lineSeparator
                    + "1. The account you are logging in as has read permissions for"
                    + SystemProperties.lineSeparator
                    + "   the log directory(usually /var/log/elasticsearch)."
                    + SystemProperties.lineSeparator
                    + SystemProperties.lineSeparator
                    + "2. You select the sudo option and provide the password for "
                    + SystemProperties.lineSeparator
                    + "   the sudo challenge. Note that public key acesss is"
                    + SystemProperties.lineSeparator
                    + "    probably unneeded if this is the case."
                    + SystemProperties.lineSeparator
                    + SystemProperties.lineSeparator
                    + "3. You specify sudo, use a keyfile access with an empty password, and have"
                    + SystemProperties.lineSeparator
                    + "   sudo configured with NOPASSWD."
                    + SystemProperties.lineSeparator
                    + SystemProperties.lineSeparator
                    + "If you are unsure what situation you fall into, you should consult"
                    + SystemProperties.lineSeparator
                    + "someone familiar with the system or consider running with --type api"
                    + SystemProperties.lineSeparator
                    + "or locally from a host with a running instance."
                    + SystemProperties.lineSeparator;

    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);

    public final static String  typeDescription = "Enter the number of the diagnostic type to run.";
    public final static String  remoteUserDescription = "User account to be used for running system commands and obtaining logs. This account must have sufficient authority to run the commands and access the logs.";
    public final static String  remotePasswordDescription = "Password for the remote login.";
    public final static String  sshKeyFileDescription= "File containing keys for remote host authentication.";
    public final static String  sshKeyFIlePassphraseDescription= "Passphrase for the keyfile if required.";
    public final static String  trustRemoteDescription = "Bypass the known hosts file and trust the specified remote server. Defaults to false.";
    public final static String  knownHostsDescription = "Known hosts file to search for target server. Default is ~/.ssh/known_hosts for Linux/Mac. Windows users should always set this explicitly.";
    public final static String  sudoDescription = "Use sudo for remote commands? If not used, log retrieval and some system calls may fail.";
    public final static String  remotePortDescription = "SSH port for the host being queried.";

    // Input Fields
    @Parameter(names = {"--type"}, description = "Designates the type of service to run. Enter local, remote, api, logstash, or logstash-api. Defaults to local.")
    public String diagType = Constants.local;
    @Parameter(names = {"--remoteUser"}, description = remoteUserDescription)
    public String remoteUser;
    @Parameter(names = {"--remotePass"}, description = "Show password prompt for the remote user account.")
    public boolean isRemotePass = false;
    @Parameter(names = {"--remotePassText"}, hidden = true)
    public String remotePassword = "";
    @Parameter(names = {"--keyFile"}, description = sshKeyFileDescription)
    public String keyfile = "";
    @Parameter(names = {"--keyFilePass" }, description = "Show prompt for keyfile passphrase for the keyfile if one exists.")
    public boolean isKeyFilePass = false;
    @Parameter(names = {"--keyFilePassText"}, hidden = true)
    public String keyfilePassword = "";
    @Parameter(names = {"--trustRemote"}, description = trustRemoteDescription)
    public boolean trustRemote = false;
    @Parameter(names = {"--knownHostsFile"}, description = knownHostsDescription)
    public String knownHostsFile = "";
    @Parameter(names = {"--sudo"}, description = sudoDescription)
    public boolean isSudo = false;
    @Parameter(names = {"--remotePort"}, description = remotePortDescription)
    public int remotePort = 22;
    // End Input Fields

    String[] typeEntries;

    public DiagnosticInputs(){
        if(runningInDocker){
            typeEntries = diagnosticTypeEntriesDocker;
        }
        else{
            typeEntries = diagnosticTypeEntries;
        }
    }

    public boolean runInteractive() {

        bypassDiagVerify = standardBooleanReader
                .withDefaultValue(bypassDiagVerify)
                .read(SystemProperties.lineSeparator + bypassDiagVerifyDescription);

        diagType = ResourceCache.textIO.newStringInputReader()
                .withNumberedPossibleValues(typeEntries)
                .withDefaultValue(typeEntries[0])
                .read(SystemProperties.lineSeparator + typeDescription)
                .toLowerCase();

        diagType = diagType.substring(0, diagType.indexOf(" - "));
        setDefaultPortForDiagType(diagType);

        // We'll do this for any Elastic or Logstash submit
        runHttpInteractive();

        if(diagType.contains("remote")) {
            logger.info(Constants.CONSOLE, remoteAccessMessage);

            String  remoteUserTxt = "User account to be used for running system commands and obtaining logs." +
                    SystemProperties.lineSeparator
                    + "This account must have sufficient authority to run the commands and access the logs.";

            remoteUser = ResourceCache.textIO.newStringInputReader()
                    .withInputTrimming(true)
                    .withValueChecker((String val, String propname) -> validateRemoteUser(val))
                    .read(SystemProperties.lineSeparator + remoteUserTxt);

            isSudo = ResourceCache.textIO.newBooleanInputReader()
                    .withDefaultValue(isSudo)
                    .read(SystemProperties.lineSeparator + sudoDescription);

            boolean useKeyfile = ResourceCache.textIO.newBooleanInputReader()
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + "Use a keyfile for authentication?");

            if (useKeyfile) {
                keyfile = standardFileReader
                        .read(SystemProperties.lineSeparator + sshKeyFileDescription);

                boolean checkMe = standardBooleanReader
                        .read("Is the keyfile password protected?");

                if(checkMe){
                    keyfilePassword = standardPasswordReader
                            .read(SystemProperties.lineSeparator + sshKeyFIlePassphraseDescription);
                }
                if(isSudo){
                    checkMe = standardBooleanReader
                            .read("Password required for sudo challenge?");

                    if(checkMe){
                        remotePassword = standardPasswordReader
                                .read(SystemProperties.lineSeparator + "Enter the password for remote sudo.");
                    }
                }
            } else {
                remotePassword = standardPasswordReader
                        .read(SystemProperties.lineSeparator + remotePasswordDescription);
            }

            remotePort = ResourceCache.textIO.newIntInputReader()
                    .withDefaultValue(remotePort)
                    .withValueChecker((Integer val, String propname) -> validatePort(val))
                    .read(SystemProperties.lineSeparator + remotePortDescription);

            trustRemote = standardBooleanReader
                    .withDefaultValue(trustRemote)
                    .read(SystemProperties.lineSeparator + trustRemoteDescription);

            if (!trustRemote){
                knownHostsFile = standardFileReader
                        .read(SystemProperties.lineSeparator + knownHostsDescription);
            }
        }

        runOutputDirInteractive();

        ResourceCache.textIO.dispose();
        return true;
    }

    public List<String> parseInputs(String[] args){
        List<String> errors = super.parseInputs(args);

        errors.addAll(ObjectUtils.defaultIfNull(validateDiagType(diagType), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(setDefaultPortForDiagType(diagType), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateRemoteUser(remoteUser), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(remotePort), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(keyfile), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(knownHostsFile), emptyList));

        if(isRemotePass){
            remotePassword = standardPasswordReader
                    .read(remotePasswordDescription);
        }

        if(isKeyFilePass){
            keyfilePassword = standardPasswordReader
                    .read(sshKeyFIlePassphraseDescription);
        }

        return errors;

    }

    /**
    * Elasticsearch, Kibana and Logstash have default port, on this function we set the default for Kibana or Logstash in case the port is not defined during the execution
    * port variable, is a public int defined in the parent class to 9200 if not defined during the execution
    *
    * @param  String val , type of diagnostic
    *
    */
    public List<String> setDefaultPortForDiagType(String val) {
        // Check the diag type and reset the default port value if
        // it is a Logstash diag.
        if (val.toLowerCase().contains("logstash")) {
            if (port == 9200) {
                port = Constants.LOGSTASH_PORT;
            }
        } else if (val.toLowerCase().contains("kibana")) {
            if (port == 9200) {
                port = Constants.KIBANA_PORT;
            }
        }
        return null;
    }

    public List<String> validateDiagType(String val) {
        List<String> types = Arrays.asList(diagnosticTypeValues);
        if (!types.contains(val)) {
            return Collections.singletonList(val + " was not a valid diagnostic type. Enter --help to see valid choices");
        }

        if(runningInDocker &&val.contains("local") ){
            return Collections.singletonList(val + " cannot be run from within a Docker container. Please use api or remote options.");
        }


        return null;
    }

    public List<String> validateRemoteUser(String val) {
        // Check the diag type and reset the default port value if
        // it is a Logstash diag.
        if(diagType.contains("remote")){
            if(StringUtils.isEmpty(val)){
                return Collections.singletonList("For remote execution a user account must be specified");
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String superString = super.toString();

        return superString + "," + "DiagnosticInputs: {" +
                ", diagType='" + diagType + '\'' +
                ", remoteUser='" + remoteUser + '\'' +
                ", keyfile='" + keyfile + '\'' +
                ", isKeyFilePass=" +  isKeyFilePass + '\'' +
                ", trustRemote=" + trustRemote + '\'' +
                ", knownHostsFile='" + knownHostsFile + '\'' +
                ", sudo=" + isSudo + '\'' +
                ", remotePort=" + remotePort +
                '}';
    }
}
