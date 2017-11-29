package com.elastic.support.diagnostics.commands;

import com.elastic.support.diagnostics.chain.DiagnosticContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SecurityPropertiesCmd extends AbstractDiagnosticCmd {

    @Override
    public boolean execute(final DiagnosticContext context) {
        final Path path = Paths.get(context.getOutputDir(), "diagnostics", "security-properties.txt");
        try (
                OutputStream outpuStream = new FileOutputStream(path.toFile());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outpuStream);
                BufferedWriter writer = new BufferedWriter(outputStreamWriter)) {
            try {
                final List<String> securityProperties = readSecurityProperites();
                for (final String securityProperty : securityProperties) {
                    writer.write(securityProperty);
                }
            } catch (final IOException e) {
                try (PrintWriter printWriter = new PrintWriter(writer)) {
                    e.printStackTrace(printWriter);
                    return false;
                }
            }
        } catch (final IOException e) {
            logger.error("failed opening " + path.toString(), e);
            return false;
        }
        return true;
    }

    private List<String> readSecurityProperites() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("security-properties.txt");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            final List<String> result = new ArrayList<>();
            while (true) {
                final String line = reader.readLine();
                if (line == null) break;
                result.add(String.format(Locale.ROOT, "%s=%s\n", line, Security.getProperty(line)));
            }
            return result;
        }
    }

}
