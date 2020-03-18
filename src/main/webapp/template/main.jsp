<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>

<div class="row">
    <div class="col-12 text-center">

        <img src="images/FreeEED-01.jpg" width="250"/>

        <br>

        FreeEEdReview - Document review part of the FreeEEd eDiscovery
        <br/>
        Click <a href="http://www.freeeed.org/index.php/documentation/document-review" target="_blank">here</a> for
        documentation

        <br/>
        <c:choose>
            <c:when test="${loggedVisitor == null}">
                <div class="row justify-content-center">
                    <div class="col-6">

                        <hr>
                        <h4>Login</h4>
                        <div class="card-body">
                            <c:if test="${loginError}">
                                <div class="error">
                                    Invalid username or password!
                                </div>
                            </c:if>
                            <form action="login.html" method="post">
                                <div class="form-group">
                                    <label>Username</label>
                                    <input type="text" class="form-control" value="admin" name="username"/>
                                </div>

                                <div class="form-group">
                                    <label>Password</label>
                                    <input type="password" class="form-control" value="admin" name="password"/>
                                </div>

                                <div class="row">
                                    <div class="col-12">
                                        <input type="Submit" class="btn btn-outline-success btn-sm" value="Login"/>
                                    </div>
                                </div>
                            </form>
                        </div>

                    </div>
                </div>


            </c:when>
        </c:choose>


    </div>
</div>


