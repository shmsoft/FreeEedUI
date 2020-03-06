<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>

<h3>
    Cases
    <a class="btn btn-outline-success btn-sm" href="usercase.html">Create new case</a>
</h3>

<div class="card mb-3">
    <table class="table table-hover table-striped">
        <thead class="thead-dark">
        <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Remove</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="c" items="${cases}">
            <tr>
                <td>${c.name}</td>
                <td>${c.description}</td>
                <td>
                    <a href="usercase.html?action=edit&id=${c.id}"><img src="images/edit.png" title="Edit"/></a>
                    <a href="usercase.html?action=delete&id=${c.id}">
                        <img src="images/delete.gif" title="Remove"/>

                    </a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>