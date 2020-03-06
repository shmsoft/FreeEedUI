<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<script>
    <c:forEach var="t" items="${tags}">
    allTags['${t}'] = 1;
    </c:forEach>

    var permanentTags = [];
</script>
<c:forEach var="savedTag" items="${permanentTags}">
    <script>
        permanentTags.push("${savedTag}");
    </script>
</c:forEach>
<script>
    $('[id*="tag-doc-"]').autocomplete({source: permanentTags});
</script>


<div class="row">
    <div class="col-3">
        <div class="card mb-3">
            <div class="card-header">
                Search ${activeCase}
            </div>
            <div class="card-body">
                <div class="form-group">
                    <input id="search-query" class="form-control" type="text" name="query" value=""/>
                </div>
                <div class="row">
                    <div class="col-12 text-right">
                        <input type="button" class="btn btn-outline-success btn-sm" name="Search" value="Search"
                               onclick="search()"/>
                    </div>
                </div>
            </div>
        </div>
        <div class="card mb-3 d-none">
            <div class="card-header">
                Search by tags
            </div>
            <div class="card-body">
                <div class="case-tags-box-label"></div>
                <div class="case-tags-box-body"></div>
                <div style="clear:both;"></div>
            </div>
        </div>
        <div class="card mb-3">
            <div class="card-header">
                Your search
            </div>
            <div class="card-body p-0">

                <ul class="list-group">
                    <c:forEach var="search" items="${searched}">
                        <li class="list-group-item d-flex justify-content-between align-items-center ${search.highlight}">
                                ${search.name}
                            <img src="images/delete.gif" onclick="removeSearch(${search.id - 1})">
                        </li>
                    </c:forEach>
                </ul>

                <div class="p-3">
                    <div class="row">
                        <div class="col-12 text-right">
                            <c:if test="${fn:length(searched) > 0}">
                                <a href="#" class="btn btn-outline-danger btn-sm " onclick="removeAllSearch()">Remove
                                    All</a>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="col-9">
        <c:choose>
            <c:when test="${result != null && result.documents != null && fn:length(result.documents) > 0}">
                <div class="row mb-3">
                    <div class="col-10">
                        <div class="card">
                            <div class="card-body p-0">
                                <div class="result-list">
                                    <table class="table table-hover">
                                        <thead class="thead-dark">
                                        <tr>
                                            <th>
                                                <div class="result-list-id">Id</div>
                                            </th>
                                            <th>
                                                <div class="result-list-from">From/Creator</div>
                                            </th>
                                            <th>
                                                <div class="result-list-subject">Subject/Filename</div>
                                            </th>
                                            <th class="table-last-row">
                                                <div class="result-list-date">Date</div>
                                            </th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <c:forEach var="doc" items="${result.documents}">
                                            <tr id="row-${doc.documentId}" class="result-list-row"
                                                onclick="selectDocument('${doc.documentId}')">
                                                <td>
                                                    <div class="result-list-id">${doc.documentId}</div>
                                                </td>
                                                <td>
                                                    <div class="result-list-from">${doc.from}</div>
                                                </td>
                                                <td>
                                                    <div class="result-list-subject">${doc.subject}</div>
                                                </td>
                                                <td class="table-last-row">
                                                    <div class="result-list-date">${doc.date}</div>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        </tbody>
                                    </table>
                                </div>

                            </div>
                        </div>
                    </div>
                    <div class="col-2">
                        <div class="btn-group-sm btn-group-vertical" role="group">
                            <a type="button" class="btn btn-outline-info" href="javascript:" onclick="tagAllBox()">
                                Tag All Results
                            </a>
                            <a type="button" class="btn btn-outline-info" href="javascript:" onclick="tagPageBox()">
                                Tag This page
                            </a>
                            <a type="button" class="btn btn-outline-info" href="filedownload.html?action=exportImageAll">
                                Export as images
                            </a>
                            <a type="button" class="btn btn-outline-info" href="filedownload.html?action=exportNativeAll">
                                Export as native
                            </a>
                            <a type="button" class="btn btn-outline-info" href="filedownload.html?action=exportLoadFile">
                                Export Load File
                            </a>
                        </div>
                        <div id="tag-all" class="tag-box">
                            <div class="card">
                                <div class="card-body">
                                    Tag All Results:
                                    <input id="tag-all-text" type="text" name="tag"/>
                                    <input type="button" value="Tag" onclick="tagAll()"/>
                                    <input type="button" value="Cancel"
                                           onclick="document.getElementById('tag-all').style.display='none';return false;"/>
                                </div>
                            </div>
                        </div>
                        <div id="tag-page" class="tag-box">
                            <div class="card">
                                <div class="card-body">
                                    Tag This Page:
                                    <input id="tag-page-text" type="text" name="tag"
                                           onkeypress="newAllTagEnter(tagPage, event)"/>
                                    <input type="button" value="Tag" onclick="tagPage()"/>
                                    <input type="button" value="Cancel"
                                           onclick="document.getElementById('tag-page').style.display='none';return false;"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="card mb-3">
                    <div class="card-body">
                        <div class="result-details">

                            <c:forEach var="doc" items="${result.documents}">
                                <input type="hidden" id="esid" class="esid" value="${doc.documentId}"/>
                                <div class="result-box" id="doc-${doc.documentId}" style="display:none">
                                    <div class="btn-toolbar">
                                        <div class="btn-group-sm btn-group" role="group">
                                            <a class="btn btn-outline-info html-preview" href="#"
                                               data="${doc.documentPath}"
                                               uid="${doc.uniqueId}">Preview</a>
                                            <a class="btn btn-outline-info" href="javascript:"
                                               onclick="$('#tag-doc-${doc.documentId}').slideToggle(500);">Tag</a>
                                            <a class="btn btn-outline-info" href="javascript:"
                                               onclick="$('#note-doc-${doc.documentId}').slideToggle(500);">Note</a>
                                            <a class="btn btn-outline-info"
                                               href="filedownload.html?action=exportNative&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export
                                                native</a>
                                            <a class="btn btn-outline-info"
                                               href="filedownload.html?action=exportImage&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export
                                                image</a>
                                        </div>
                                    </div>
                                    <div id="tag-doc-${doc.documentId}" class="tag-box">
                                        <div class="card">
                                            <div class="card-body">
                                                <div class="form-group">
                                                    <input id="tag-doc-field-${doc.documentId}" class="form-control"
                                                           type="text"
                                                           name="tag"
                                                           onkeypress="newTagEnter('${doc.documentId}', event)"/>
                                                </div>
                                                <div class="row">
                                                    <div class="col-12">
                                                        <input type="button" class="btn btn-info btn-sm" value="Tag"
                                                               onclick="newTag('${doc.documentId}')"/>

                                                        <input type="button" class="btn btn-danger btn-sm"
                                                               value="Cancel"
                                                               onclick="document.getElementById('tag-doc-${doc.documentId}').style.display='none';return false;"/>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>


                                    </div>
                                    <div id="note-doc-${doc.documentId}" class="tag-box">

                                        <div class="card">
                                            <div class="card-body">
                                                <div class="form-group">
                                            <textarea id="note-doc-field-${doc.documentId}" class="form-control"
                                                      rows="3"
                                                      name="tag"
                                                      onkeypress="newNoteEnter('${doc.documentId}', event)"></textarea>
                                                </div>

                                                <div class="row">
                                                    <div class="col-12">
                                                        <input class="btn btn-info btn-sm" type="button"
                                                               value="Add Note"
                                                               onclick="newNote('${doc.documentId}')"/>
                                                        <input class="btn btn-danger btn-sm" type="button"
                                                               value="Cancel"
                                                               onclick="document.getElementById('note-doc-${doc.documentId}').style.display='none';return false;"/>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                    </div>
                                    <hr>
                                    <div class="row">
                                        <div class="col">
                                            <div class="dropdown">
                                                <button class="btn btn-secondary dropdown-toggle" type="button"
                                                        id="dropdownMenu2"
                                                        data-toggle="dropdown" aria-haspopup="true"
                                                        aria-expanded="false"
                                                        style="width: 441px">
                                                        ${doc.documentId} has ${fn:length(doc.tags)} Tag<c:if
                                                        test="${fn:length(dosc.tags) > 0}">s</c:if>
                                                </button>
                                                <div class="dropdown-menu" id="tags-box-${doc.documentId}">
                                                    <table id="tags-table-${doc.documentId}" border="0" cellpadding="0"
                                                           cellspacing="0">
                                                        <c:forEach var="tag" items="${doc.tags}">
                                                            <input type="hidden" class="doc-tag-${doc.documentId}"
                                                                   value="${tag.value}"/>
                                                            <tr id="document-tags-row-${tag.value}-${doc.documentId}"
                                                                class="document-tags-row">
                                                                <td>
                                                                    <div class="document-tags-tag">${tag.value}</div>
                                                                </td>
                                                                <td><a href="#"
                                                                       onclick="deleteTag('${doc.documentId}', this, '${tag.name}')"><img
                                                                        src="images/delete.gif"/></a>&nbsp;&nbsp;
                                                                </td>
                                                            </tr>
                                                        </c:forEach>
                                                    </table>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col">
                                            <div class="dropdown">
                                                <button class="btn btn-secondary dropdown-toggle" type="button"
                                                        id="dropdownMenu3"
                                                        data-toggle="dropdown" aria-haspopup="true"
                                                        aria-expanded="false"
                                                        style="width: 441px">
                                                        ${doc.documentId} has ${fn:length(doc.notes)} Note<c:if
                                                        test="${fn:length(dosc.notes) > 0}">s</c:if>
                                                </button>
                                                <div class="dropdown-menu" id="notes-box-${doc.documentId}"
                                                     style="max-height: 368px;overflow-x:hidden;">
                                                    <table id="notes-table-${doc.documentId}" border="0" cellpadding="0"
                                                           cellspacing="0">
                                                        <c:forEach var="note" items="${doc.notes}">
                                                            <tr id="document-notes-row-${doc.documentId}"
                                                                class="document-tags-row">
                                                                <td>
                                                                    <div class="card" style="width: 439px">
                                                                        <div class="card-body">
                                                                            <blockquote class="blockquote mb-0">
                                                                                <p>${note.value}</p>
                                                                                <footer class="blockquote-footer">by
                                                                                    <cite
                                                                                            title="Source Title">${note.author}</cite>
                                                                                    on ${note.addedOn}</footer>
                                                                            </blockquote>
                                                                            <input class="btn btn-outline-danger"
                                                                                   style="float: right"
                                                                                   type="button" value="Delete Note"
                                                                                   onclick="deleteNote('${doc.documentId}', this, '${note.id}')">
                                                                        </div>
                                                                    </div>
                                                                </td>
                                                            </tr>
                                                        </c:forEach>
                                                    </table>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <hr>
                                    <h5>Document Details</h5>
                                    <table id="result-table" class="table table-striped table-bordered"
                                           style="margin: 11px 10px 0px -5px">
                                        <c:forEach var="entry" items="${doc.entries}">
                                            <c:choose>
                                                <c:when test="${entry.key != 'text' && entry.key != 'notes'}">
                                                    <tr>
                                                        <td class="result-box-key">${entry.key}</td>
                                                        <td>
                                                            <div class="result-box-value">${entry.value}</div>
                                                        </td>
                                                    </tr>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:choose>
                                                        <c:when test="${entry.key != 'notes'}">
                                                            <tr>
                                                                <td colspan=2 class="result-box-text">
                                                                    <c:choose>
                                                                        <c:when test="${fn:length(entry.value) > 400}">
                                                                            <div id="textid-txt-${doc.documentId}"
                                                                                 class="result-box-text-container">
                                                                                    ${entry.value}
                                                                            </div>
                                                                            <div id="textid-coll-${doc.documentId}"
                                                                                 class="result-box-text-collapse">
                                                                                The text has been truncated. Click <a
                                                                                    href="#"
                                                                                    onclick="document.getElementById('textid-coll-${doc.documentId}').style.display='none';document.getElementById('textid-txt-${doc.documentId}').className=' ';return false;">here</a>
                                                                                to see it complete.
                                                                            </div>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <div>
                                                                                    ${entry.value}
                                                                            </div>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </td>
                                                            </tr>
                                                        </c:when>
                                                    </c:choose>
                                                </c:otherwise>
                                            </c:choose>

                                        </c:forEach>
                                    </table>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
                <div class="row">
                    <div class="col-12">
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
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="no-result"> No result</div>
            </c:otherwise>
        </c:choose>

    </div>
</div>
