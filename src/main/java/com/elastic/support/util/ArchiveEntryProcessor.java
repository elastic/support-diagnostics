package com.elastic.support.util;

import org.apache.commons.compress.archivers.zip.ZipFile;
import java.io.InputStream;

public interface ArchiveEntryProcessor {
   public void process(InputStream is, String name);
   public void init(ZipFile zipFile);
}
