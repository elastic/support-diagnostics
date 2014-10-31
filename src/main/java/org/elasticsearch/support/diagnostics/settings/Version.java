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
package org.elasticsearch.support.diagnostics.settings;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@code Version}s are used to represent the current version of Elasticsearch, the minimum supported version (e.g.,
 * release version), and the maximum supported version (e.g., API removal or replacement).
 * <p />
 * For example, when running against an instance of Elasticsearch v1.0.2, the {@code Version} should be
 * <code>Version.fromString("1.0.2")</code>, which is going to be {@code true} for {@link #isSameOrNewer(Version)}
 * relative to {@link #VERSION_1_0} and {@code false} for {@link #isSameOrNewer(Version)} relative to any newer
 * {@code Version}.
 */
public class Version {
    /**
     * Version 1.0.0.
     */
    public static final Version VERSION_1_0 = new Version(1, 0, 0);
    /**
     * Version 1.1.0.
     */
    public static final Version VERSION_1_1 = new Version(1, 1, 0);
    /**
     * Version 1.2.0.
     */
    public static final Version VERSION_1_2 = new Version(1, 2, 0);
    /**
     * Version 1.4.0.
     */
    public static final Version VERSION_1_4 = new Version(1, 4, 0);

    /**
     * The regular expression used to extract the version information from the incoming version string.
     */
    public static final Pattern EXTRACTOR = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(.*)");

    /**
     * The x in the x.y.z release numbers.
     */
    private final int major;
    /**
     * The y in the x.y.z release numbers.
     */
    private final int minor;
    /**
     * The z in the x.y.z release numbers.
     */
    private final int maintenance;
    /**
     * Anything following the {@link #maintenance} number stripped of any leading dashes and periods, and always set to
     * lowercase (e.g., "-Beta1" becomes "beta1").
     * <p />
     * Example: "1.4.0Beta1" would set this to "beta1".
     */
    private final String tag;

    /**
     * Parse the {@link Version} from the {@code version} string.
     *
     * @param version The version in the format x.y.zTAG
     * @return Never {@code null}.
     * @throws NullPointerException if {@code version} is {@code null}
     * @throws IllegalArgumentException if {@code version} does not match the expected format (\d+\.\d+\.\d+.*)
     * @see #EXTRACTOR
     */
    public static Version fromString(String version) {
        Matcher versionMatcher = EXTRACTOR.matcher(version.trim());

        // ^x\.y\.z
        checkArgument(versionMatcher.matches(), "[%s] does not match expected format", version);

        int major = Integer.valueOf(versionMatcher.group(1));
        int minor = Integer.valueOf(versionMatcher.group(2));
        int maintenance = Integer.valueOf(versionMatcher.group(3));
        String tag = versionMatcher.group(4);

        return new Version(major, minor, maintenance, tag);
    }

    /**
     * Create a new {@link Version} without a {@link #getTag() tag}.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @param maintenance The Z in x.y.z
     */
    public Version(int major, int minor, int maintenance) {
        this(major, minor, maintenance, null);
    }

    /**
     * Create a new {@link Version} with an optional {@code tag}.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @param maintenance The Z in x.y.z
     * @param tag The optional tag added to the end of the version (e.g., "beta1").
     * @throws IllegalArgumentException if any version number is negative
     */
    public Version(int major, int minor, int maintenance, String tag) {
        checkArgument(major > -1, "Major cannot be negative");
        checkArgument(minor > -1, "Minor cannot be negative");
        checkArgument(maintenance > -1, "Maintenance cannot be negative");

        if (tag != null) {
            // remove leading '.', space, or '-'
            tag = tag.replaceFirst("^([\\. -]+)", "").toLowerCase();

            // if there's no real value, then discard it
            if (tag.trim().isEmpty()) {
                tag = null;
            }
        }

        // required
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        // optional
        this.tag = tag;
    }

    /**
     * Get the major version number (X in x.y.z).
     *
     * @return Never negative.
     */
    public int getMajor() {
        return major;
    }

    /**
     * Get the minor version number (Y in x.y.z).
     *
     * @return Never negative.
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Get the maintenance version number (Z in x.y.z).
     *
     * @return Never negative.
     */
    public int getMaintenance() {
        return maintenance;
    }

    /**
     * Get the optional tag (A in x.y.zA).
     *
     * @return Can be {@code null}. Never blank.
     */
    public @Nullable String getTag() {
        return tag;
    }

    /**
     * Determine if {@code this} {@link Version} is the same as the specified version.
     * <p />
     * Note: This does not consider the {@link #getMinor() minor release},
     * {@link #getMaintenance() maintenance release}, or {@link #getTag() tag} of {@code this} {@code Version}.
     *
     * @param major The X in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same as the specified version. {@code false}
     *         otherwise.
     */
    public boolean isSame(int major) {
        return this.major == major;
    }

    /**
     * Determine if {@code this} {@link Version} is the same as the specified version.
     * <p />
     * Note: This does not consider the {@link #getMaintenance() maintenance release} or {@link #getTag() tag} of
     * {@code this} {@code Version}.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same as the specified version. {@code false}
     *         otherwise.
     */
    public boolean isSame(int major, int minor) {
        return isSame(major) && this.minor == minor;
    }

    /**
     * Determine if {@code this} {@link Version} is the same as the specified version.
     * <p />
     * Note: This does not consider the {@link #getTag() tag} of {@code this} {@code Version}. Use
     * {@link #equals(Object)} for that purpose.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @param maintenance The Z in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same as the specified version. {@code false}
     *         otherwise.
     */
    public boolean isSame(int major, int minor, int maintenance) {
        return isSame(major, minor) && this.maintenance == maintenance;
    }

    /**
     * Determine if {@code this} {@link Version} is the same as the specified {@code version}.
     * <p />
     * This is a shortcut for the {@link #isSame(int, int, int)} variant.
     * <p />
     * Note: This does not consider the {@link #getTag() tag} of either {@code Version}. Use {@link #equals(Object)} for
     * that purpose.
     *
     * @param version The version to compare
     * @return {@code true} if {@code this} {@link Version} is the same as the {@code version}. {@code false}
     *         otherwise.
     * @see #isSame(int, int, int)
     */
    public boolean isSame(Version version) {
        return isSame(version.getMajor(), version.getMinor(), version.getMaintenance());
    }

    /**
     * Determine if {@code this} {@link Version} is the same or newer than the specified version.
     * <p />
     * Note: This does not consider the {@link #getMinor() minor release},
     * {@link #getMaintenance() maintenance release}, or {@link #getTag() tag} of {@code this} {@code Version}.
     *
     * @param major The X in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same or newer than the specified version.
     *         {@code false} otherwise.
     */
    public boolean isSameOrNewer(int major) {
        return this.major >= major;
    }

    /**
     * Determine if {@code this} {@link Version} is the same or newer than the specified version.
     * <p />
     * Note: This does not consider the {@link #getMaintenance() maintenance release} or {@link #getTag() tag} of
     * {@code this} {@code Version}.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same or newer than the specified version.
     *         {@code false} otherwise.
     */
    public boolean isSameOrNewer(int major, int minor) {
        return this.major > major || (this.major == major && this.minor >= minor);
    }

    /**
     * Determine if {@code this} {@link Version} is the same or newer than the specified version.
     * <p />
     * Note: This does not consider the {@link #getTag() tag} of {@code this} {@code Version}.
     *
     * @param major The X in x.y.z
     * @param minor The Y in x.y.z
     * @param maintenance The Z in x.y.z
     * @return {@code true} if {@code this} {@link Version} is the same or newer than the specified version.
     *         {@code false} otherwise.
     */
    public boolean isSameOrNewer(int major, int minor, int maintenance) {
        return this.major > major ||
               (this.major == major &&
                       (this.minor > minor || (this.minor == minor && this.maintenance >= maintenance)));
    }

    /**
     * Determine if {@code this} {@link Version} is the same or newer than the specified {@code version}.
     * <p />
     * This is a shortcut for the {@link #isSameOrNewer(int, int, int)} variant.
     * <p />
     * Note: This does not consider the {@link #getTag() tag} of either {@code Version}.
     * 
     * @param version The version to compare
     * @return {@code true} if {@code this} {@link Version} is the same or newer than the {@code version}. {@code false}
     *         otherwise.
     * @see #isSameOrNewer(int, int, int) 
     */
    public boolean isSameOrNewer(Version version) {
        return isSameOrNewer(version.getMajor(), version.getMinor(), version.getMaintenance());
    }

    /**
     * {@inheritDoc}
     * <p />
     * This <em>does</em> consider the {@link #getTag()}.
     */
    @Override
    public boolean equals(Object obj) {
        boolean equal = obj == this;

        if ( ! equal && obj instanceof Version) {
            Version other = (Version)obj;

            // the tag is nullable
            if ((tag == null && other.tag == null) || (tag != null && tag.equals(other.tag))) {
                equal = isSame(other);
            }
        }

        return equal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 100 * major + 10 * minor + maintenance;

        if (tag != null) {
            hash += tag.hashCode();
        }

        return hash;
    }

    /**
     * Get the {@link Version} in the format "{major}.{minor}.{maintenance}-{tag}" where "-{tag}" is only added if the
     * {@link #getTag() tag} is defined.
     *
     * @return Never blank.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(major).append('.').append(minor).append('.').append(maintenance);

        if (tag != null) {
            builder.append('-').append(tag);
        }

        return builder.toString();
    }
}
