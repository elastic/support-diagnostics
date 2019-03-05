package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.util.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class NetworkCacheCheckCmd implements Command {

    private final Logger logger = LogManager.getLogger(NetworkCacheCheckCmd.class);

    public void execute(final DiagnosticContext context) {

        Properties securityProperties = new Properties();

        final Path path = Paths.get(context.getTempDir(), "network-cache-settings.properties");
        try {
            OutputStream outpuStream = new FileOutputStream(path.toFile());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outpuStream));
            securityProperties.store(writer, "");
            outpuStream.close();
        } catch (final IOException e) {
            logger.error("Failed saving network-cache-settings.properties file.", e);
        }

    }


}
