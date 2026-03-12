<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<script src="js/freeedai.js?v=21"></script>

<!-- Hidden inputs used by freeedai.js -->
<input type="hidden" id="aiApiKey"    value="${aiApiKey}" />
<input type="hidden" id="aiApiUrl"    value="${aiApiUrl}" />
<input type="hidden" id="aiProjectId" value="${selectedCase != null ? selectedCase.projectId : ''}" />

<div class="chat-page">

    <!-- ─── Topbar: title + case selector ─── -->
    <div class="chat-topbar">
        <span class="chat-topbar-title"><i class="bi-stars"></i>&nbsp;AI Advisor</span>
        <form name="change" method="post" action="freeeedai.html" class="chat-case-form">
            <input type="hidden" name="action" value="changecase" />
            <label class="chat-case-label">Case:</label>
            <select class="chat-case-select your-case-select" id="case_Select" name="id">
                <c:forEach var="c" items="${cases}">
                    <option value="${c.id}" data-project-id="${c.projectId}"
                        ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
                </c:forEach>
            </select>
            <label class="chat-all-label">
                <input id="allCasesCheckbox" type="checkbox" /> All cases
            </label>
        </form>
    </div>

    <!-- ─── Scrollable message area ─── -->
    <div class="chat-messages" id="chat-messages-area">
        <div class="chat-welcome" id="chat-welcome">
            <div class="chat-welcome-icon">✦</div>
            <h2 class="chat-welcome-title">FreeEED AI Advisor</h2>
            <p class="chat-welcome-sub">Ask questions about your legal documents, or pick a suggestion below.</p>
        </div>
        <div class="chat-wrapper"></div>
    </div>

    <!-- ─── Fixed bottom input area ─── -->
    <div class="chat-input-area">
        <div class="chat-chips" id="chat-chips">
            <button class="chat-chip" onclick="sendTemplateQuestion(event,'responsive')">📄 Responsive docs</button>
            <button class="chat-chip" onclick="sendTemplateQuestion(event,'privileged')">🔒 Privileged docs</button>
            <button class="chat-chip" onclick="sendTemplateQuestion(event,'smoking_gun')">🔫 Smoking gun docs</button>
            <button class="chat-chip" onclick="sendTemplateQuestion(event,'timeline')">📅 Timeline of events</button>
            <button class="chat-chip" onclick="sendTemplateQuestion(event,'key_parties')">👥 Key parties</button>
        </div>
        <form name="casifyAIForm" onsubmit="onSubmit(event)" class="chat-input-row">
            <select id="matterType" class="chat-matter-select" onchange="matterTypeSelect(this.value)">
                <option value="generic" selected>Generic</option>
                <option value="civil">Civil</option>
                <option value="criminal">Criminal</option>
                <option value="investigation">Investigation</option>
            </select>
            <input type="text" class="question_input chat-input" placeholder="Ask a question about your documents…" />
            <button class="chat-send-btn" id="send_question" type="submit">
                <i class="bi-send-fill"></i>
            </button>
        </form>
    </div>

</div>