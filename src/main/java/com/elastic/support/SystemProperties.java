package com.elastic.support;


import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemProperties {

    public static final String osName = System.getProperty("os.name");

    public static final String pathSeparator = System.getProperty("path.separator");

    public static final String fileSeparator = System.getProperty("file.separator");

    public static final String userDir = System.getProperty("user.dir");

    public static final String userHome = System.getProperty("user.home");

    private static final String UTC_DATE_FORMAT = "MM/dd/yyyy KK:mm:ss a Z";
    
    private static final String FILE_DATE_FORMAT = "yyyyMMdd-KKmmssa";

    public static String getUtcDateString(){
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat(UTC_DATE_FORMAT);
        return format.format(curDate);
    }

    public static String getFileDateString(){
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat(FILE_DATE_FORMAT);
        return format.format(curDate);
    }
}
