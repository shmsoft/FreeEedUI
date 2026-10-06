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
package org.freeeed.search.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Loads the FOIA/public-records exemption codes a reviewer can apply.
 *
 * <p>Per {@code docs/decisions/redaction.md} the codes are <b>data, not code</b>:
 * jurisdictions differ and counsel maintains the list. They are read from
 * {@code redaction-exemptions.tsv}. An operator-editable copy in the server's
 * working directory (e.g. on the appliance) wins; otherwise the bundled default
 * on the classpath is used. The file is re-read on each request so an edit takes
 * effect without a restart.</p>
 */
public class ExemptionProfiles {
    private static final Logger log = Logger.getLogger(ExemptionProfiles.class);
    private static final String FILE_NAME = "redaction-exemptions.tsv";

    public static class Code {
        public final String code;
        public final String label;
        public Code(String code, String label) { this.code = code; this.label = label; }
    }

    /** profile name -> ordered codes. */
    public Map<String, List<Code>> load() {
        Map<String, List<Code>> profiles = new LinkedHashMap<String, List<Code>>();
        InputStream in = null;
        try {
            File external = new File(FILE_NAME);
            if (external.isFile()) {
                in = new FileInputStream(external);
                log.info("Loading redaction exemptions from " + external.getAbsolutePath());
            } else {
                in = getClass().getClassLoader().getResourceAsStream(FILE_NAME);
            }
            if (in == null) {
                log.warn("No " + FILE_NAME + " found (classpath or working dir); no exemption codes available");
                return profiles;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] p = line.split("\t");
                if (p.length < 2) {
                    continue;
                }
                String profile = p[0].trim();
                String code = p[1].trim();
                String label = p.length >= 3 ? p[2].trim() : code;
                List<Code> codes = profiles.get(profile);
                if (codes == null) {
                    codes = new ArrayList<Code>();
                    profiles.put(profile, codes);
                }
                codes.add(new Code(code, label));
            }
        } catch (Exception e) {
            log.error("Could not load " + FILE_NAME, e);
        } finally {
            if (in != null) {
                try { in.close(); } catch (Exception ignore) { }
            }
        }
        return profiles;
    }

    /** Emit the profiles as JSON for the viewer. */
    public String toJson() {
        Map<String, List<Code>> profiles = load();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"profiles\":[");
        boolean firstProfile = true;
        for (Map.Entry<String, List<Code>> e : profiles.entrySet()) {
            if (!firstProfile) {
                sb.append(',');
            }
            firstProfile = false;
            sb.append("{\"name\":\"").append(jsonEscape(e.getKey())).append("\",\"codes\":[");
            boolean firstCode = true;
            for (Code c : e.getValue()) {
                if (!firstCode) {
                    sb.append(',');
                }
                firstCode = false;
                sb.append("{\"code\":\"").append(jsonEscape(c.code))
                  .append("\",\"label\":\"").append(jsonEscape(c.label)).append("\"}");
            }
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
