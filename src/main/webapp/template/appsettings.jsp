<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>
<script src="js/dynamic_rows.js" type="application/javascript"></script>

<div class="reg-proj-head">
    Application Settings
</div>

<div class="delimiter">
</div>

<c:if test="${fn:length(errors) > 0}">
    <div class="error">
        <c:forEach var="err" items="${errors}">
            ${err} <br/>
        </c:forEach>
    </div>
</c:if>
<c:if test="${success}">
    <div class="success">
        Your data is stored successfully!
    </div>
</c:if>

<div class="jumbotron jumbotron-fluid">
<div class="user-box">
    <form action="appsettings.html" method="post">
        <table id="settings" border="0" cellpadding="0" cellspacing="0">
            <tr>
                <td>Results per page*:</td>
                <td colspan="2"><input type="text" name="results_per_page" value="${appSettings.resultsPerPage}"/></td>
            </tr>
            <tr>
                <td>ElasticSearch endpoint URL*:</td>
                <td colspan="2"><input type="text" name="es_endpoint" value="${appSettings.esEndpoint}"/></td>
            </tr>
            <tr>
                <td>Permanent Tags:</td>
                <td colspan="2"><input type="button" class="btn btn-link" id="addnewtag" value="Add tags"/></td>
            </tr>
        </table>

        <span class="explanation">(Fields marked with * are mandatory)</span>
        <input type="Submit" class="btn btn-sm btn-primary" value="Save"/>
        <input type="hidden" name="action" value="save"/>
    </form>
</div>
</div>
<c:forEach var="savedTag" items="${appSettings.permanentTags}">
    <script>
     renderTag('${savedTag}');
    </script>
</c:forEach>