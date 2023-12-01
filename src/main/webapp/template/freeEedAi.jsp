<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="js/freeedai.js"></script>

<div class="your-case">
        <form name="change" method="post" action="search.html">
        <input type="hidden" name="action" value="changecase"/>

        Selected case: <select  class="form-control your-case-select" name="id">
            <c:forEach var="c" items="${cases}">
                <option value="${c.id}" ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
            </c:forEach>
        </select>
        </form>
</div>
<div>
    <input type="hidden" id="aiApiKey" value="${aiApiKey}"/>
    <input type="hidden" id="aiApiUrl" value="${aiApiUrl}"/>
    <div class="container">
        <div class="chat-wrapper">

        </div>
        <br />
        <form name="casifyAIForm" onsubmit="onSubmit(event)">
            <input type="text" class="question_input" rows="1" placeholder="Ask a question" />
            <button class="action-button" id="send_question" type="submit" class="save">Send</button>
        </form>

    </div>
</div>

