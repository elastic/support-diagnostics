package com.elastic.support.test;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GenericTest {

    private final Logger logger = LoggerFactory.getLogger(GenericTest.class);

    @Test
    public void testLogstashSubmit(){
/*       ClientBuilder diagnosticRequestFactory = new ClientBuilder(30, 30, false, null, null, null, null);
       RestExec restModule = new RestExec(diagnosticRequestFactory, false);

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
