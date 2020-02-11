package com.elastic.support.diagnostics.commands;

import com.elastic.support.Constants;
import com.elastic.support.util.RemoteUserInfo;
import com.elastic.support.util.SystemProperties;
import com.elastic.support.util.SystemUtils;
import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestRemoteSystemCommands {

    List<String> cmds = new ArrayList<String>();
    {
        cmds.add("echo 'warm0thgu1tar' | sudo -S -p '' ps ax");
        cmds.add("echo 'warm0thgu1tar' | sudo -S -p '' ls -alt '/var/log/elasticsearch' | grep 'elasticsearch-.*-1.log.gz' | awk '{print $9}'");
        cmds.add("echo 'warm0thgu1tar' | sudo -S -p '' mkdir templogs");
        cmds.add("echo 'warm0thgu1tar' | sudo -S -p '' cp '/var/log/elasticsearch/elasticsearch-2019-09-19-1.log.gz' 'templogs/elasticsearch-2019-09-19-1.log.gz'");


        cmds.add("sudo -S -p '' ps ax");

        cmds.add("sudo -S -p '' ps ax");
        cmds.add("netstat -an");
        cmds.add("ps ax");
        cmds.add("ulimit -a");

    }

    private static final Logger logger = LogManager.getLogger(TestRemoteSystemCommands.class);
    private static final int BUFFER_SIZE = 8192;
    private static final int RETRY_INTERVAL = 500;
    private static final String TIMEOUT_MESSAGE =
            "Timeout period exceeded, connection dropped.";
    String knownHosts = null;
    RemoteUserInfo userInfo;
    String host = "192.168.5.174";
    private int port = 22;
    private boolean failOnError = true;
    private boolean verbose;
    private int serverAliveCountMax = 3;
    private int serverAliveIntervalSeconds = 0;
    private String command = null;
    private long maxwait = 0;

/*    private Thread thread = null;
    private String outputProperty = null;   // like <exec>
    private String errorProperty = null;
    private String resultProperty = null;
    private File outputFile = null;   // like <exec>
    private File errorFile = null;
    private String inputProperty = null;
    private String inputString = null;   // like <exec>
    private File inputFile = null;   // like <exec>
    private boolean append = false;   // like <exec>
    private boolean appenderr = false;
    private boolean usePty = false;
    private boolean useSystemIn = false;*/


    @Test
    public void testSSH() {

        userInfo = new RemoteUserInfo("gnieman","warm0thgu1tar", "");
/*        userInfo.setName("gnieman");
        userInfo.setPassword("l6sgu1tar");*/

        //userInfo.setKeyfile("/Users/gnieman/.ssh/id_rsa");
        //userInfo.setPassphrase("g1n13man");

        Session session = null;
        final StringBuffer output = new StringBuffer();
        try {

            session = openSession();
            try {
                String cmd = cmds.get(3);
                executeCommand(session, cmd, output);
                output.append("\n");
            } catch ( Exception e) {
                logger.info("Caught exception: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.info(e);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void executeCommand(final Session session, final String cmd, final StringBuffer sb) throws Exception{
        OutputStream out = new ByteArrayOutputStream();
        InputStream istream = new ByteArrayInputStream(cmd.getBytes());
        ChannelExec channel = null;

        try {
            session.setTimeout(0);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setInputStream(null);
            channel.setPty(true);
            istream = channel.getInputStream();
            channel.connect();
            byte[] tmp=new byte[1024];
            while(true){
                while(istream.available()>0){
                    int i=istream.read(tmp, 0, 1024);
                    if(i<0)break;
                    sb.append(new String(tmp, 0, i));
                }
                if(channel.isClosed()){
                    if(istream.available()>0) continue;
                    logger.log(SystemProperties.DIAG, "Cmd: {}, Exit status: {}", cmd, channel.getExitStatus());
                    break;
                }
                try{
                    TimeUnit.SECONDS.sleep(1);}catch(Exception ee){}
            }
            logger.info(sb.toString());
        }
        catch(Exception e){
            System.out.println(e);
        }
        finally {
            if (channel!= null && channel.isConnected()) {
                channel.disconnect();
            }
        }
/*        try {
            istream = new ByteArrayInputStream(cmd.getBytes());
            session.setTimeout((int) maxwait);
            *//* execute the command *//*
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setOutputStream(out);
            channel.setExtOutputStream(out);
            channel.setErrStream(out);
            channel.setInputStream(null);
*//*            istream = channel.getInputStream();
            out = channel.getOutputStream();*//*
            channel.setPty(true);
            channel.connect();
*//*            byte[] pwd = "l6sgu1tar\n".getBytes();
            channel.getOutputStream().write( pwd );
            channel.getOutputStream().flush();*//*

            //channel.getOutputStream().flush();
            // wait for it to finish
            while(! channel.isClosed()) {
                logger.info("Not done");
                TimeUnit.SECONDS.sleep(1);
            }
            logger.info("Done {}", channel.getExitStatus());

        } catch ( Exception e) {
            logger.info(e);
        } finally {
            logger.info((out.toString()));
            channel.disconnect();
            //sb.append(out.toString());
            SystemUtils.streamClose("", istream);
            if (out != null) {
                out.close();
            }
        }*/
    }

    /**
     * Writes a string to a file. If destination file exists, it may be
     * overwritten depending on the "append" value.
     *
     * @param from   string to write
     * @param to     file to write to
     * @param append if true, append to existing file, else overwrite
     */
    private void writeToFile(final String from, final boolean append, final File to)
            throws IOException {
        FileWriter out = null;
        try {
            out = new FileWriter(to.getAbsolutePath(), append);
            final StringReader in = new StringReader(from);
            final char[] buffer = new char[BUFFER_SIZE];
            int bytesRead;
            while (true) {
                bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    protected Session openSession() throws JSchException {
        final JSch jsch = new JSch();




        Session session = jsch.getSession("gnieman", host);
        final Hashtable config= new Hashtable();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications",
                "publickey,keyboard-interactive,password");



        session.setConfig(config);
        session.setUserInfo(userInfo);
        session.setServerAliveCountMax(3);
        session.setServerAliveInterval(10000);
        session.connect();
        return session;

    }

    @Test
    public void testFuture() {

        Future<Integer> future = new FutureRunner().calculate(10);
        try {
            while (!future.isDone()) {
                System.out.println("Calculating...");
                Thread.sleep(300);
            }

            Integer result = future.get(500, TimeUnit.MILLISECONDS);
            System.out.println(result);
        } catch (Exception e) {
            logger.info(e);
        }

    }

    private class FutureRunner {
        private ExecutorService executor
                = Executors.newSingleThreadExecutor();

        public Future<Integer> calculate(Integer input) {
            return executor.submit(() -> {
                int i = 0;
                while (i < 20000){
                    System.out.println(i);
                    i += 1000;
                    Thread.sleep(1000);
                }
                return input * input;
            });
        }
    }

    @Test
    public void testProcessBuilder(){

        try {
            String[] cmds = {"/bin/sh", "-c"};
            ProcessBuilder pb = new ProcessBuilder(cmds);
/*            args.add("/bin/sh");
            args.add("-c");
            args.add("top -l 1");*/
            /*args.add("top");
            args.add("-l");
            args.add("1");*/

            //args.add(" ls -alt /Users/gnieman/Servers/elasticsearch-6.6.2-local/logs | grep 'monitoring-six-six-.*-1.log.gz' | awk '{print $9}'");
            List current = pb.command();
            if(current.size() == 2){
                current.add("top -l 1");
            }
            else{
                current.set(2,"top -l 1" );
            }
            //pb.command(args);
            pb.redirectErrorStream(true);
            Process pr = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(pr.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while (( line = reader.readLine()) != null) {
                sb.append(line + SystemProperties.lineSeparator);
            }
            pr.waitFor();

            //String output = IOUtils.toString(in, "UTF-8");
            logger.info(sb.toString());

            current.set(2, "ps -fp 14887 | awk '{ if (NR!=1) print $8}'");
            //pb.command(args);
            pr = pb.start();
            reader =
                    new BufferedReader(new InputStreamReader(pr.getInputStream()));
            sb = new StringBuffer();
            while (( line = reader.readLine()) != null) {
                sb.append(line);
            }
            pr.waitFor();
            logger.info(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}
