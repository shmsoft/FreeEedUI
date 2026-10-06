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
package org.freeeed.search.web.model.redaction;

/**
 * A single manual redaction: a rectangle on one page of a document's PDF
 * rendition, with the legal exemption that justifies withholding it.
 *
 * <p>Per {@code docs/decisions/redaction.md}: a redaction is a <b>separate,
 * per-case annotation layer</b> -- the original/rendition files are never
 * modified. The box is only burned into a <i>copy</i> when a release set is
 * produced ({@code exportRedacted*}). Coordinates are stored <b>normalized</b>
 * (0..1, top-left origin) relative to the page, so they survive re-rasterization
 * at any export DPI.</p>
 *
 * <p>This MVP ships the manual box; {@code source} is kept so the later
 * pattern-rule / name-list paths (same decision record) can mark their origin
 * without a data-model change.</p>
 */
public class Redaction {

    /** Solr document id (== the id the review viewer uses for the open doc). */
    private String docId;
    /** 1-based page number within the document's PDF rendition. */
    private int page;
    /** Normalized box, top-left origin, each in [0,1]. */
    private double x;
    private double y;
    private double w;
    private double h;
    /** Exemption code printed on the box and in the log, e.g. "(b)(6)". */
    private String exemptionCode;
    /** manual | pattern:<rule> | list:<name>  (MVP writes "manual"). */
    private String source = "manual";
    /** proposed | accepted | rejected  (MVP writes "accepted"). */
    private String status = "accepted";
    /** Epoch millis the box was created. */
    private long createdAt;

    public Redaction() {
    }

    public Redaction(String docId, int page, double x, double y, double w, double h,
                     String exemptionCode, String source, String status, long createdAt) {
        this.docId = docId;
        this.page = page;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.exemptionCode = exemptionCode;
        this.source = source;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getW() { return w; }
    public void setW(double w) { this.w = w; }

    public double getH() { return h; }
    public void setH(double h) { this.h = h; }

    public String getExemptionCode() { return exemptionCode; }
    public void setExemptionCode(String exemptionCode) { this.exemptionCode = exemptionCode; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
