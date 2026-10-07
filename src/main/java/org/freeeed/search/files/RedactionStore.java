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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.freeeed.search.web.model.redaction.Redaction;

/**
 * Per-case persistence for manual redactions.
 *
 * <p>Redactions are a <b>separate annotation layer</b> (see
 * {@code docs/decisions/redaction.md}) and are deliberately NOT written into the
 * processed output, the Solr index, or the native/rendition files -- those stay
 * untouched for forensic soundness. They live in a sidecar file next to the
 * case's processed output:</p>
 *
 * <pre>{filesLocation}/redactions.tsv</pre>
 *
 * <p>One tab-separated line per box; dependency-free on purpose (the review app
 * has no JSON library on its classpath -- {@link CaseFileService} and
 * {@code SolrTagService} both hand-roll their wire format). Tabs/newlines in the
 * (short, code-like) exemption value are escaped so a line always round-trips.</p>
 */
public class RedactionStore {
    private static final Logger log = Logger.getLogger(RedactionStore.class);
    private static final String FILE_NAME = "redactions.tsv";
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private File fileFor(String filesLocation) {
        return new File(filesLocation, FILE_NAME);
    }

    /** All redactions for the case, keyed by docId, insertion-ordered. */
    public Map<String, List<Redaction>> listAll(String filesLocation) {
        Map<String, List<Redaction>> byDoc = new LinkedHashMap<String, List<Redaction>>();
        if (filesLocation == null) {
            return byDoc;
        }
        lock.readLock().lock();
        try {
            File f = fileFor(filesLocation);
            if (!f.exists()) {
                return byDoc;
            }
            for (Object lineObj : FileUtils.readLines(f, "UTF-8")) {
                Redaction r = parseLine((String) lineObj);
                if (r == null) {
                    continue;
                }
                List<Redaction> list = byDoc.get(r.getDocId());
                if (list == null) {
                    list = new ArrayList<Redaction>();
                    byDoc.put(r.getDocId(), list);
                }
                list.add(r);
            }
        } catch (Exception e) {
            log.error("Could not read redactions from " + filesLocation, e);
        } finally {
            lock.readLock().unlock();
        }
        return byDoc;
    }

    /** Redactions for one document (never null). */
    public List<Redaction> list(String filesLocation, String docId) {
        List<Redaction> list = listAll(filesLocation).get(docId);
        return list != null ? list : new ArrayList<Redaction>();
    }

    /**
     * Replace the complete set of redactions for one document (the viewer always
     * sends the whole set for the open doc -- same approach as a Solr atomic
     * {@code set}). Other documents' lines are preserved untouched.
     */
    public void replace(String filesLocation, String docId, List<Redaction> redactions) {
        if (filesLocation == null || docId == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            Map<String, List<Redaction>> byDoc = listAllUnlocked(filesLocation);
            if (redactions == null || redactions.isEmpty()) {
                byDoc.remove(docId);
            } else {
                byDoc.put(docId, redactions);
            }
            StringBuilder sb = new StringBuilder();
            for (List<Redaction> list : byDoc.values()) {
                for (Redaction r : list) {
                    sb.append(toLine(r)).append('\n');
                }
            }
            File f = fileFor(filesLocation);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileUtils.writeStringToFile(f, sb.toString(), "UTF-8");
        } catch (Exception e) {
            log.error("Could not save redactions for doc " + docId + " in " + filesLocation, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Count of all accepted boxes in the case (for a quick UI/log summary). */
    public int count(String filesLocation) {
        int n = 0;
        for (List<Redaction> list : listAll(filesLocation).values()) {
            n += list.size();
        }
        return n;
    }

    // Read without taking the lock -- callers inside replace() already hold the write lock.
    private Map<String, List<Redaction>> listAllUnlocked(String filesLocation) {
        Map<String, List<Redaction>> byDoc = new LinkedHashMap<String, List<Redaction>>();
        try {
            File f = fileFor(filesLocation);
            if (!f.exists()) {
                return byDoc;
            }
            for (Object lineObj : FileUtils.readLines(f, "UTF-8")) {
                Redaction r = parseLine((String) lineObj);
                if (r == null) {
                    continue;
                }
                List<Redaction> list = byDoc.get(r.getDocId());
                if (list == null) {
                    list = new ArrayList<Redaction>();
                    byDoc.put(r.getDocId(), list);
                }
                list.add(r);
            }
        } catch (Exception e) {
            log.error("Could not read redactions from " + filesLocation, e);
        }
        return byDoc;
    }

    // docId \t page \t x \t y \t w \t h \t code \t source \t status \t createdAt
    private String toLine(Redaction r) {
        return esc(r.getDocId()) + '\t'
                + r.getPage() + '\t'
                + r.getX() + '\t' + r.getY() + '\t' + r.getW() + '\t' + r.getH() + '\t'
                + esc(r.getExemptionCode()) + '\t'
                + esc(r.getSource()) + '\t'
                + esc(r.getStatus()) + '\t'
                + r.getCreatedAt();
    }

    private Redaction parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] p = line.split("\t", -1);
        if (p.length < 10) {
            return null;
        }
        try {
            return new Redaction(
                    unesc(p[0]),
                    Integer.parseInt(p[1]),
                    Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                    unesc(p[6]), unesc(p[7]), unesc(p[8]),
                    Long.parseLong(p[9]));
        } catch (NumberFormatException e) {
            log.warn("Skipping malformed redaction line: " + line);
            return null;
        }
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "");
    }

    private String unesc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 't') {
                    out.append('\t');
                } else if (n == 'n') {
                    out.append('\n');
                } else {
                    out.append(n); // covers the escaped backslash and any literal
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
