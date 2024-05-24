<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom"%>
<script>
    function disableButton(id) {
        var button = document.getElementById('processButton' + id);
        button.disabled = true;
        button.innerText = 'Cancel Processing';
    }
</script>
<div class="reg-proj-head">
    Cases
</div>
<div class="delimiter">
</div>
<div class="listusers-newuser align-center">
    <a class="action-button" href="usercase.html?action=create">Create new case</a>
</div>

<div class="listusers-box scroll">
  <table class="table-bordered" cellpadding="0" cellspacing="0">
    <tr>
        <th class="listusers-header">Edit</th>
        <th class="listusers-header">Name</th>
        <th class="listusers-header">Description</th>
        <th class="listusers-header">Status</th>
        <th class="listusers-header">Remove</th>
    </tr>
      <c:forEach var="c" items="${cases}">
          <tr>
              <td>
                  <a href="usercase.html?action=edit&id=${c.id}"><i class="bi-pencil-fill" title="Edit"></i></a>
              </td>
              <td>${c.name}</td>
              <td>${c.description}</td>
              <td>
                  <c:choose>
                      <c:when test="${c.status == 'PROCESSING_PENDING'}">
                          ${c.status} <a href="processing.html?action=process&id=${c.id}"> <button class="btn-xs btn-primary">Start Processing</button></a>
                      </c:when>
                      <c:otherwise>
                          ${c.status} <button disabled class="btn-xs btn-secondary">Processing ... </button>
                      </c:otherwise>
                  </c:choose>
              </td>
              <td>
                  <a href="usercase.html?action=delete&id=${c.id}"><i class="bi-trash-fill" title="Remove"></i></a>
              </td>
          </tr>
      </c:forEach>
  </table>
</div>