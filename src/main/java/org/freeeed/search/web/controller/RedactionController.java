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
package org.freeeed.search.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.freeeed.search.files.ExemptionProfiles;
import org.freeeed.search.files.RedactionStore;
import org.freeeed.search.web.WebConstants;
import org.freeeed.search.web.model.Case;
import org.freeeed.search.web.model.redaction.Redaction;
import org.freeeed.search.web.session.SolrSessionObject;
import org.springframework.web.servlet.ModelAndView;

/**
 * Manual-redaction endpoint (FOIA; see {@code docs/decisions/redaction.md}).
 *
 * <p>Stores/reads the per-document redaction boxes (the annotation layer) and
 * serves the jurisdiction exemption codes. The actual burn-in happens only when
 * a release set is produced, in {@link CaseFileDownloadController}
 * ({@code exportRedacted*}). JSON is written straight to the response, mirroring
 * {@link CaseFileDownloadController}.</p>
 *
 * <p>Actions: {@code exemptions} (list codes), {@code list} (boxes for a docId),
 * {@code save} (replace a doc's boxes).</p>
 */
public class RedactionController extends SecureController {
    private static final Logger log = Logger.getLogger(RedactionController.class);

    private RedactionStore redactionStore;

    @Override
    public ModelAndView execute() {
        String action = (String) valueStack.get("action");

        if ("exemptions".equals(action)) {
            writeJson(new ExemptionProfiles().toJson());
            return null;
        }

        HttpSession session = this.request.getSession(true);
        SolrSessionObject solrSession = (SolrSessionObject)
                session.getAttribute(WebConstants.WEB_SESSION_SOLR_OBJECT);
        if (solrSession == null || solrSession.getSelectedCase() == null) {
            writeJson("{\"status\":\"error\",\"message\":\"no case selected\"}");
            return null;
        }
        Case selectedCase = solrSession.getSelectedCase();
        String filesLocation = selectedCase.getFilesLocation();
        // The redaction key is the document's uniqueId -- the stable key shared by the
        // viewer, the rendition lookup and selected-export (see docs/decisions/redaction.md).
        String docId = (String) valueStack.get("uniqueId");

        if ("list".equals(action)) {
            if (docId == null || docId.isEmpty()) {
                writeJson("{\"redactions\":[]}");
                return null;
            }
            writeJson(toJson(redactionStore.list(filesLocation, docId)));
            return null;
        }

        if ("save".equals(action)) {
            if (docId == null || docId.isEmpty()) {
                writeJson("{\"status\":\"error\",\"message\":\"missing docId\"}");
                return null;
            }
            String boxes = (String) valueStack.get("boxes");
            List<Redaction> parsed = parseBoxes(docId, boxes);
            redactionStore.replace(filesLocation, docId, parsed);
            writeJson("{\"status\":\"ok\",\"count\":" + parsed.size() + "}");
            return null;
        }

        writeJson("{\"status\":\"error\",\"message\":\"unknown action\"}");
        return null;
    }

    /**
     * Parse the viewer's save payload. Each box is
     * {@code page,x,y,w,h,exemptionCode}; boxes are joined by {@code |||}.
     * Coordinates are normalized (0..1). An empty payload clears the document.
     */
    private List<Redaction> parseBoxes(String docId, String boxes) {
        List<Redaction> result = new ArrayList<Redaction>();
        if (boxes == null || boxes.trim().isEmpty()) {
            return result;
        }
        long now = System.currentTimeMillis();
        for (String part : boxes.split("\\|\\|\\|")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            // limit 6 so a code containing commas stays intact in the last field
            String[] f = part.split(",", 6);
            if (f.length < 6) {
                continue;
            }
            try {
                double x = clamp01(Double.parseDouble(f[1]));
                double y = clamp01(Double.parseDouble(f[2]));
                double w = clamp01(Double.parseDouble(f[3]));
                double h = clamp01(Double.parseDouble(f[4]));
                int page = Integer.parseInt(f[0].trim());
                String code = f[5].trim();
                result.add(new Redaction(docId, page, x, y, w, h, code, "manual", "accepted", now));
            } catch (NumberFormatException e) {
                log.warn("Skipping malformed box: " + part);
            }
        }
        return result;
    }

    private double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private String toJson(List<Redaction> redactions) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"redactions\":[");
        for (int i = 0; i < redactions.size(); i++) {
            Redaction r = redactions.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"page\":").append(r.getPage())
              .append(",\"x\":").append(r.getX())
              .append(",\"y\":").append(r.getY())
              .append(",\"w\":").append(r.getW())
              .append(",\"h\":").append(r.getH())
              .append(",\"code\":\"").append(ExemptionProfiles.jsonEscape(r.getExemptionCode())).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private void writeJson(String json) {
        try {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(json);
            response.getWriter().flush();
        } catch (Exception e) {
            log.error("Could not write redaction JSON response", e);
        }
    }

    public void setRedactionStore(RedactionStore redactionStore) {
        this.redactionStore = redactionStore;
    }
}
