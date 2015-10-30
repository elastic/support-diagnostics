package com.elastic.support.test;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;

public class SandboxTest {

    @Test
    public void testFilters() throws Exception{

        String filters[]  = { "*.*"};
        FileFilter fileFilter = new WildcardFileFilter(filters);
        File src = new File("/Users/gnieman/servers/elasticsearch-2.0.0/config/shield");
        File dest = new File("/Users/gnieman/temp/es2");
        FileUtils.copyDirectory(src, dest,  true);

    }





}
