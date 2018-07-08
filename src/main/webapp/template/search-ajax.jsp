<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

   <div class="delimiter2">
    </div>

    <div class="your-search">
        <div class="your-search-label">
            Your search
        </div>
        <div class="your-search-box">
            <c:forEach var="search" items="${searched}">
                <div class="your-search-box-search">
                    <div class="your-search-box-name">
                        <div class="your-search-box-name-inner ${search.highlight}" title="${search.name}">
                            ${search.name}
                        </div>
                        <div class="your-search-box-delete" title="Remove from search">
                            <img src="images/delete.gif" onclick="removeSearch(${search.id - 1})">
                        </div>
                    </div>
                </div>
            </c:forEach>
            <div class="spacer" style="clear: both;"></div>
        </div>

        <c:if test="${fn:length(searched) > 0}">
          <div class="your-search-clear-all">
              <a href="#" onclick="removeAllSearch()">Remove All</a>
          </div>
        </c:if>
    </div>

<c:choose>
  <c:when test="${result != null && result.documents != null && fn:length(result.documents) > 0}">

    <div class="delimiter2">
    </div>

    <div class="your-search">
        <div class="your-search-label">
            Results: (${result.totalSize})
        </div>

        <div class="btn-group" role="group" style="margin-top: 10px">
                    <a type="button" class="btn btn-lg btn-default" href="javascript:" class="operation-link-text" onclick="tagAllBox()">Tag All Results</a>
                    <a type="button" class="btn btn-lg btn-default" href="javascript:" class="operation-link-text" onclick="tagPageBox()">Tag This page</a>
                    <a type="button" class="btn btn-lg btn-default" href="filedownload.html?action=exportImageAll">Export as images</a>
                    <a type="button" class="btn btn-lg btn-default" href="filedownload.html?action=exportNativeAll">Export as native</a>
                    <a type="button" class="btn btn-lg btn-default" href="filedownload.html?action=exportLoadFile">Export Load File</a>
                <div class="tags-remove-box">
                </div>
            </div>

            <div id="tag-all" class="tag-box">
                Tag All Results:
                <input id="tag-all-text" type="text" name="tag"/>
                <input type="button" value="Tag" onclick="tagAll()"/>
                <input type="button" value="Cancel" onclick="document.getElementById('tag-all').style.display='none';return false;"/>
            </div>

            <div id="tag-page" class="tag-box">
                Tag This Page:
                <input id="tag-page-text" type="text" name="tag" onkeypress="newAllTagEnter(tagPage, event)"/>
                <input type="button" value="Tag" onclick="tagPage()"/>
                <input type="button" value="Cancel" onclick="document.getElementById('tag-page').style.display='none';return false;"/>
            </div>
        </div>
    </div>

    <div class="delimiter2">
    </div>

    <table border="0" cellpadding="0" cellspacing="0">
    <tr><td valign="top">

    <div class="result-list">
        <table border="0" cellpadding="0" cellspacing="0">
            <tr class="result-list-header-row">
                <th><div class="result-list-id">Id</div></th>
                <th><div class="result-list-from">From/Creator</div></th>
                <th><div class="result-list-subject">Subject/Filename</div></th>
                <th class="table-last-row"><div class="result-list-date">Date</div></th>
            </tr>

            <c:forEach var="doc" items="${result.documents}">
            <tr id="row-${doc.documentId}" class="result-list-row" onclick="selectDocument('${doc.documentId}')">
                <td><div class="result-list-id">${doc.documentId}</div></td>
                <td><div class="result-list-from">${doc.from}</div></td>
                <td><div class="result-list-subject">${doc.subject}</div></td>
                <td class="table-last-row"><div class="result-list-date">${doc.date}</div></td>
            </tr>
            </c:forEach>
        </table>
    </div>

    </td>
    <td valign="top">

    <div class="result-details">

    <c:forEach var="doc" items="${result.documents}">
         <input type="hidden" id="esid" class="esid" value="${doc.documentId}"/>
         <div class="result-box" id="doc-${doc.documentId}" style="display:none">
             <div class="btn-toolbar">
                <div class="btn-group" role="group">
                    <a type="button" class="btn btn-lg btn-default" data="${doc.documentPath}" uid="${doc.uniqueId}">Preview</a>
                    <a type="button" class="btn btn-lg btn-default" href="javascript:" onclick="$('#tag-doc-${doc.documentId}').slideToggle(500);">Tag</a>
                    <a type="button" class="btn btn-lg btn-default" href="javascript:" onclick="$('#note-doc-${doc.documentId}').slideToggle(500);">Note</a>
                    <a type="button" class="btn btn-lg btn-default" href="filedownload.html?action=exportNative&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export native</a>
                    <a type="button" class="btn btn-lg btn-default" href="filedownload.html?action=exportImage&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export image</a>
                </div>
             </div>
            <div id="tag-doc-${doc.documentId}" class="tag-box" style="box-shadow: 0 0 1px 1px #5e5e5e;">
                <div class="form-row" style="margin: 5px 20px;">
                    <div class="col">
                 <input id="tag-doc-field-${doc.documentId}" class="form-control" type="text" name="tag" onkeypress="newTagEnter('${doc.documentId}', event)"/>
                    </div>
                    <div class="col">
                 <input type="button" class="btn btn-lg btn-primary" value="Tag" onclick="newTag('${doc.documentId}')"/>
                 <input type="button" class="btn btn-lg btn-warning" value="Cancel" onclick="document.getElementById('tag-doc-${doc.documentId}').style.display='none';return false;"/>
                    </div>
                </div>
            </div>
             <div id="note-doc-${doc.documentId}" class="tag-box" style="box-shadow: 0 0 1px 1px #5e5e5e;">
                 <br/>
                 <div class="form-group" style="margin: 5px 20px;">
                    <textarea id="note-doc-field-${doc.documentId}" class="form-control" rows="3" name="tag" onkeypress="newNoteEnter('${doc.documentId}', event)"/>
                    <br/>
                    <input class="btn btn-lg btn-primary" type="button" value="Add Note" onclick="newNote('${doc.documentId}')"/>
                    <input class="btn btn-lg btn-warning" type="button" value="Cancel" onclick="document.getElementById('note-doc-${doc.documentId}').style.display='none';return false;"/>
                 </div>
             </div>
             <br/>
             <hr>
             <div class="dropdown">
                 <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenu2" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" style="width: 441px">
                         ${doc.documentId} has ${fn:length(doc.tags)} Tag<c:if test="${fn:length(dosc.tags) > 0}">s</c:if>
                 </button>
                 <div class="dropdown-menu" id="tags-box-${doc.documentId}">
                     <table id="tags-table-${doc.documentId}" border="0" cellpadding="0" cellspacing="0">
                         <c:forEach var="tag" items="${doc.tags}">
                             <input type="hidden" class="doc-tag-${doc.documentId}" value="${tag.value}"/>
                             <tr id="document-tags-row-${tag.value}-${doc.documentId}" class="document-tags-row">
                                 <td><div class="document-tags-tag">${tag.value}</div></td>
                                 <td><a href="#" onclick="deleteTag('${doc.documentId}', this, '${tag.name}')"><img src="images/delete.gif"/></a>&nbsp;&nbsp;</td>
                             </tr>
                         </c:forEach>
                     </table>
                 </div>
             </div>
             <hr>
             <div class="dropdown">
                 <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenu3" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" style="width: 441px">
                         ${doc.documentId} has ${fn:length(doc.notes)} Note<c:if test="${fn:length(dosc.notes) > 0}">s</c:if>
                 </button>
                 <div class="dropdown-menu" id="notes-box-${doc.documentId}" style="max-height: 368px;overflow-x:hidden;">
                     <table id="notes-table-${doc.documentId}" border="0" cellpadding="0" cellspacing="0">
                         <c:forEach var="note" items="${doc.notes}">
                             <tr id="document-notes-row-${doc.documentId}" class="document-tags-row">
                                 <td>
                                     <div class="card" style="width: 439px">
                                         <div class="card-body">
                                             <blockquote class="blockquote mb-0">
                                                 <p>${note.value}</p>
                                                 <footer class="blockquote-footer">by <cite title="Source Title">${note.author}</cite> on ${note.addedOn}</footer>
                                             </blockquote>
                                         </div>
                                         <input class="btn btn-lg btn-danger" style="float: right" type="button" value="Delete Note" onclick="deleteNote('${doc.documentId}', this, '${note.id}')">
                                     </div>
                                 </td>
                             </tr>
                         </c:forEach>
                     </table>
                 </div>
             </div>
             <hr>
             <div class="alert alert-dark" role="alert">
                 Document Details
             </div>
            <table id="result-table" class="table table-striped table-bordered" style="margin: 11px 10px 0px -5px">
                <c:forEach var="entry" items="${doc.entries}">
                    <tr>
                      <c:choose>
                        <c:when test="${entry.key != 'text'}">
                          <td class="result-box-key">${entry.key}</td>
                          <td><div class="result-box-value">${entry.value}</div></td>
                        </c:when>
                        <c:otherwise>
                            <td colspan=2 class="result-box-text">
                              <c:choose>
                                <c:when test="${fn:length(entry.value) > 300}">
                                  <div id="textid-txt-${doc.documentId}" class="result-box-text-container">
                                    ${entry.value}
                                  </div>
                                  <div id="textid-coll-${doc.documentId}" class="result-box-text-collapse">
                                    The text has been truncated. Click <a href="#" onclick="document.getElementById('textid-coll-${doc.documentId}').style.display='none';document.getElementById('textid-txt-${doc.documentId}').className=' ';return false;">here</a> to see it complete.
                                  </div>
                                </c:when>
                                <c:otherwise>
                                  <div>
                                    ${entry.value}
                                  </div>
                                </c:otherwise>
                              </c:choose>
                            </td>
                        </c:otherwise>
                      </c:choose>
                    </tr>
                </c:forEach>
            </table>
        </div>
    </c:forEach>
    </div>

    </td></tr>
    </table>
    <div class="pagination">
        <c:if test="${showPagination}">
            <div class="prev_page">
                <c:if test="${showPrev}">
                    <a href="#" onclick="changePage(${currentPage - 1})"> Prev </a>
                </c:if>
            </div>
            <div class="page">
                ${currentPage}
            </div>
            <div class="next_page">
                <c:if test="${showNext}">
                    <a href="#" onclick="changePage(${currentPage + 1})"> Next </a>
                </c:if>
            </div>
        </c:if>
    </div>
  </c:when>
  <c:otherwise>
    <div class="delimiter2">
    </div>

    <div class="no-result">
        No result
    </div>
    <div class="delimiter3">
    </div>
  </c:otherwise>
</c:choose>