package com.elastic.support.scrub;

public class PostProcessorBypass implements PostProcessor {

   public String process(String input){
      return input;
   }

}