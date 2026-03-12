<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script src="js/freeedai.js?v=21"></script>

<div class="pii-page">

    <!-- ─── Page header ─── -->
    <div class="pii-page-header">
        <span class="pii-page-icon">🔍</span>
        <div>
            <h2 class="pii-page-title">PII Intelligence Report</h2>
            <p class="pii-page-subtitle">Detect and analyze Personally Identifiable Information across case documents using AI.</p>
        </div>
    </div>

    <!-- ─── Controls bar: case selector + generate button ─── -->
    <div class="pii-controls-bar">
        <div class="pii-ctrl-group">
            <label class="pii-ctrl-label">Select Case</label>
            <select class="pii-case-select" id="pii-page-case-select">
                <c:forEach var="c" items="${cases}">
                    <option value="${c.id}"
                            data-project-id="${c.projectId}"
                            data-name="${c.name}"
                        ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
                </c:forEach>
            </select>
        </div>
        <button class="pii-gen-btn" id="pii-gen-btn" onclick="generatePiiReport()">
            <i class="bi-shield-shaded"></i>&nbsp;Generate Report
        </button>
    </div>

    <!-- ─── Inline report area ─── -->
    <div class="pii-report-area" id="pii-report-area">
        <div class="pii-area-placeholder">
            <div class="pii-area-icon">🛡️</div>
            <div class="pii-area-msg">
                Select a case and click <strong>Generate Report</strong> to scan for PII entities
            </div>
        </div>
    </div>

</div>
