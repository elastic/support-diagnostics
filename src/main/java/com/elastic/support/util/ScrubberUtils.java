package com.elastic.support.util;

import com.elastic.support.diagnostics.Constants;
import com.elastic.support.diagnostics.PostProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ScrubberUtils implements PostProcessor {

   private static final Logger logger = LogManager.getLogger();


   LinkedHashMap<Integer, Integer> ipv4 = new LinkedHashMap<>();
   LinkedHashMap<String, String> usedTokenMatches = new LinkedHashMap<>();
   List<String> configuredTokens = new ArrayList<>();
   List<String> tokens = new ArrayList<>();
   int tokenReplacementLen;

   public ScrubberUtils(){

      try {
         Map<String, Object> scrubConfig = JsonYamlUtils.readYamlFromClasspath("scrub.yml", false);
         tokenReplacementLen = SystemUtils.toInt(scrubConfig.get("tokenLenght"), 18);
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


   public String process(String line){

      if( line.contains("Apple") ){
         boolean hit = true;
      }
      line = processIpv4Addresses(line);
      line = processIpv6Addresses(line);
      line = processMacddresses(line);
      line = processTokens(line);

      return line;
   }

   public String processTokens(String line){

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

   public String processMacddresses(String input){

      Pattern pattern = Pattern.compile(Constants.MacAddrRegex);
      Matcher matcher = pattern.matcher(input);
      while(matcher.find()){
         String group = matcher.group();
         input = input.replaceFirst(group, "XX:XX:XX:XX:XX:XX");
      }
      return input;

   }

   public String processIpv4Addresses(String input){

      Pattern pattern = Pattern.compile(Constants.IPv4Regex);
      Matcher matcher = pattern.matcher(input);

      while (matcher.find()) {
         StringBuffer newIp = new StringBuffer();

         String group = matcher.group();
         String[] ipSegments = splitIpSegments(group, "\\.");
         for(int i = 0; i < 4; i++){
            int set = SystemUtils.toInt(ipSegments[i]);
            if (!ipv4.containsKey(set)){
               logger.error("Error converting ip segment {} from address: {}", SystemUtils.toString(set), group);
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

   public String processIpv6Addresses(String input){

      Pattern pattern = Pattern.compile(Constants.IPv6Regex);
      Matcher matcher = pattern.matcher(input);

      while (matcher.find()) {
         String group = matcher.group();
         input = input.replaceFirst(group, "XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX.XXXX");
      }

      return input;
   }

   public String[] splitIpSegments(String address, String sep){
      String[] ipSegments = address.split(sep);
      return ipSegments;
   }


   public String generateReplacementToken(String token, int len){

      String replacement =
         ("" + (UUID.nameUUIDFromBytes(token.getBytes())).toString())
         .replaceAll("-", "")
         .substring(0, len);

      return replacement;

   }


}