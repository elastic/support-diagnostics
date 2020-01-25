package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.rest.ElasticRestClientInputs;
import com.elastic.support.util.SystemCommand;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beryx.textio.*;

import java.util.*;

public class DiagnosticInputs extends ElasticRestClientInputs {

    public final static String[]
            diagnosticTypeValues = {
            Constants.local,
            Constants.remote,
            Constants.api,
            Constants.logstashLocal,
            Constants.logstashRemote,
            Constants.logstashApi};

    public final static String[]
            diagnosticTypeDescriptions = {
            "Local node running on the same host as the diagnostic utility.",
            "Remote node running on a different host than the diagnostic utility",
            "Run only the Elasticsearch REST API calls, no system calls or logs.",
            "Local Logstash process on the same host as the diagnostic utility.",
            "Remote Logstash on a different host than the diagnostic utility.",
            "Run only Logstash REST calls. No system calls. \t"};

    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);

    public final static String  typeDescription = "Enter the number of the diagnostic type to run.";
    public final static String  remoteUserDescription = "User account to be used for running system commands and obtaining logs. This account must have sufficient authority to run the commands and access the logs.";
    public final static String  remotePasswordDescription = "Password for the remote login.";
    public final static String  sshKeyFileDescription= "File containing keys for remote host authentication. Default is ~/.ssh/id_rsa for Mac/Linux. Windows users should always set this explicitly.";
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
    @Parameter(names = {"--remotePass"}, description = "Show password prompt for the remote user account.", hidden = true)
    public boolean remotePasswordSwitch = false;
    @Parameter(names = {"--remotePwdText"}, hidden = true)
    public String remotePassword = "";
    @Parameter(names = {"--keyFile"}, description = sshKeyFileDescription)
    public String keyfile = "";
    @Parameter(names = {"--keyPass" }, description = "Show prompt for keyfile passphrase for the keyfile if one exists.", hidden = true)
    public boolean keyFilePasswordSwitch = false;
    @Parameter(names = {"--keyFilePwdText"}, hidden = true)
    public String keyfilePassword = "";
    @Parameter(names = {"--trustRemote"}, description = trustRemoteDescription)
    public boolean trustRemote = false;
    @Parameter(names = {"--knownHostsFile"}, description = knownHostsDescription)
    public String knownHostsFile = "";
    @Parameter(names = {"--sudo"}, description = sudoDescription)
    public boolean sudo = false;
    @Parameter(names = {"--remotePort"}, description = remotePortDescription)
    public int remotePort = 22;
    // Input Fields

    // Input Readers

    protected StringInputReader diagnosticTypeReader = textIO.newStringInputReader()
            .withNumberedPossibleValues(diagnosticTypeValues)
            .withDefaultValue(diagnosticTypeValues[0]);


    protected StringInputReader remoteUserReader = textIO.newStringInputReader()
            .withInputTrimming(true)
            .withValueChecker((String val, String propname) -> validateRemoteUser(val));
    // Input Readers

    public boolean runInteractive() {

        TextTerminal<?> terminal = textIO.getTextTerminal();
        terminal.println("Interactive Mode");
        terminal.println("");

/*        boolean registeredAbort = terminal.registerHandler("ctrl Q",
                t -> new ReadHandlerData(ReadInterruptionStrategy.Action.ABORT)
                        .withPayload("Exiting now."));
        terminal.println("Press ctrl Q to abort entry at any time." + SystemProperties.lineSeparator);*/

        bypassDiagVerify = standardBooleanReader
                .withDefaultValue(bypassDiagVerify)
                .read(bypassDiagVerifyDescription);

        terminal.println("");
        terminal.println("List of valid diagnostic choices:");

        for(int i = 0; i < diagnosticTypeDescriptions.length; i++){
            terminal.println( i+1 + ".  " + diagnosticTypeDescriptions[i] + "\t - " + diagnosticTypeValues[i]);
        }

        diagType = diagnosticTypeReader
                .read(SystemProperties.lineSeparator + typeDescription);
        setDefaultPortForDiagType(diagType);

        runHttpInteractive();

        if(diagType.contains("remote")) {
            remoteUser = remoteUserReader
                    .read(SystemProperties.lineSeparator + remoteUserDescription);

            boolean useKeyfile = standardBooleanReader
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + "Use a keyfile instead of user/password identification?");

            if (useKeyfile) {
                keyfile = standardFileReader
                        .read(SystemProperties.lineSeparator + sshKeyFileDescription);
                keyfilePassword = standardPasswordReader
                        .read(SystemProperties.lineSeparator + sshKeyFIlePassphraseDescription);
            } else {
                remotePassword = standardPasswordReader
                        .read(SystemProperties.lineSeparator + remotePasswordDescription);
            }

            remotePort = standardPortReader
                    .withDefaultValue(remotePort)
                    .read(SystemProperties.lineSeparator + remotePortDescription);

            trustRemote = standardBooleanReader
                    .withDefaultValue(trustRemote)
                    .read(SystemProperties.lineSeparator + trustRemoteDescription);

            if (!trustRemote){
                knownHostsFile = standardFileReader
                        .read(SystemProperties.lineSeparator + knownHostsDescription);
            }

            sudo = standardBooleanReader
                    .withDefaultValue(sudo)
                    .read(SystemProperties.lineSeparator + sudoDescription);
        }

        runOutputDirInteractive();

        textIO.dispose();
        return true;
    }

    public List<String> parseInputs(String[] args){
        List<String> errors = super.parseInputs(args);

        // If we're in interactive mode don't bother validating anything
        if(interactive){
            return emptyList;
        }

        errors.addAll(ObjectUtils.defaultIfNull(validateDiagType(diagType), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(setDefaultPortForDiagType(diagType), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateRemoteUser(remoteUser), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validatePort(remotePort), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(keyfile), emptyList));
        errors.addAll(ObjectUtils.defaultIfNull(validateFile(knownHostsFile), emptyList));

        if(StringUtils.isEmpty(keyfile) && StringUtils.isEmpty(remotePassword)){
            remotePassword = standardPasswordReader
                    .read(remotePasswordDescription);
        }

        if(StringUtils.isNotEmpty(keyfile) && StringUtils.isEmpty(keyfilePassword)){
            keyfilePassword = standardPasswordReader
                    .read(sshKeyFIlePassphraseDescription);
        }

        return errors;

    }

    public List<String> setDefaultPortForDiagType(String val) {
        // Check the diag type and reset the default port value if
        // it is a Logstash diag.
        if (val.toLowerCase().contains("logstash")) {
            if (port == 9200) {
                port = Constants.LOGSTASH_PORT;
            }
        }
        return null;
    }

    public List<String> validateDiagType(String val) {
        List<String> types = Arrays.asList(diagnosticTypeValues);
        if (!types.contains(val)) {
            return Collections.singletonList(val + " was not a valid diagnostype type. Enter --help to see valid choices");
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

        return superString + "," + "DiagnosticInputs{" +
                ", diagType='" + diagType + '\'' +
                ", remoteUser='" + remoteUser + '\'' +
                ", keyfile='" + keyfile + '\'' +
                ", keyFilePasswordSwitch=" + keyFilePasswordSwitch +
                ", trustRemote=" + trustRemote +
                ", knownHostsFile='" + knownHostsFile + '\'' +
                ", sudo=" + sudo +
                ", remotePort=" + remotePort +
                '}';
    }
}
