package com.elastic.support.scrub;

import com.elastic.support.util.ArchiveEntryProcessor;
import com.elastic.support.Constants;
import com.elastic.support.util.JsonYamlUtils;
import com.elastic.support.util.SystemProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class ScrubProcessor implements ArchiveEntryProcessor {

   private static final Logger logger = LogManager.getLogger();


   LinkedHashMap<Integer, Integer> ipv4 = new LinkedHashMap<>();
   LinkedHashMap<String, String> usedTokenMatches = new LinkedHashMap<>();
   List<String> configuredTokens = new ArrayList<>();
   List<String> tokens = new ArrayList<>();
   String targetDir;

   public ScrubProcessor(String config, String targetDir){

      this.targetDir = targetDir;

      try {
         Map<String, Object> scrubConfig;
         if(StringUtils.isEmpty(config)){
            scrubConfig = JsonYamlUtils.readYamlFromClasspath("scrub.yml", false);
         }
         else{
            scrubConfig = JsonYamlUtils.readYamlFromPath(config, false);
         }

         if(scrubConfig.get("tokens") != null){
            tokens = (List<String>)scrubConfig.get("tokens");
            if (tokens.size() == 0){
               logger.info("Scrubbing was enabled but no substitutions were defined. Bypassing log file processing.");
            }
         }

         Random random = new Random();
         IntStream intStream = random.ints(300, 556).distinct().limit(256);

         int key = 0;
         int[] vals = intStream.toArray();
         for (int i = 0; i < 256; i++){
            ipv4.put(i, vals[i]);
         }


      } catch (Exception e) {
         logger.error("Error initializing scrubbing  files.", e);
         throw new RuntimeException("Scrub initialization failed");
      }

   }

   public String processLine(String line){

      line = processIpv4Addresses(line);
      line = processIpv6Addresses(line);
      line = processMacddresses(line);
      line = processTokens(line);

      return line;

   }

   public void process(InputStream ais, String name){

      try {

         String dir = targetDir;
         InputStream processedStream = ais;

         logger.info("Processing: {}", name);

         // It's a directory, so we don't process the file. but we do need to create a target subdirectory for subsequent files.
         if(name.endsWith("/")){
            name = name.replace("/", "");
            Files.createDirectories(Paths.get(targetDir + SystemProperties.fileSeparator + name));
            return;
         }
         else if (name.endsWith(".gz")) {
            name = name.replace(".gz", "");
            processedStream = new GZIPInputStream(ais);
         }
         else  {
            dir = targetDir;
         }

         BufferedReader br = null;
         BufferedWriter writer = new BufferedWriter(new FileWriter(
                 dir + SystemProperties.fileSeparator + name));
         br = new BufferedReader(new InputStreamReader(processedStream));

         String thisLine = null;

         while ((thisLine = br.readLine()) != null) {
            thisLine = processLine(thisLine);
            writer.write(thisLine);
            writer.newLine();
         }

         writer.close();
      } catch (Throwable t) {
         logger.error("Error processing entry,", t);
      }
   }

   private String processTokens(String line){

      for(String token: tokens){

         Pattern pattern = Pattern.compile(token);
         Matcher matcher = pattern.matcher(line);

         while (matcher.find()) {
            String replacement = null;
            String group = matcher.group();
            if(usedTokenMatches.containsKey(group)){
               replacement = usedTokenMatches.get(group);
            }
            else {
               replacement = generateReplacementToken(group, group.length());
               usedTokenMatches.put(group, replacement);
            }

            line = line.replaceFirst(group, replacement);
         }

      }

      return line;
   }

   private String processMacddresses(String input){

      Pattern pattern = Pattern.compile(Constants.MacAddrRegex);
      Matcher matcher = pattern.matcher(input);
      while(matcher.find()){
         String group = matcher.group();
         input = input.replaceFirst(group, "XX:XX:XX:XX:XX:XX");
      }
      return input;

   }

   private String processIpv4Addresses(String input){

      Pattern pattern = Pattern.compile(Constants.IPv4Regex);
      Matcher matcher = pattern.matcher(input);

      while (matcher.find()) {
         StringBuffer newIp = new StringBuffer();

         String group = matcher.group();
         String[] ipSegments = splitIpSegments(group, "\\.");
         for(int i = 0; i < 4; i++){
            int set = Integer.parseInt(ipSegments[i]);
            if (!ipv4.containsKey(set)){
               logger.error("Error converting ip segment {} from address: {}", Integer.toString(set), group);
               throw new RuntimeException("Error scrubbing IP Addresses");
            }
            int replace = ipv4.get(set);
            newIp.append(replace);
            if(i < 3){
               newIp.append(".");
            }
         }
         input = input.replaceFirst(group, newIp.toString());

      }

      return input;

   }

   private String processIpv6Addresses(String input){

      Pattern pattern = Pattern.compile(Constants.IPv6Regex);
      Matcher matcher = pattern.matcher(input);

      while (matcher.find()) {
         String group = matcher.group();
         input = input.replaceFirst(group, "XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX");
      }

      return input;
   }

   private String[] splitIpSegments(String address, String sep){
      String[] ipSegments = address.split(sep);
      return ipSegments;
   }


   private String generateReplacementToken(String token, int len){

      StringBuilder newToken = new StringBuilder();

      int passes = 1;
      if(len > 32){
         passes = (len/32) + 1;
      }

      for(int i = 0; i < passes; i++) {
         newToken.append(
                 UUID.nameUUIDFromBytes(token.getBytes()).toString()
                         .replaceAll("-", "")
         );
      }

      return newToken.toString().substring(0, len);

   }


}
