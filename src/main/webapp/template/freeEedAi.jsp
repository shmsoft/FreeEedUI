<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="js/freeedai.js"></script>

<script>
<c:forEach var="t" items="${tags}">
    allTags['${t}'] = 1;  
</c:forEach>
</script>

    <div id="html_preview_modal" class="modal fade">
      <div class="modal-dialog modal-wide">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="modal-title">Preview</h4>
          </div>
          <div class="modal-body" id="html_preview_modal_content">
            
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
          </div>
        </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
    </div>

<div class="your-case">
        <form name="change" method="post" action="search.html">
        <input type="hidden" name="action" value="changecase"/>
        Selected case: <select  class="form-control your-case-select" name="id" onchange="document.change.submit()">
            <c:forEach var="c" items="${cases}">
                <option value="${c.id}" ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
            </c:forEach>
        </select>
        </form>
</div>


