package com.elastic.support.test;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.util.ArchiveUtils;
import com.elastic.support.util.ScrubberUtils;
import com.elastic.support.util.SystemProperties;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class ScrubTests {

   private final Logger logger = LoggerFactory.getLogger(ScrubTests.class);

/*   @Test
   public void testArchive() throws Exception {
      String sourceInput = "/Users/gnieman/temp/diag-output/diagnostics-20180427-155423.tar.gz";
      int pos = sourceInput.lastIndexOf(SystemProperties.fileSeparator);
      String targetDir = sourceInput.substring(0, pos) + SystemProperties.fileSeparator + "scrubbed";
      String scrubbedName = "scrub-" + sourceInput.substring(pos + 1);
      ArchiveUtils.extractDiagnosticArchive(sourceInput, targetDir );
      ArchiveUtils.createArchive(targetDir, scrubbedName);
   }*/


/*   @Test
   public void testGUUID() throws Exception{

      for(int i = 0; i < 3; i++ ){

         String ip = "This is a test of some sort";

         String name = "" + (UUID.nameUUIDFromBytes(ip.getBytes())).toString();
         name = name.replaceAll("-", "");
         name = name.substring(0, 18);

         logger.error(name);
         Thread.sleep(60000);

      }

   }*/

   @Test
   public void checkIpv6Patterns() {


      //System.out.println(octets.size());


   }
}
