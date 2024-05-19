<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="shmwebfunc" uri="http://freeeed.org/tags/custom"%>

<script src="js/jquery-fu.js"></script>
<script src="js/case.js"></script>
<script>
      $(document).ready(function(){
          let fileInput = document.getElementById("fileInput");
          let directoryInput = document.getElementById("directoryInput");

          fileInput.addEventListener("change", () => {
              let inputFile = fileInput.files[0];
              if (inputFile) {
                  document.getElementById("pathDisplay").value = inputFile.name;
              }
          });

          directoryInput.addEventListener("change", () => {
              let inputDir = directoryInput.files[0];
              if (inputDir) {
                  let directoryPath = inputDir.webkitRelativePath.split('/')[0];
                  document.getElementById("pathDisplay").value = directoryPath;
              }
          });
      });
</script>
<div class="reg-proj-head">
    Create Case
</div>

<div class="delimiter">
</div>

<div class="user-box">
  <form action="usercase.html" method="post">

    <table class="case-form-table" cellpadding="10" cellspacing="0">
        <tr>
            <td class="table-label">Name<span class="required">*</span>:</td>
            <td><input type="text" class="form-control" name="name" placeholder="Project Name"/></td>
          </tr>
          <tr>
            <td>Description<span class="required">*</span>: </td>
            <td><textarea class="form-control" name="description" placeholder="Project Descrition"></textarea></td>
          <tr>
            <td>Files location: </td>
            <td>
                <input type="file" id="fileInput" class="form-control" style="visibility: hidden;" />
                <input type="file" id="directoryInput" class="form-control" webkitdirectory style="visibility: hidden;" />
                <input type="text" id="pathDisplay" class="form-control" />
            </td>
          </tr>
        <tr>
            <td></td>
            <td>
            <button type="button" class="action-button-secondary" onclick="document.getElementById('fileInput').click();">Browse File</button>
            <button type="button" class="action-button-secondary" onclick="document.getElementById('directoryInput').click();">Browse Directory</button>
            </td>
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