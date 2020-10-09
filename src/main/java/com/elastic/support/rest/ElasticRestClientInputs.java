package com.elastic.support.rest;

import com.beust.jcommander.Parameter;
import com.elastic.support.BaseInputs;
import com.elastic.support.Constants;
import com.elastic.support.util.ResourceCache;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public abstract class ElasticRestClientInputs extends BaseInputs {

    public static final String uriDescription = " Required field. Full uri in format <scheme>://<host>:<port> ex. https://localhost:9200.  HTTP access for this node must be enabled. ";
    public static final String proxyUriDescription = "Full proxy server uri in format <scheme>://<host>:<port> ex. https://localhost:8888 ";

    public static final String pkiKeystoreDescription = "Path/filename for PKI keystore with client certificate: ";
    public static final String pkiKeystorePasswordDescription = "PKI keystore password if required: ";

    public static final String deprecationMessage = "Deprecated parameters: -h, --port, -u -p, --ssl are being used. Consult the documentation or run --help to see the new formate.";
    public static final String verifyHostName = "Verify hostname for certificate? ";
    public static final String disableDiagCheck = "Bypass the diagnostic version check. Use when internet outbound HTTP access is blocked by a firewall. ";

    public final static String userLoginAuth = "Username:Password";
    public final static String pkiLoginAuth = "PKI:";

    // Deprecrated parameters - send back a message prompting for new
    // Indicates we need to prompt for a masked input
    @Parameter(names = {"-h", "--host"}, hidden = true)
    public String host = "";
    @Parameter(names = {"--port"}, hidden = true)
    public int port = -1;
    @Parameter(names = {"-u", "--user"}, hidden = true)
    public String user = "";
    @Parameter(names = {"-p", "--password"}, hidden = true)
    public String password = "";
    @Parameter(names = {"-s", "--ssl"}, hidden = true)
    public boolean isSsl = false;
    @Parameter(names = {"--pkiKeystore"}, description = pkiKeystoreDescription)
    public String pkiKeystore = "";

    // The basics
    @Parameter(names = {"-n", "--node", "--url"}, description = uriDescription)
    public String url = "";
    @Parameter(names = {"--proxyUri"}, description = proxyUriDescription)
    public String proxyUrl = "";

    public String pkiPass = "";
    // Not shown in the the help display due to security risks - allow input via command line arguments in plain text.
    @Parameter(names = {"--credentials"}, hidden = true)
    public String credentials = "";
    @Parameter(names = {"--proxyCredentials"}, hidden = true)
    public String proxyCredentials = "";
    @Parameter(names = {"--pkiCredentials"}, hidden = true)
    public String pkiCredentials = "";
    public String proxyUser = "";
    public String proxyPassword = "";

    // Behavioral modifiers
    @Parameter(names = {"--bypassAuth"}, description = "Do not send login credentials - cluster is unsecured.")
    public boolean bypassAuth = false;
    @Parameter(names = {"--verifyHost"}, description = verifyHostName)
    public boolean verifyHost = false;
    @Parameter(names = {"--disableDiagCheck"}, description = disableDiagCheck)
    public boolean bypassDiagVerify = false;

    Logger logger = LogManager.getLogger(ElasticRestClientInputs.class);

    public List<String> parseInputs(String args[]) {

        List<String> errors = super.parseInputs(args);
        if (StringUtils.isNotEmpty(host) ||
                StringUtils.isNotEmpty(user) ||
                port != -1 ||
                StringUtils.isNotEmpty(password) ||
                isSsl == true) {
            errors.addAll(Collections.singletonList(deprecationMessage));
        }

        errors.addAll(ObjectUtils.defaultIfNull(validateEsUri(url), emptyList));

        if (!bypassAuth && (StringUtils.isEmpty(credentials) && StringUtils.isEmpty(pkiCredentials))) {
            authInteractive();
        } else {
            if (StringUtils.isNotEmpty(credentials)) {
                errors.addAll(ObjectUtils.defaultIfNull(validateEsCredentials(), emptyList));
            } else {
                errors.addAll(ObjectUtils.defaultIfNull(validatePkiCredentials(), emptyList));
            }
        }

        if (StringUtils.isNotEmpty(proxyUrl)) {
            errors.addAll(ObjectUtils.defaultIfNull(validateUri(proxyUrl), emptyList));
            if (StringUtils.isNotEmpty(proxyCredentials)) {
                errors.addAll(ObjectUtils.defaultIfNull(validateProxyCredentials(), emptyList));
            } else {
                proxyInteractive();
            }

        }

        errors.addAll(ObjectUtils.defaultIfNull(validateFile(pkiKeystore), emptyList));
        if (StringUtils.isNotEmpty(pkiKeystore)) {
            errors.addAll(ObjectUtils.defaultIfNull(validatePKI(pkiLoginAuth), emptyList));
        }

        return errors;

    }

    protected void runHttpInteractive() {

        try {
            bypassDiagVerify = ResourceCache.textIO.newBooleanInputReader()
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + disableDiagCheck);

            url = ResourceCache.textIO.newStringInputReader()
                    .withIgnoreCase()
                    .withInputTrimming(true)
                    .withMinLength(1)
                    .withValueChecker((String val, String propname) -> validateEsUri(val))
                    .read(SystemProperties.lineSeparator + uriDescription).toLowerCase();

            bypassAuth = ResourceCache.textIO.newBooleanInputReader()
                    .withDefaultValue(false)
                    .read(SystemProperties.lineSeparator + "Bypass authentication - cluster is unsecured? ");

            if(!bypassAuth){
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
                proxyUrl = ResourceCache.textIO.newStringInputReader()
                        .withIgnoreCase()
                        .withInputTrimming(true)
                        .withValueChecker((String val, String propname) -> validateUri(val))
                        .read(SystemProperties.lineSeparator + proxyUriDescription).toLowerCase();

                proxyInteractive();

            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void authInteractive() {
        if (!bypassAuth) {
            String authType = ResourceCache.textIO.newStringInputReader()
                    .withNumberedPossibleValues(userLoginAuth, pkiLoginAuth)
                    .withDefaultValue(userLoginAuth)
                    .withValueChecker((String val, String propname) -> validatePKI(val))
                    .read(SystemProperties.lineSeparator + "Basic or PKI Authentication: ");
            if (authType.equals(pkiLoginAuth)) {
                pkiInteractive();
            } else {
                basicInteractive();
            }
        }
    }

    protected void basicInteractive() {

        user = ResourceCache.textIO.newStringInputReader()
                .withInputMasking(true)
                .withInputTrimming(true)
                .withMinLength(1).read(SystemProperties.lineSeparator + "user: ");

        password = ResourceCache.textIO.newStringInputReader()
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


    protected void proxyInteractive() {

        boolean httpProxyAuth = standardBooleanReader
                .withDefaultValue(false)
                .read(SystemProperties.lineSeparator + "Proxy secured? ");

        if (httpProxyAuth) {
            proxyUser = ResourceCache.textIO.newStringInputReader()
                    .withInputMasking(true)
                    .withInputTrimming(true)
                    .withMinLength(1).read(SystemProperties.lineSeparator + "proxy user: ");

            if (StringUtils.isNotEmpty(proxyUser)) {
                proxyPassword = ResourceCache.textIO.newStringInputReader()
                        .withInputMasking(true)
                        .withInputTrimming(true)
                        .withMinLength(1).read(SystemProperties.lineSeparator + "proxy password: ");
            }
        }
    }


    public List<String> validateEsUri(String val) {

        if (StringUtils.isEmpty(val)) {
            return Collections.singletonList("Full uri for the endpoint is required." + SystemProperties.lineSeparator + uriDescription);
        }

        if (runningInDocker) {
            String host = url.substring(url.lastIndexOf("/") + 1);
            host = val.substring(0, host.indexOf(":"));
            if (Constants.localAddressList.contains(host)) {
                return Collections.singletonList("Local addresses are not permitted when running in a Docker container. Please use an assigned host name or IP address.");
            }
        }

        if (!isValidUri(val)) {
            return Collections.singletonList("Full uri for the endpoint is required." + SystemProperties.lineSeparator + uriDescription);
        }

        return null;
    }

    public List validatePKI(String val) {
        if (val.equals(pkiLoginAuth)) {
            if (!url.contains("https")) {
                return Collections.singletonList("TLS must be enabled to use PKI.");
            }
        }
        return null;
    }

    protected boolean isValidUri(String val) {
        return val.matches("((http|https?)://)?.*:(\\d{4,5})");
    }

    public List validateEsCredentials() {
        String[] combo = credentials.split(":");

        if (combo.length < 2){
            return Collections.singletonList("Invalid credentials content or format " + userLoginAuth);
        }

        if(combo[1].length() < 6) {
            return Collections.singletonList("Password must have at least 6 characters. " + userLoginAuth);
        }

        user = combo[0];
        password = combo[1];

        return null;
    }

    public List validatePkiCredentials() {
        String[] combo = pkiCredentials.split(":");
        List errs = validateRequiredFile(combo[0]);
        if (errs.size() > 0) {
            return Collections.singletonList("Format for credentials is <absolute path to filename> or <absolute path to filename>:password");
        }

        if (StringUtils.isNotEmpty(combo[0])) {
            pkiKeystore = combo[0];
        }
        if (combo.length > 1 && StringUtils.isNotEmpty(combo[1])) {
            pkiKeystore = combo[1];
        }

        return null;
    }

    public List validateProxyCredentials() {
        if (!proxyCredentials.contains(":")) {
            return Collections.singletonList("Format for credentials is" + userLoginAuth);
        }

        String[] combo = proxyCredentials.split(":");

        if (combo.length < 2) {
            return Collections.singletonList("Format for credentials is" + userLoginAuth);
        }

        proxyUser = combo[0];
        proxyPassword = combo[1];

        return null;

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
