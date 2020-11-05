package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.util.ResourceUtils;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public abstract class ElasticRestClientInputs extends BaseInputs {

    public static final String urlDescription = " Required field. Full uri in format <scheme>://<host>:<port> ex. https://localhost:9200.  HTTP access for this node must be enabled. ";
    public static final String proxyUriDescription = "Full proxy server uri in format <scheme>://<host>:<port> ex. https://localhost:8888 ";

    public static final String pkiKeystoreDescription = "Path/filename for PKI keystore with client certificate: ";
    public static final String pkiKeystorePasswordDescription = "PKI keystore password if required: ";

    public static final String verifyHostName = "Verify hostname for certificate? ";
    public static final String disableDiagCheck = "Bypass the diagnostic version check. Use when internet outbound HTTP access is blocked by a firewall. ";
    public static final String disableAuthPrompt = "Do not send login credentials - cluster is unsecured.";

    // The basics
    @Parameter(names = {"-url"}, description = urlDescription)
    public String url;
    public String host;

    @Parameter(names = {"-user"}, hidden = true)
    public String user;
    @Parameter(names = {"-password"}, hidden = true)
    public String password;

    @Parameter(names = {"-pkiKeytore"}, description = pkiKeystoreDescription)
    public String pkiKeystore = "";
    @Parameter(names = {"-pkiPassword"}, hidden = true)
    public String pkiPass = "";

    @Parameter(names = {"-proxyUrl"}, description = proxyUriDescription)
    public String proxyUrl = "";

    // Behavioral modifiers
    @Parameter(names = {"-bypassAuth"}, description = disableAuthPrompt)
    public boolean bypassAuth = false;
    @Parameter(names = {"-verifyHost"}, description = verifyHostName)
    public boolean verifyHost = false;
    @Parameter(names = {"-disableDiagCheck"}, description = disableDiagCheck)
    public boolean bypassDiagVerify = false;

    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

    public ElasticRestClientInputs(String delimiter) {
        super(delimiter);
    }

    public List<String> parseInputs(String args[]) {

        List<String> errors = super.parseInputs(args);
        errors.addAll(ObjectUtils.defaultIfNull(validateEsUri(url), emptyList));
        if (StringUtils.isNotEmpty(proxyUrl)) {
            errors.addAll(ObjectUtils.defaultIfNull(validateUri(proxyUrl), emptyList));
        }

        // Auth disabled so exit
        if (bypassAuth) {
            return errors;
        }

        boolean loginPrompt = StringUtils.isEmpty(user) || StringUtils.isEmpty(password);
        boolean usePki = StringUtils.isNotEmpty(pkiKeystore);

        // Looks weird but if they give a PKI input the only way we know whether there's
        // a password to prompt for is if we know intent. Trade off of cutting down on the
        // numnber of input fields for resetting the value.
        if (usePki) {
            if (pkiPass.equalsIgnoreCase("no")) {
                pkiPass = "";
            } else {
                if (StringUtils.isEmpty(pkiPass)) {
                    pkiPass = standardPasswordReader
                            .withDefaultValue("none")
                            .read(SystemProperties.lineSeparator + pkiKeystorePasswordDescription);
                }
            }

            errors.addAll(ObjectUtils.defaultIfNull(validatePkiInputs(), emptyList));
            errors.addAll(ObjectUtils.defaultIfNull(validatePKIScheme(), emptyList));
        } else {
            if (loginPrompt) {
                basicInteractive();
            } else {
                errors.addAll(ObjectUtils.defaultIfNull(validateEsCredentials(), emptyList));
            }
        }

        return errors;
    }

    protected void runHttpInteractive() {

        try {
            bypassDiagVerify = ResourceUtils.textIO.newBooleanInputReader()
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + disableDiagCheck);

            url = ResourceUtils.textIO.newStringInputReader()
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withMinLength(1)
                    .withValueChecker((String val, String propname) -> validateEsUri(val))
                    .read(SystemProperties.lineSeparator + urlDescription).toLowerCase();

            bypassAuth = ResourceUtils.textIO.newBooleanInputReader()
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + "Bypass authentication - cluster is unsecured? ");

            if (!bypassAuth) {
                authInteractive();
            }

            if (url.contains("https")) {
                verifyHost = standardBooleanReader
                        .withDefaultValue(false)
                        .read(SystemProperties.lineSeparator + verifyHostName);
            }

            boolean httpProxy = standardBooleanReader
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + "Use Http Proxy Server? ");

            if (httpProxy) {
                proxyUrl = ResourceUtils.textIO.newStringInputReader()
                        .withIgnoreCase()
                        .withInputTrimming(true)
                        .withValueChecker((String val, String propname) -> validateUri(val))
                        .read(SystemProperties.lineSeparator + proxyUriDescription).toLowerCase();

            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void authInteractive() {
        if (!bypassAuth) {
            String authType = ResourceUtils.textIO.newStringInputReader()
                    .withNumberedPossibleValues("Basic Auth", "PKI")
                    .withDefaultValue("Basic Auth")
                    .withValueChecker((String val, String propname) -> validatePKIScheme())
                    .read(SystemProperties.lineSeparator + "Basic Auth or PKI: ");
            if (authType.equals("PKI")) {
                pkiInteractive();
            } else {
                basicInteractive();
            }
        }
    }

    protected void basicInteractive() {

        user = ResourceUtils.textIO.newStringInputReader()
                .withInputTrimming(true)
                .withMinLength(1).read(SystemProperties.lineSeparator + "user: ");

        password = ResourceUtils.textIO.newStringInputReader()
                .withInputMasking(true)
                .withInputTrimming(true)
                .withMinLength(6).read(SystemProperties.lineSeparator + "password: ");
    }

    protected void pkiInteractive() {
        pkiKeystore = standardFileReader
                .read(SystemProperties.lineSeparator + pkiKeystoreDescription);
        pkiPass = standardPasswordReader
                .read(SystemProperties.lineSeparator + pkiKeystorePasswordDescription);
    }

    public List<String> validateEsUri(String val) {

        if (StringUtils.isEmpty(val)) {
            return Collections.singletonList("Full uri for the endpoint is required." + SystemProperties.lineSeparator + urlDescription);
        }

        if (!isValidUri(val)) {
            return Collections.singletonList("Full uri for the endpoint is required." + SystemProperties.lineSeparator + urlDescription);
        }

        host = val.substring(val.lastIndexOf("/") + 1);

        if (runningInDocker) {
            String host = val.substring(val.lastIndexOf("/") + 1);
            host = val.substring(0, host.indexOf(":"));
            if (Constants.localAddressList.contains(host)) {
                return Collections.singletonList("Local addresses are not permitted when running in a Docker container. Please use an assigned host name or IP address.");
            }
        }

        return null;
    }

    public List validatePKIScheme() {
        if (!url.contains("https")) {
            return Collections.singletonList("TLS must be enabled to use PKI.");
        }
        return null;
    }

    protected boolean isValidUri(String val) {
        return val.matches("((http|https?)://)?.*:(\\d{4,5})");
    }

    public List validateEsCredentials() {
        if (password.length() < 6) {
            return Collections.singletonList("Password must have at least 6 characters.");
        }
        return null;
    }

    public List validatePkiInputs() {
        return validateRequiredFile(pkiKeystore);
    }

    public List<String> validateUri(String val) {

        if (StringUtils.isNotEmpty(val)) {
            if (!isValidUri(val)) {
                return Collections.singletonList(proxyUriDescription);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String superString = super.toString();
        return superString + ",";
    }
}
