package com.elastic.support;

public class PostProcessorBypass implements PostProcessor {

   public String process(String input){
      return input;
   }

}