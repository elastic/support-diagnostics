/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.support.diagnostics;

import com.google.inject.Singleton;

import org.elasticsearch.support.diagnostics.process.ProcessRunner;
import org.elasticsearch.support.diagnostics.process.SynchronousProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * {@code OperatingSystem} provides information about the current running operating system based on its name.
 * <p />
 * This also provides utilities based on the determined operating system.
 */
@Singleton
public class OperatingSystem {
    /**
     * {@link Logger} for {@link OperatingSystem}.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingSystem.class);

    /**
     * The current running operating system's name.
     */
    public static final String OS_NAME = System.getProperty("os.name");

    /**
     * The name of the specified operating system.
     */
    private final String osName;
    /**
     * {@code true} if the current operating system is a Linux variant.
     */
    private final boolean linux;
    /**
     * {@code true} if the current operating system is a Linux variant.
     */
    private final boolean mac;
    /**
     * {@code true} if the current operating system is Windows.
     */
    private final boolean windows;

    /**
     * Create a new {@link OperatingSystem}.
     */
    public OperatingSystem() {
        this(OS_NAME);
    }

    /**
     * Create a new {@link OperatingSystem}.
     *
     * @param osName The operating system name
     */
    public OperatingSystem(String osName) {
        // unexpected case, but setup to allow
        if (osName == null) {
            osName = "Other";
        }

        String lowerOsName = osName.toLowerCase(Locale.ENGLISH);

        // required
        this.osName = osName;
        this.linux = lowerOsName.contains("linux");
        this.mac = lowerOsName.contains("mac");
        this.windows = lowerOsName.contains("windows");
    }

    /**
     * Get the operating system name.
     *
     * @return Never {@code null}.
     */
    public String getOperatingSystemName() {
        return osName;
    }

    /**
     * Get the hostname for the current machine.
     * <p />
     * This currently uses the <code>"COMPUTERNAME"</code> environment variable for Windows, and it attempts to use the
     * <code>"hostname"</code> process for everything else.
     *
     * @return Never {@code null}. <code>"unknown"</code> if it cannot be determined.
     */
    public String getHostname() {
        String hostname = "unknown";

        // Windows always defines the COMPUTERNAME environment variable
        if (isWindows()) {
            hostname = System.getenv("COMPUTERNAME");
        }
        // most other systems tend to define HOSTNAME, but it can be different than the hostname
        else if (isUnix()) {
            try {
                String unixHostname = runProcess("hostname");

                // ensure that we don't end up with a blank hostname
                if ( ! unixHostname.isEmpty()) {
                    hostname = unixHostname;
                }
            }
            catch (IOException e) {
                LOGGER.error("Unable to determine hostname.", e);
            }
        }

        return hostname;
    }

    /**
     * Determine if the current running operating system is a Linux variant.
     *
     * @return {@code true} if it is Linux. {@code false} otherwise.
     */
    public boolean isLinux() {
        return linux;
    }

    /**
     * Determine if the current running operating system is a Mac.
     *
     * @return {@code true} if it is Mac. {@code false} otherwise.
     */
    public boolean isMac() {
        return mac;
    }

    /**
     * Determine if the current running operating system is not one of the recognized ones.
     *
     * @return {@code true} if all other checks would be {@code false}.
     */
    public boolean isOther() {
        return ! (linux || mac || windows);
    }

    /**
     * Determine if the current running operating system is Unix variant.
     *
     * @return {@code true} if it is a Unix variant. {@code false} otherwise.
     * @see #isLinux()
     * @see #isMac()
     */
    public boolean isUnix() {
        return linux || mac;
    }

    /**
     * Determine if the current running operating system is Windows.
     *
     * @return {@code true} if it is Windows. {@code false} otherwise.
     */
    public boolean isWindows() {
        return windows;
    }

    /**
     * Run the process with the given {@code processName} and optional {@code args}.
     *
     * @param processName The process name, or path and process name if it is not on the <code>PATH</code>
     * @param args Optional arguments passed as-is to the process
     * @return Never {@code null}.
     * @throws IOException
     * @see ProcessRunner#run(String[])
     */
    public String runProcess(String processName, String... args) throws IOException {
        return new SynchronousProcessRunner(processName).run(args);
    }
}
