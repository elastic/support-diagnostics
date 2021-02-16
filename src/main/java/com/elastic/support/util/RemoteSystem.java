package com.elastic.support.util;

import com.elastic.support.Constants;
import com.elastic.support.diagnostics.DiagnosticException;
import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RemoteSystem extends SystemCommand {

    public static final String sudoPrefix = "echo '{{PASSWORD}}' | sudo -S -p '' ";
    public static final String sudoNoPasswordPrefix = "sudo -S -p '' ";

    private static final Logger logger = LogManager.getLogger(RemoteSystem.class);

    private     boolean disabledForPlatform = false;
    private     Session session;
    private     String sudo = "";

    public RemoteSystem(String osName,
                        String remoteUser,
                        String remotePassword,
                        String host,
                        int port,
                        String keyFile,
                        String keyFilePass,
                        String knownHostsFile,
                        boolean trustRemote,
                        boolean isSudo){

        this.osName = osName;

        try {
            if(osName.equals(Constants.winPlatform)){
                logger.info(Constants.CONSOLE, "Windows is not supported for remote calls at this time. Session not created.");
                disabledForPlatform = true;
                return;
            }

            JSch jsch; jsch = new JSch();
            if(StringUtils.isNotEmpty(keyFile)){
                jsch.addIdentity(keyFile);
                if(StringUtils.isEmpty(keyFilePass)){
                    keyFilePass = null;
                }
            }
            if(StringUtils.isNotEmpty(remotePassword)){
                if(isSudo){
                    sudo = sudoPrefix.replace("{{PASSWORD}}", remotePassword);
                }
            }
            else{
                remotePassword = null;
                if(isSudo){
                    sudo = sudoNoPasswordPrefix;
                }
            }

            if(StringUtils.isNotEmpty(knownHostsFile)){
                jsch.setKnownHosts(knownHostsFile);
            }

            String hostKeyChecking = "yes";
            if (trustRemote) {
                hostKeyChecking = "no";
            }

            UserInfo userInfo = new RemoteUserInfo(remoteUser, remotePassword, keyFilePass);
            session = jsch.getSession(remoteUser, host, port);
            final Hashtable config = new Hashtable();

            config.put("StrictHostKeyChecking", hostKeyChecking);
            config.put("PreferredAuthentications",
                    "publickey,keyboard-interactive,password");

            session.setConfig(config);
            session.setUserInfo(userInfo);
            session.setServerAliveCountMax(3);
            session.setServerAliveInterval(10000);
            session.connect();

        } catch (JSchException e) {
            throw new DiagnosticException("Error obtaining session for remote server - try running with diagnostic type: api.");
        }
    }

    public String runCommand(String cmd) {
        if(disabledForPlatform){
            logger.info(Constants.CONSOLE, "Windows is not supported for remote calls at this time");
            return "No Content available: Incompatible platform.";
        }

        // If it's sudo we need to pipe the password into the input stream
        // Only there if if's been set, otherwise just prepending an empty string
        cmd = sudo + cmd;

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream errout = new ByteArrayOutputStream();
        StringBuffer sb = new StringBuffer();

        InputStream istream = null;
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
                    logger.error( "Cmd: {}, Exit status: {}", scrubCommandText(cmd), channel.getExitStatus());
                    break;
                }
                try{
                    TimeUnit.SECONDS.sleep(1);}catch(Exception ee){}
            }
        }
        catch(Exception e){
            logger.info(Constants.CONSOLE, "Failed remote command: {}. {}", cmd, Constants.CHECK_LOG) ;
            logger.error("System command failed.", e);
        }
        finally {
            if (channel!= null && channel.isConnected()) {
                channel.disconnect();
            }
        }

        return sb.toString();
    }

    @Override
    public void copyLogs(List<String> entries, String logDir, String targetDir) {

        ChannelSftp channelSftp = null;
        String tempDir = "templogs";
        String mkdir = "mkdir templogs";
        String copy = " cp '{{LOGPATH}}/{{FILENAME}}'  '{{TEMP}}/{{FILENAME}}'";
        copy = copy.replace("{{LOGPATH}}", logDir);
        copy = copy.replace("{{TEMP}}", tempDir);
        // Because logs are only available to the elasticsearch user, other accounts with access,
        // or sudo, we need to copy them into the home directory of the account being used. If sudo was
        // specified or the appropriate account level it will work. If not, we would have gotten an empty
        // file listing and exited by now.
        try {
            runCommand(mkdir);
            for(String filename : entries){
                String cmd = copy.replace("{{FILENAME}}", filename);
                runCommand(cmd);
            }

            // Now they have been moved from the log directory into the temp dir created in the user's home
            // we can move them to the the calling host. It should pull them all in one pass.
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.get("templogs/*", targetDir);
            channelSftp.exit();

            // clean up the temp logs on the remote host
            runCommand(" rm -Rf templogs");

        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "Error occurred copying remote logfiles. {}", Constants.CHECK_LOG);
            logger.error( e);
        } finally {
            if(channelSftp != null && channelSftp.isConnected()){
                channelSftp.disconnect();
            }
        }
    }

    /**
    * On this function we will try to collect the journalctl logs, 
    * Some services as Kibana installed with the RPM package will give the access to the logs using the journalctl command
    * We will create a temperory file to copy those logs, then move them to the calling host.
    *
    * @param  String serviceName
    * @param  String targetDir
    *
    * @return         void
    */
    @Override
    public void copyLogsFromJournalctl(String serviceName, String targetDir) {

        ChannelSftp channelSftp = null;
        String tempDir = "templogs";
        String mkdir = "mkdir templogs";
        String journalctl = " journalctl -u {{SERVICE}} > '{{TEMP}}/{{SERVICE}}.log'";
        journalctl = journalctl.replace("{{SERVICE}}", serviceName);
        journalctl = journalctl.replace("{{TEMP}}", tempDir);
       
        try {
            runCommand(mkdir);
            runCommand(journalctl);

            // Now they have been moved from the log directory into the temp dir created in the user's home
            // we can move them to the the calling host. It should pull them all in one pass.
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.get("templogs/*", targetDir);
            channelSftp.exit();

            // clean up the temp logs on the remote host
            runCommand(" rm -Rf templogs");

        } catch (Exception e) {
            logger.info(Constants.CONSOLE, "Error occurred copying remote logfiles. {}", Constants.CHECK_LOG);
            logger.error( e);
        } finally {
            if(channelSftp != null && channelSftp.isConnected()){
                channelSftp.disconnect();
            }
        }
    }


    private String scrubCommandText(String command){
        // If we had to pipe in suoo information with the command remove it before
        // displaying the command status.
        if(command.startsWith("echo") && command.contains("sudo")) {
            int idx = command.indexOf("-p") + 6;
            return command.substring(idx);
        }
        return command;

    }

    @Override
    public void close() throws IOException {
        if(session != null && session.isConnected()){
            session.disconnect();
        }
    }
}
