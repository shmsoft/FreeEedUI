<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>

<script src="js/jquery-fu.js"></script>
<script src="js/case.js"></script>

<h3>
    Edit Case
</h3>


<c:if test="${fn:length(errors) > 0}">
    <div class="error">
        <c:forEach var="err" items="${errors}">
            ${err} <br/>
        </c:forEach>
    </div>
</c:if>

<div class="card">
    <div class="card-body">
        <form action="usercase.html" method="post">
            <c:if test="${usercase != null}">
                <input type="hidden" name="id" value="${usercase.id}"/>
            </c:if>

            <div class="form-group">
                <label>Name*:</label>
                <input type="text" class="form-control" name="name" value="${usercase.name}"/>
            </div>

            <div class="form-group">
                <label>Description*:</label>
                <textarea name="description" class="form-control">${usercase.description}</textarea>
            </div>

            <div class="form-group">
                <label>Elastic search indices*:</label>
                <select name="esindices" class="form-control">
                    <c:forEach var="core" items="${indices}">
                        <option value="${core}" ${core == usercase.esSourceIndices ? 'selected' : ''}>${core}</option>
                    </c:forEach>
                </select>
                <span class="explanation">(For experts! Use only when you know your FreeEED project code)</span>
            </div>

            <div class="form-group">
                <label>Files location:</label>
                <input type="text" class="form-control" name="filesLocation" value="${usercase.filesLocation}"/>
            </div>

            <div class="form-group">
                <label>Upload file:</label>

                <input id="uploadfile" type="file" name="file"/>
                <input id="uploadfilebutton" type="button" value="Upload">
                <input id="uploadedFileId" type="hidden" name="filesLocationUp" value=""/>

                <span class="explanation">Please upload the native.zip file, produced by your FreeEed player application. All other types of files will be rejected.
              Please visit <a href="https://github.com/markkerzner/FreeEedUI/wiki/Quick-Start" target="_blank">our
                      Wiki</a> for more information.
              </span>

                <div class="uploadedFileBox" id="uploadedFileBoxId"
                     style="display:${usercase.uploadedFile != null ? 'block' : 'none'}">Will use: <span
                        id="uploadedFileNameId">${usercase.uploadedFile}</span></div>

            </div>

            <div class="form-group">
                <label>Load CSV file:</label>
                <div id="choosefile" style="color: #ac2925;display: none;">Choose file</div>
                <input id="uploadLoadfile" type="file" name="file">
                <input id="uploadLoadFilebutton" type="button" value="Upload">

                <div id="success" style="color: #398439;display: none;">
                    File uploaded successfully.
                </div>
                <div id="fail" style="color:#ac2925;display: none;">
                    Problem encountered while upload, please try again !
                </div>
            </div>

            <span class="explanation">(Fields marked with * are mandatory)</span>

            <div class="row">
                <div class="col-12">
                    <input type="Submit" class="btn btn-outline-success" value="Save"/>
                    <input type="hidden" name="action" value="save"/>
                </div>
            </div>

        </form>
    </div>
</div>