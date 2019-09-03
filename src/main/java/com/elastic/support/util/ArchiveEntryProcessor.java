package com.elastic.support.util;

import java.io.InputStream;

public interface ArchiveEntryProcessor {
   public void process(InputStream is, String name);
}
