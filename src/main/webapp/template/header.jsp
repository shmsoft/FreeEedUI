<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<button class="menu_button" aria-label="Toggle menu"><i class="bi-list"></i></button>
<a href="login.html" class="brand-link">
    <img src="./images/FreeEedLogowhite.png" class="brand-logo" width="128" />
    <span class="brand-name"><span class="brand-accent"> Review</span></span>
</a>

<div class="topbar-center">
    <c:if test="${loggedVisitor != null}">
        <form name="headerCaseForm" method="post" action="search.html" class="topbar-case-form">
            <input type="hidden" name="action" value="changecase"/>
            <span class="topbar-case-label">Case:</span>
            <select class="topbar-case-select" id="header_case_select" name="id" onchange="document.headerCaseForm.submit()">
                <c:forEach var="c" items="${cases}">
                    <option value="${c.id}" ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
                </c:forEach>
            </select>
        </form>
        <div class="topbar-search-wrap">
            <i class="bi-search topbar-search-icon"></i>
            <input type="text" class="topbar-search-input" id="topbar-search-query" placeholder="Search documents, emails, text, metadata..." onkeypress="if(event.keyCode==13){document.getElementById('search-query').value=this.value;search();}" />
        </div>
    </c:if>
</div>

<div class="topbar-actions">
    <c:if test="${loggedVisitor != null}">
        <a href="search.html" class="topbar-action-link"><i class="bi-search"></i> Advanced Search</a>
        <a href="filedownload.html?action=exportReport" class="topbar-action-link" target="_blank"><i class="bi-download"></i> Export</a>
        <a href="freeeedai.html" class="topbar-action-link"><i class="bi-stars"></i> AI Advisor</a>
        <div class="topbar-avatar" title="${loggedVisitor.user.firstName} ${loggedVisitor.user.lastName}">
            <c:choose>
                <c:when test="${loggedVisitor.user.firstName != null}">
                    ${loggedVisitor.user.firstName.substring(0,1)}${loggedVisitor.user.lastName != null ? loggedVisitor.user.lastName.substring(0,1) : ''}
                </c:when>
                <c:otherwise>U</c:otherwise>
            </c:choose>
        </div>
    </c:if>
</div>
<span class="topbar-version">v10.8</span>