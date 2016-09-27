package com.elastic.support.test;

import com.elastic.support.util.DiagnosticRequestFactory;
import com.elastic.support.util.RestModule;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GenericTest {

    private final Logger logger = LoggerFactory.getLogger(GenericTest.class);

    @Test
    public void testLogstashSubmit(){
/*       DiagnosticRequestFactory diagnosticRequestFactory = new DiagnosticRequestFactory(30, 30, false, null, null, null, null);
       RestModule restModule = new RestModule(diagnosticRequestFactory, false);

       String cmds[] = {
          "?pretty",
          "_node?pretty",
          "_node/stats?pretty",
          "_node/hot_threads?human=true"
       };

       List cmdList = new ArrayList((Arrays.asList(cmds)));

       for (Object res: cmdList){
          String result = restModule.submitLogstashRequest("http", "localhost", 9600, res.toString());
          logger.error(result);
       }
*/
    }

}
