package com.elastic.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class SystemUtils {

    private static final String UTC_DATE_FORMAT = "MM/dd/yyyy KK:mm:ss a Z";
    private static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

    public static void zipDir(String relPath, File file, ZipOutputStream out) {
        try {
            File[] files = file.listFiles();
            assert files != null;
            byte[] buf = new byte[1024];

            for (File fl : files) {
                if (fl.isDirectory()) {
                    out.putNextEntry(new ZipEntry(relPath + fl.getName() + SystemProperties.fileSeparator));
                    SystemUtils.zipDir(relPath + fl.getName() + SystemProperties.fileSeparator, fl, out);
                    //fl.delete();
                    continue;

                }
                FileInputStream in = new FileInputStream(fl);
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(relPath + fl.getName()));
                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                // Complete the entry
                out.closeEntry();
                in.close();
                //fl.delete();
            }
        } catch (Exception e) {
            logger.error("Couldn't create archive.\n", e);
            throw new RuntimeException(("Error creating compressed archive from statistics files."));

        }
    }

    public static void zipDir(File file, ZipOutputStream out) {
        try {


            File[] files = file.listFiles();
            assert files != null;
            byte[] buf = new byte[1024];


            for (File fl : files) {
                if (fl.isDirectory()) {
                    zipDir(fl, out);
                    continue;

                }
                FileInputStream in = new FileInputStream(fl);
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(fl.getName()));
                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                // Complete the entry
                out.closeEntry();
                in.close();
            }


        } catch (Exception e) {
            logger.error("Couldn't create archive.\n", e);
            throw new RuntimeException(("Error creating compressed archive from statistics files."));

        }
    }


    public static boolean deleteDir(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return true;
        }

        if (!file.isDirectory())
            return file.delete();

        String[] list = file.list();
        for (int i = 0; i < list.length; i++) {
            if (!deleteDir(filePath + File.separator + list[i]))
                return false;
        }

        return file.delete();
    }

    public static String getUtcDateString(){
        Date curDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat(UTC_DATE_FORMAT);
        return format.format(curDate);

    }

    public static void copyFile(String src, String dest) {

        logger.error("source:" + src + " dest:" + dest);
        try {
            File sourceFile = new File(src);

            File destFile = new File(dest);
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
        } catch (Exception e) {
            String msg = "Problem copying file: " + src;
            logger.error(msg, e);
        }
    }

    public static Map readYaml(InputStream inputStream, boolean isBlock){
        Map doc = new LinkedHashMap();

        try{
            DumperOptions options = new DumperOptions();
            if(isBlock){
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            }

            Yaml yaml=new Yaml(options);
            doc = (Map)yaml.load(inputStream);

        }
        catch (  Exception e) {
            logger.error("Not able to read config file ", e);
            throw new RuntimeException("Error reading configuration");
        }
        return doc;
    }

    public static String getFileFromPath(String path){
        Path p = Paths.get(path);
        String file = p.getFileName().toString();
        return file;

    }
}
