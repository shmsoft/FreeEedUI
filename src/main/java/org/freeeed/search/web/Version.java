/*
 *
 * Copyright SHMsoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.freeeed.search.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Version and build identity for the review web app (and the FreeEed Viewer
 * package built from it).
 *
 * Bump {@link #V} MANUALLY at milestones (PATCH/MINOR/MAJOR); daily builds keep
 * the same number. The git commit SHA and build time below are stamped in
 * automatically by git-commit-id-maven-plugin so every build is traceable.
 */
public class Version {

    private static final String V = "10.8";

    // Written into WEB-INF/classes/git.properties by the build.
    private static final Properties BUILD = loadBuildProperties();

    /** Compact label for the top bar, e.g. "v10.8 · g6d40400". */
    public static String getDisplayVersion() {
        String sha = BUILD.getProperty("git.commit.id.abbrev", "");
        boolean dirty = "true".equals(BUILD.getProperty("git.dirty", ""));
        if (sha.isEmpty()) {
            return "v" + V;
        }
        return "v" + V + " · g" + sha + (dirty ? "+" : "");
    }

    /** Full detail for a tooltip, e.g. "10.8 (build 2026-06-21 14:03 UTC, g6d40400)". */
    public static String getBuildDetail() {
        String sha = BUILD.getProperty("git.commit.id.abbrev", "");
        String time = BUILD.getProperty("git.build.time", "");
        boolean dirty = "true".equals(BUILD.getProperty("git.dirty", ""));
        if (sha.isEmpty() && time.isEmpty()) {
            return V;
        }
        StringBuilder sb = new StringBuilder(V).append(" (build");
        if (!time.isEmpty()) {
            sb.append(" ").append(time).append(" UTC");
        }
        if (!sha.isEmpty()) {
            sb.append(", g").append(sha);
            if (dirty) {
                sb.append("+");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static Properties loadBuildProperties() {
        Properties props = new Properties();
        try (InputStream in = Version.class.getResourceAsStream("/git.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // Build info is best-effort; fall back to the version number alone.
        }
        return props;
    }
}
