package com.elastic.support.rest;

import com.elastic.support.diagnostics.DiagnosticInputs;
import com.elastic.support.diagnostics.chain.GlobalContext;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

public class RestExec implements Closeable {

    private PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
    private HttpClient genericClient, esClient;
    private static final int retries = 3;

    private final static Logger logger = LogManager.getLogger(RestExec.class);

    public RestExec(DiagnosticInputs diagnosticInputs, Map<String, Integer> settings) {
        try {
            connectionManager.setMaxTotal(25);
            connectionManager.setDefaultMaxPerRoute(5);

            ClientBuilder builder = new ClientBuilder(
                    settings.get("socketTimeout"),
                    settings.get("requestTimeout"),
                    settings.get("connectTimeout")
            );

            builder.setConnectionManager(connectionManager);
            genericClient = builder.build();

            // Set some extra options for the ES specific client.
            builder.setBypassVerify(diagnosticInputs.isBypassDiagVerify());
            builder.setKeyStore(diagnosticInputs.getKeystore());
            builder.setKeyStorePass(diagnosticInputs.getKeystorePass());

            esClient = builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        connectionManager.shutdown();
    }

    public String execGeneric(String url, HttpHost httpHost, String user, String password) {

        HttpClientContext httpContext = getLocalContext(httpHost);
        applyCredentials(httpContext, user, password);
        return execQuery(genericClient, url, httpHost, httpContext);

    }

    public String execSimpleDiagnosticQuery(String url,  HttpHost httpHost){

        HttpClientContext httpContext = getLocalContext(httpHost);
        applyCredentials(httpContext, GlobalContext.getDiagnosticInputs().getUser(), GlobalContext.getDiagnosticInputs().getPassword());
        return execQuery(genericClient, url, httpHost, httpContext);

    }

    private String execQuery( HttpClient client, String url, HttpHost httpHost, HttpClientContext httpContext){
        HttpResponse response = null;

        try  {
            response = exec(client, url, httpHost, httpContext);
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return getResponseString(response);
            } else {
                logger.info("Error completing query {}. See diagnostics.log for more detail. Status: {}", url, status);
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error executing query", e);
            throw new RuntimeException();
        }
        finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public void execConfiguredDiagnosticQuery(String url, String destination, HttpHost httpHost) {

        HttpResponse response = null;
        try {
            HttpClientContext httpContext = getLocalContext(httpHost);
            applyCredentials(httpContext, GlobalContext.getDiagnosticInputs().getUser(), GlobalContext.getDiagnosticInputs().getPassword());
            boolean belowLimit = true;
            int attempt = 0;

            while (belowLimit){
                attempt ++;
                response = exec(esClient, url, httpHost, httpContext);
                int status = response.getStatusLine().getStatusCode();
                if (status == 200 || attempt == 3) {
                    belowLimit = false;
                }
                else{
                    logger.log(SystemProperties.DIAG, "Status: {}, Status Reason: {}", status, response.getStatusLine().getReasonPhrase());
                    if (! isRetryable(status)) {
                        throw new RuntimeException();
                    }
                }
            }

            streamResponseToFile(response, destination);

        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Could not run query. See previous log entries.");
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public HttpResponse exec(HttpClient client, String query, HttpHost httpHost, HttpClientContext httpContext) {

        try {
            HttpGet httpget = new HttpGet(query);
            return esClient.execute(httpHost, httpget, httpContext);
        } catch (HttpHostConnectException e) {
            logger.log(SystemProperties.DIAG, "Host connection error.", e);
            throw new RuntimeException("Host connection");
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Unexpected Execution Error", e);
            throw new RuntimeException("Unexpected exception");
        }

    }

    private String getResponseString(HttpResponse response) {

        try {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            logger.log(SystemProperties.DIAG, "Error while processing response.", e);
        }

        return null;
    }

    private void streamResponseToFile(HttpResponse response, String destination) {

        try (FileOutputStream fos = new FileOutputStream(destination)) {

            // Use HttpEntity.writeTo
            org.apache.http.HttpEntity entity = response.getEntity();
            InputStream responseStream = entity.getContent();
            IOUtils.copy(responseStream, fos);
            if (response.getStatusLine().getStatusCode() == 200) {
                logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk.", destination);
            } else {
                logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk with errors.", destination);
            }
        } catch (Exception e) {
            logger.error("Error processing response", e);
        }
    }

    private boolean isRetryable(int statusCode) {

        if (statusCode == 400) {
            logger.info("No data retrieved.");
            return true;
        } else if (statusCode == 401) {
            logger.info("Authentication failure: invalid login credentials. Check logs for details.");
            return false;
        } else if (statusCode == 403) {
            logger.info("Authorization failure or invalid license. Check logs for details.");
            return false;
        } else if (statusCode == 404) {
            logger.info("Endpoint does not exist.");
            return true;
        } else if (statusCode > 500 && statusCode < 600) {
            logger.info("Unrecoverable server error.");
            return true;
        }

        return false;

    }

    private HttpClientContext getLocalContext(HttpHost httpHost) {

        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local
        // auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(httpHost, basicAuth);

        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);

        return localContext;
    }

    private void applyCredentials(HttpClientContext context, String user, String password) {

        if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    authScope,
                    new UsernamePasswordCredentials(user, password));
            context.setCredentialsProvider(credentialsProvider);
        }

    }

}