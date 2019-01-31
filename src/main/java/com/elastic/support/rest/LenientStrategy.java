package com.elastic.support.rest;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;

import java.security.cert.X509Certificate;

class LenientStrategy extends TrustSelfSignedStrategy {

   public LenientStrategy() {
      super();
   }

   public boolean isTrusted(X509Certificate[] chain, String authType) {
      return true;
   }
}
