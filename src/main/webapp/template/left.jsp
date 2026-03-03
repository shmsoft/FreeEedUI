<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom"%>

<c:choose>
    <c:when test="${loggedVisitor == null}">
    
  <div class="login_back">
        <div class="login-hero">
            <img src="./images/FreeEED-01.png" class="login-logo"/>
            <h1 class="login-title">FreeEED Review</h1>
            <p class="login-subtitle">Document Review &amp; eDiscovery Platform</p>
        </div>
        <div class="login_content">
            <h3 class="login-heading">Sign in</h3>
            <c:if test="${loginError}">
                <div class="login-error">Invalid username or password</div>
            </c:if>
            <form action="login.html" method="post">
                <div class="login-field">
                    <input type="text" class="login-input" value="admin" placeholder="Username" name="username"/>
                </div>
                <div class="login-field">
                    <input type="password" class="login-input" value="admin" placeholder="Password" name="password"/>
                </div>
                <input type="submit" class="login_btn" value="Sign In"/>
            </form>
        </div>
        <div class="login-footer">
            FreeEed&trade; Review V10.8.1-SNAPSHOT &mdash;
            <a href="https://github.com/shmsoft/FreeEed/wiki/Review" target="_blank">Documentation</a>
        </div>
    </div>
    </c:when>
    <c:otherwise>
    
        <ul>
            <li><a class="menulink" href="login.html"><i class="bi-house-door-fill"></i><span>Home</span></a></li>
            <li><a class="menulink" href="search.html"><i class="bi-search"></i><span>Search</span></a></li>
            <li><a class="menulink" href="freeeedai.html"><i class="bi-stars"></i><span>AI Advisor</span></a></li>
            <li><a class="menulink" href="piireport.html"><i class="bi-shield-exclamation"></i><span>PII Report</span></a></li>
            <li><a class="menulink" href="casesummary.html"><i class="bi-graph-up"></i><span>Case Summary</span></a></li>
            <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'CASES')}">
                <li><a class="menulink" href="listcases.html"><i class="bi-folder-fill"></i><span>Cases</span></a></li>
            </c:if>
            <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'APP_CONFIG')}">
                <li><a class="menulink" href="appsettings.html"><i class="bi-gear-fill"></i><span>App Settings</span></a></li>
            </c:if>
            <c:if test="${shmwebfunc:hasRight(loggedVisitor.user, 'USERS_ADMIN')}">
                <li><a class="menulink" href="listusers.html"><i class="bi-people-fill"></i><span>Users</span></a></li>
            </c:if>
            <li class="sidenav-divider"></li>
            <li><a class="menulink menulink-danger" href="logout.html"><i class="bi-box-arrow-right"></i><span>Logout</span></a></li>
        </ul>
     
    </c:otherwise>
</c:choose>