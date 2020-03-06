<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>

<nav class="navbar navbar-expand-lg navbar-dark bg-scaia mb-3 text-white">
    <a class="navbar-brand" href="main.html">FreeEed&trade; Review V8.0.0</a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
            aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <c:choose>
            <c:when test="${loggedVisitor != null}">
                <ul class="navbar-nav mr-auto">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" data-toggle="dropdown"> Search </a>
                        <div class="dropdown-menu" aria-labelledby="navbarDropdown">
                            <c:forEach var="c" items="${cases}">
                                <a class="dropdown-item" href="search.html?id=${c.id}">${c.name}</a>
                            </c:forEach>
                        </div>
                    </li>
                    <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'CASES')}">
                        <li class="nav-item"><a class="nav-link" href="listcases.html"> Cases </a></li>
                    </c:if>
                    <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'APP_CONFIG')}">
                        <li class="nav-item"><a class="nav-link" href="appsettings.html"> Application settings </a></li>
                    </c:if>
                    <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'USERS_ADMIN')}">
                        <li class="nav-item"><a class="nav-link" href="listusers.html"> User Administration </a></li>
                    </c:if>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                    <li class="nav-item"><a class="nav-link" href="logout.html"> Logout </a></li>
                </ul>
            </c:when>
        </c:choose>
    </div>
</nav>