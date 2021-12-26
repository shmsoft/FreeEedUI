<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script src="js/search.js"></script>

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

<div class="delimiter2">
</div>

<div class="your-case">
    <div class="your-search-label">
        Your case
    </div>
    <div class="your-search-box">
        <form name="change" method="post" action="search.html">
        <input type="hidden" name="action" value="changecase"/>
        Selected case: <select class="your-case-select" name="id" onchange="document.change.submit()">
            <c:forEach var="c" items="${cases}">
                <option value="${c.id}" ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
            </c:forEach>
        </select>
        </form>
    </div>
</div>

<div class="delimiter2">
</div>

<div class="your-search">
    <div class="your-search-label">
       Search <a href="https://github.com/shmsoft/FreeEedUI/wiki/Search" target="_blank">(Help)</a>
    </div>
    <div class="your-search-box">
        <div class="search-box">
            <input id="search-query" class="search-field" type="text" name="query" value=""/>
            <input type="button" name="Search" value="Search" onclick="search()"/>
        </div>
        
        <div class="case-tags-box">
            <div class="case-tags-box-label">Search by tags</div>
            <div class="case-tags-box-body"></div>
        </div>
        
        <div style="clear:both;"></div>
    </div>
</div>

<div id="result-ajax">
    <div class="delimiter3">
    </div>
</div>