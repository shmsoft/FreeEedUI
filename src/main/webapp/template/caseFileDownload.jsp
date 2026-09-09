<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:choose>
  <%-- A specific explanation, when the controller has one. The generic message
       below blames the 'Files location' setting, which is wrong and misleading
       for cases that simply have no PDF renditions. --%>
  <c:when test="${not empty errorMessage}">
    <div class="error">${errorMessage}</div>
  </c:when>
  <c:when test="${error}">
    Data do not exist! Please set the 'Files location' property in Case settings page.
  </c:when>
  <c:otherwise>
    Download should start soon...
  </c:otherwise>
</c:choose>

<div class="delimiter3">
</div>