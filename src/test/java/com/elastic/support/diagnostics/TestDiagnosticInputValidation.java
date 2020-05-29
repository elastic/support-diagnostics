package com.elastic.support.diagnostics;

import com.elastic.support.util.ArchiveUtils;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestDiagnosticInputValidation {

    @Test
    public void testRemoteApi(){


        try {
            ArchiveUtils.extractArchive("/Users/gnieman/temp/bad-diag/local-diagnostics-20200506-142840.zip", "/Users/gnieman/temp/diagnostic/extract");

            Pattern pt = Pattern.compile("a*b");
            String test = "aabfooaabfooabfoob";
            Matcher matcher = pt.matcher(test);
            String ret = "";
            if(matcher.find()){
                ret = matcher.replaceAll("XXX");
            }

/*            if(mt.matches()){
                String group = mt.group();
                String ret = mt.replaceAll("zz12");

            }*/
            System.out.println(ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
