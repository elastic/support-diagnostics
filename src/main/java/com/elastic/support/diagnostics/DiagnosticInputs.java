package com.elastic.support.diagnostics;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.rest.ElasticRestClientInputs;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class DiagnosticInputs extends ElasticRestClientInputs {

    public final static String[]
            DiagnosticTypeValues = {
            Constants.local,
            Constants.api,
            Constants.remote,
            Constants.logstashLocal,
            Constants.logstashRemote,
            Constants.logstashApi};
    private static Logger logger = LogManager.getLogger(DiagnosticInputs.class);
    public ArrayList<String> diagnosticTypes = new ArrayList<>(Arrays.asList(DiagnosticTypeValues));

    @Parameter(names = {"--type"}, description = "Designates the type of service to run. Enter local, remote, api, logstash, or logstash-api. Defaults to local.")
    public String diagType = Constants.local;
    @Parameter(names = {"--remoteUser"}, description = "User account to be used for running system commands and obtaining logs. This account must have sufficient authority to run the commands and access the logs.")
    public String remoteUser;
    @Parameter(names = {"--remotePassword"}, description = "Show password prompt for the remote user account.")
    public boolean remotePasswordSwitch = false;
    @Parameter(names = {"--remotePwdText"}, hidden = true)
    public String remotePassword = "";
    @Parameter(names = {"--sshKeyFile"}, description = "File containing keys for remote host authentication. Default is ~/.ssh/id_rsa.")
    public String keyfile = "~/.ssh/id_rsa";
    @Parameter(names = {"--keyFilePassphrase"}, description = "Show prompt for keyfile passphrase for the keyfile if one exists.")
    public boolean keyFilePasswordSwitch = false;
    @Parameter(names = {"--keyFilePwdText"}, hidden = true)
    public String keyfilePassword = "";
    @Parameter(names = {"--trustRemote"}, description = "Bypass the known hosts file and trust the specified remote server. Defaults to false.")
    public boolean trustRemote = false;
    @Parameter(names = {"--knownHostsFile"}, description = "Known hosts file to search for target server. Default is ~/.ssh/known_hosts.")
    public String knownHostsFile = "~/.ssh/known_hosts";
    @Parameter(names = {"--sudo"}, description = "User sudo for remote commands. Defaults to false.")
    public boolean sudo = false;
    @Parameter(names = {"--remotePort"}, description = "SSH port for the host being queried. Default is 22.")
    public int remotePort = 22;


    public int getLogstashPort() {
        if (diagType.equalsIgnoreCase("logstash") || diagType.equalsIgnoreCase("logstash-api")) {
            if (port == 9200) {
                return Constants.LOGSTASH_PORT;
            }
        }
        return port;
    }

    public boolean validate() {
        boolean noErrors = true;

        if (!super.validate()) {
            noErrors = false;
        }

        if (!diagnosticTypes.contains(diagType)) {
            messages.add(diagType + " was not a valid diagnostype type. Enter --help to see valid choices");
            noErrors = false;
        }

        if (diagType.equals(Constants.remote)) {
            if (StringUtils.isEmpty(user)) {
                messages.add("For remote access a user login must be specified.");
            }
        }

        return noErrors;
    }

    protected void promptForDiagType() {

        while (!diagnosticTypes.contains(diagType)) {
            System.out.println();
            System.out.println("These are the valid diagnostic options:");
            System.out.println();
            System.out.println("local         Complete diagnostic collection for a node running on this host.");
            System.out.println("remote        Complete diagnostic collection for a node running on a different host.");
            System.out.println("api           Run only the REST diagnostic calls. No logs or system calls.");
            System.out.println("logstash      REST diagnostics and system calls.");
            System.out.println("logstash-api  Logstash REST diagnostics only.");
            System.out.println();

            diagType = consoleInput("Enter a diagnostic type from the list above or hit <Enter Key> for local: ");
            if (StringUtils.isEmpty(diagType)) {
                diagType = Constants.local;
            }
        }
    }

    protected void promptForRemoteUser() {
        System.out.println();
        System.out.println("A user account is required for remote diagnostics.");
        remoteUser = console.readLine("Enter the remote user account: ");
    }

    protected void promptForRemotePassword() {
        System.out.println();
        char[] passwordArray = console.readPassword("Enter password for remote account login or sudo. <>Enter> if none: ");
        remotePassword = new String(passwordArray);
    }

    protected void promptForKeyFile() {
        System.out.println();
        System.out.println("If authenticating to a remate server with a public key");
        System.out.println("certificate enter the absolute path to the key file location.");
        System.out.println("if necessary. The default location is ~/.ssh/id_rsa.");
        keyfile = consoleInput("or hit the <Enter Key> for the default: ");
    }

    protected void promptForKeyFilePassword() {
        System.out.println();
        System.out.println("Enter a passphrase for the keyfile if it has one.");
        keyfile = consoleInput("or hit the <Enter Key> to skip: ");
    }

    protected void getPromptForTrustRemote() {
        while (true) {
            System.out.println();
            System.out.println("Bypass validating the target host with the known_hosts file?");
            System.out.println("This is a security risk. It defaults to no.");
            String input = consoleInput("Trust server? Y|N <Enter Key> for default N: ");
            try {
                trustRemote = convertYesNoStringInput(input);
                break;
            } catch (IllegalArgumentException iae) {
                System.out.println(BaseInputs.YesNoValidationMessage);
            }
        }
    }

    protected void promptForKnownHosts() {
        System.out.println();
        System.out.println("If not trusting a remate server there must be a known hosts file.");
        System.out.println("Default location is ~/.ssh/known_hosts.");
        keyfile = consoleInput("Enter the absolute path to the known_host file location or hit the <Enter Key> for the default: ");
    }

    protected void promptForIsSudo() {

        while (true) {
            System.out.println();
            System.out.println("Run remote commands with sudo?");
            System.out.println("This may be necessary if the account used has insufficient privileges.");
            String input = consoleInput("Sudo? Y|N <Enter Key> for default N: ");
            try {
                sudo = convertYesNoStringInput(input);
                break;
            } catch (IllegalArgumentException iae) {
                System.out.println(BaseInputs.YesNoValidationMessage);
            }
        }
     }

    @Override
    public String toString() {
        String superString = super.toString();

        return superString + "," +  "DiagnosticInputs{" +
                "diagnosticTypes=" + diagnosticTypes +
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
