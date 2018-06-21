package com.elastic.support.diagnostics;

public class PostProcessorBypass implements PostProcessor {

   public String process(String input){
      return input;
   }

}