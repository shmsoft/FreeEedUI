<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom" %>

<script src="js/jquery-fu.js"></script>
<script src="js/case.js"></script>

<div class="reg-proj-head">
    Edit Case
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

<div class="user-box">
    <form action="usercase.html" method="post">
        <c:if test="${usercase != null}">
            <input type="hidden" name="id" value="${usercase.id}"/>
        </c:if>
        <table border="0" cellpadding="0" cellspacing="0">
            <tr>
                <td>Name*:</td>
                <td><input type="text" name="name" value="${usercase.name}"/></td>
            </tr>
            <tr>
                <td>Description*:</td>
                <td><textarea name="description">${usercase.description}</textarea></td>
            </tr>
            <tr>
                <td>Elastic search indices*:</td>
                <td>
                    <select name="esindices">
                        <c:forEach var="core" items="${indices}">
                            <option value="${core}" ${core == usercase.esSourceIndices ? 'selected' : ''}>${core}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <span class="explanation">(For experts! Use only when you know your SHMcloud project code)</span>
                </td>
            </tr>
            <tr>
                <td>Files location:</td>
                <td><input type="text" name="filesLocation" value="${usercase.filesLocation}"/></td>
            </tr>
            <tr>
                <td>Upload file:</td>
                <td>
                    <input id="uploadfile" type="file" name="file"/>
                    <input id="uploadfilebutton" type="button" value="Upload">
                    <input id="uploadedFileId" type="hidden" name="filesLocationUp" value=""/>
                </td>
            </tr>
            <tr>
                <td colspan="2">
              <span class="explanation">Please upload the native.zip file, produced by your FreeEed player application. All other types of files will be rejected.
              Please visit <a href="https://github.com/markkerzner/FreeEedUI/wiki/Quick-Start" target="_blank">our
                      Wiki</a> for more information.
              </span>
                </td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td>
                    <div class="uploadedFileBox" id="uploadedFileBoxId"
                         style="display:${usercase.uploadedFile != null ? 'block' : 'none'}">Will use: <span
                            id="uploadedFileNameId">${usercase.uploadedFile}</span></div>
                </td>
            </tr>
            <tr>
                <td>Load CSV file:</td>
                <td>
                    <div id="choosefile" style="color: #ac2925;display: none;">Choose file</div>
                    <input id="uploadLoadfile" type="file" name="file">
                    <input id="uploadLoadFilebutton" type="button" value="Upload">
                </td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td>
                    <div id="success" style="color: #398439;display: none;">
                        File uploaded successfully.
                    </div>
                    <div id="fail" style="color:#ac2925;display: none;">
                        Problem encountered while upload, please try again !
                    </div>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    &nbsp;
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <span class="explanation">(Fields marked with * are mandatory)</span>
                </td>
            </tr>
            <tr>
                <td colspan="2">
                    <input type="Submit" value="Save"/>
                </td>
            </tr>
        </table>
        <input type="hidden" name="action" value="save"/>
    </form>
</div>