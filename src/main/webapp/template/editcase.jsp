<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom"%>

<script src="js/jquery-fu.js"></script>
<script src="js/case.js"></script>
<div class="reg-proj-head">
    Edit Case
</div>

<div class="delimiter">
</div>

<div class="user-box">
    <form action="usercase.html" method="post">
        <c:if test="${usercase != null}">
            <input type="hidden" name="id" value="${usercase.id}"/>
        </c:if>
        <input type="hidden" name="projectId" value="${usercase.projectId}"/>
        <table class="case-form-table" cellpadding="10" cellspacing="0">
            <tr>
                <td class="table-label">Name<span class="required">*</span>:</td>
                <td><input type="text" class="form-control" name="name" value="${usercase.name}"/></td>
            </tr>
            <tr>
                <td>Description<span class="required">*</span>: </td>
                <td><textarea class="form-control" name="description">${usercase.description}</textarea></td>
            </tr>
            <tr>
                <td>Files location: </td>
                <td><input type="text" class="form-control" name="filesLocation" value="${usercase.uploadedFile}"/></td>
            </tr>

            <tr>
                <td colspan="2">
                    <span class="explanation">(Fields marked with <span class="required">*</span> are mandatory)</span>
                </td>
            </tr>
            <tr>
                <td colspan="2" class="text-center">
                    <input type="Submit" class="action-button" value="Save"/>
                </td>
            </tr>
            <tr>
                <td  colspan="2"  class="align-center">
                    <c:if test="${fn:length(errors) > 0}">
                        <div class="error">
                            <c:forEach var="err" items="${errors}">
                                ${err} <br/>
                            </c:forEach>
                        </div>
                    </c:if>
                </td>
            </tr>
        </table>
        <input type="hidden" name="action" value="save"/>
    </form>
</div>