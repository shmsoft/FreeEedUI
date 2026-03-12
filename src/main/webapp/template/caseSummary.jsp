<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="js/freeedai.js?v=21"></script>
<!-- AI API URL used by generateCaseSummary() in freeedai.js -->
<input type="hidden" id="cs-ai-url" value="${aiApiUrl}" />

<div class="cs-page">

    <!-- ─── Page header ─── -->
    <div class="cs-header">
        <div class="cs-header-icon">🧠</div>
        <div class="cs-header-text">
            <h2 class="cs-title">Case Intelligence Summary</h2>
            <p class="cs-subtitle">AI-powered deep analysis across 8 dimensions: actors &amp; identity, relationships, groups &amp; structure, timeline, topics &amp; intent, behavior anomalies, evidence confidence, and review actions.</p>
        </div>
    </div>

    <!-- ─── Controls bar ─── -->
    <div class="cs-controls">
        <div class="cs-ctrl-group">
            <label class="cs-ctrl-label">Select Case</label>
            <select class="cs-case-select" id="cs-case-select">
                <c:forEach var="c" items="${cases}">
                    <option value="${c.id}"
                            data-project-id="${c.projectId}"
                            data-name="${c.name}"
                        ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
                </c:forEach>
            </select>
        </div>
        <button class="cs-analyze-btn" id="cs-analyze-btn" onclick="generateCaseSummary()">
            <i class="bi-cpu"></i>&nbsp;Analyze Case
        </button>
    </div>

    <!-- ─── Inline report area ─── -->
    <div class="cs-report-area" id="cs-report-area">
        <div class="cs-placeholder">
            <div class="cs-placeholder-icon">🧠</div>
            <div class="cs-placeholder-msg">
                Select a case and click <strong>Analyze Case</strong> to generate a
                comprehensive intelligence summary across all 8 analytical dimensions.
            </div>
            <div class="cs-placeholder-badges">
                <span class="cs-badge">👥 Actors &amp; Identity</span>
                <span class="cs-badge">🔗 Relationships</span>
                <span class="cs-badge">🏢 Groups &amp; Structure</span>
                <span class="cs-badge">📅 Time &amp; Change</span>
                <span class="cs-badge">💡 Topics &amp; Intent</span>
                <span class="cs-badge">⚠️ Anomalies</span>
                <span class="cs-badge">🔬 Evidence &amp; Confidence</span>
                <span class="cs-badge">🎯 Review &amp; Actions</span>
            </div>
        </div>
    </div>

</div>
