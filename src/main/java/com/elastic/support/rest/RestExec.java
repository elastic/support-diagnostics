package com.elastic.support.rest;

import com.elastic.support.util.SystemProperties;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class RestExec {

/*    private PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    private HttpClient genericClient, esClient;

    private final static Logger logger = LogManager.getLogger(RestExec.class);

    public RestExec(int socketTimeout,
                    int requestTimeout,
                    int connectTimeout,
                    boolean isByassVerify,
                    String pkiKeystore,
                    String pkiKeystorePass) {
        try {

            RestClientBuilder builder = new RestClientBuilder(
                    socketTimeout,
                    requestTimeout,
                    connectTimeout
            );

            builder.setConnectionManager(connectionManager);
            genericClient = builder.build();

            // Set extra options for the ES specific client.
            builder.setBypassVerify(isByassVerify);
            builder.setKeyStore(pkiKeystore);
            builder.setKeyStorePass(pkiKeystorePass);

            esClient = builder.build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        connectionManager.shutdown();
    }


    public RestResult execSimpleQuery(String url, String host, int port, String scheme) {

        RestClient restClient = new RestClient(genericClient, host, port, scheme);
        return new RestResult(execGet(url, restClient));

    }

    public RestResult execSimpleQuery(String url, String host, int port, String scheme, String user, String password) {

        RestClient restClient = new RestClient(genericClient, host, port, scheme, user, password);
        return new RestResult(execGet(url, restClient));

    }

    public RestResult execQuerye(String url, String host, int port, String scheme, String user, String password) {

        RestClient restClient = new RestClient(esClient, host, port, scheme, user, password);
        return new RestResult( execGet(url, restClient));

    }

    public RestResult execQuery(String url, String host, int port, String scheme, String user, String password, OutputStream out) {

        RestClient restClient = new RestClient(esClient, host, port, scheme, user, password);
        return new RestResult( execGet(url, restClient), out);

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

    public HttpResponse execGet(String query, RestClient restClient) {

        try {
            HttpGet httpget = new HttpGet(query);
            return esClient.execute(restClient.getHttpHost(), httpget, restClient.getHttpContext());
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

            HttpEntity entity = response.getEntity();
            entity.writeTo(new FileOutputStream(destination));
            if (response.getStatusLine().getStatusCode() == 200) {
                logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk.", destination);
            } else {
                logger.log(SystemProperties.DIAG, "File {} was retrieved and saved to disk with errors.", destination);
            }
        } catch (Exception e) {
            logger.error("Error processing response", e);
        }
    }*/



}