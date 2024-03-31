<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="js/freeedai.js"></script>

<div class="your-case">
        <form name="change" method="post" action="search.html">
        <input type="hidden" name="action" value="changecase"/>

        Selected case:
            <div>
                <input id="allCasesCheckbox" type="checkbox" /> All Cases
                <select  class="form-control your-case-select" id="case_Select" name="id">
                    <c:forEach var="c" items="${cases}">
                        <option value="${c.projectId}" ${(selectedCase != null && selectedCase.projectId == c.projectId) ? 'selected' : ''}>${c.name}</option>
                    </c:forEach>
                </select>
            </div>

        </form>
</div>
<div>
    <input type="hidden" id="aiApiKey" value="${aiApiKey}"/>
    <input type="hidden" id="aiApiUrl" value="${aiApiUrl}"/>
    <div class="container">
        <div class="chat-wrapper">

        </div>
        <br />
        <div class="edis-pred-container">
            <button class="edis-pred-button" onclick="sendQuestion(event)" id="responsive_question">Responsive documents</button>
            <button class="edis-pred-button" onclick="sendQuestion(event)" id="privileged_question">Privileged documents</button>
            <button class="edis-pred-button" onclick="sendQuestion(event)" id="smoking_gun_question">Smoking gun documents</button>
            <button class="edis-pred-button" onclick="sendQuestion(event)" id="pii_question">PII report</button>
        </div>
        <form name="casifyAIForm" onsubmit="onSubmit(event)">
            <div>
                <select id="matterType" class="question_matter_type" onchange="matterTypeSelect(this.value)">
                    <option value="generic" selected="selected">Generic</option>
                    <option value="civil" >Civil</option>
                    <option value="criminal" >Criminal</option>
                    <option value="investigation" >Investigation</option>
                </select>
            </div>
            <input type="text" class="question_input" rows="1" placeholder="Ask a question" />
            <button class="action-button" id="send_question" type="submit" class="save">Send</button>
        </form>

    </div>
</div>

