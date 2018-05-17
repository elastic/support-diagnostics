package com.elastic.support.diagnostics;

import com.elastic.support.diagnostics.PostProcessor;

public class PostProcessorBypass implements PostProcessor {

   public String process(String input){
      return input;
   }

}