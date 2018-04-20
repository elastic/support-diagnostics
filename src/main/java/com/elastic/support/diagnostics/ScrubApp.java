package com.elastic.support.diagnostics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ScrubApp {

   private static Logger logger = LogManager.getLogger();
   public static void main(String[] args) throws Exception {

      try {
         Scrubber scrub = new Scrubber(args);
         scrub.exec();
      } catch (Exception e) {
         logger.error(e.getMessage());
      }
   }

}