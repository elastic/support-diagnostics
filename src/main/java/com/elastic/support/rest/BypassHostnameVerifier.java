package com.elastic.support.rest;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * This overrides any hostname mismatch in the certificate
 */
class BypassHostnameVerifier implements HostnameVerifier {

   public boolean verify(String hostname, SSLSession session) {
      return true;
   }

}
