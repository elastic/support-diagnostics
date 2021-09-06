/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 *  or more contributor license agreements. Licensed under the Elastic License
 *  2.0; you may not use this file except in compliance with the Elastic License
 *  2.0.
 */
package co.elastic.support.util;

import org.apache.commons.compress.archivers.zip.ZipFile;
import java.io.InputStream;

public interface ArchiveEntryProcessor {
   public void process(InputStream is, String name);
   public void init(ZipFile zipFile);
}
