<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script>
    var currentPage = ${currentPage};
    var totalPage = ${totalPage};
    var showPrev = ${showPrev};
    var showNext = ${showNext};
    var currentSortField = '${sortField}';
    var currentSortDir = '${sortDir}';
    var pageSize = ${pageSize};
    var caseView = ${caseView == true};
    var caseViewAnchorId = "${caseViewAnchorId}";
    var documents = [];
    <c:forEach var="doc" items="${result.documents}">
    documents.push({
        documentPath: "${doc.documentPath}",
        documentName: "${doc.documentName}",
        documentId: "${doc.documentId}",
        uniqueId: "${doc.uniqueId}"
    });
    </c:forEach>
</script>

<!-- Active Search Keywords -->
<div class="search-keywords-bar">
    <c:set var="hasVisibleSearches" value="false"/>
    <c:forEach var="search" items="${searched}">
        <c:if test="${search.name != 'Keyword: *' && search.name != 'Keyword: *:*'}">
            <c:set var="hasVisibleSearches" value="true"/>
        </c:if>
    </c:forEach>
    <c:if test="${hasVisibleSearches == 'true'}">
        <c:forEach var="search" items="${searched}">
            <c:if test="${search.name != 'Keyword: *' && search.name != 'Keyword: *:*'}">
                <div class="search-keyword-chip">
                    <span class="search-keyword-value">${search.name}</span>
                    <button type="button" class="search-keyword-remove" onclick="removeSearch(${search.id - 1})" title="Remove"><i class="bi-x"></i></button>
                </div>
            </c:if>
        </c:forEach>
        <button type="button" class="search-keyword-clear-all" onclick="removeAllSearch()"><i class="bi-trash"></i> Clear All</button>
    </c:if>
</div>

<c:choose>
  <c:when test="${result != null && result.documents != null && fn:length(result.documents) > 0}">

    <!-- Results Header -->
    <div class="results-header">
        <!-- Search Results / Case View toggle (issue #76) -->
        <div class="view-tabs">
            <button type="button" class="view-tab ${caseView ? '' : 'view-tab-active'}"
                    onclick="exitCaseView()" title="Show the documents matching your search">
                <i class="bi-search"></i> Search Results
            </button>
            <button type="button" class="view-tab ${caseView ? 'view-tab-active' : ''}"
                    onclick="enterCaseView()" title="Browse the whole case in order, around the selected document">
                <i class="bi-collection"></i> Case View
            </button>
        </div>
        <c:choose>
            <c:when test="${caseView}">
                <span class="results-count"><i class="bi-collection"></i> Case View &mdash; all <strong>${result.totalSize}</strong> documents in natural order</span>
            </c:when>
            <c:otherwise>
                <span class="results-count">Results: <strong>${result.totalSize} documents</strong></span>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Results Toolbar -->
    <div class="results-toolbar">
        <div class="results-toolbar-left">
            <div class="toolbar-group">
                <span class="toolbar-group-label"><i class="bi-tag"></i> Tag</span>
                <button type="button" class="toolbar-btn" onclick="tagSelectedBox()" title="Tag the checked documents">Selected</button>
                <button type="button" class="toolbar-btn" onclick="tagPageBox()" title="Tag every document on this page">Page</button>
                <button type="button" class="toolbar-btn" onclick="tagAllBox()" title="Tag every document in the results">All</button>
            </div>
            <div class="toolbar-group">
                <span class="toolbar-group-label"><i class="bi-download"></i> Export</span>
                <button type="button" class="toolbar-btn" onclick="exportSelected()" title="Export the checked documents as native files">Selected</button>
                <a class="toolbar-btn" href="filedownload.html?action=exportNativeAll" title="Export all results as native files">All (natives)</a>
                <a class="toolbar-btn" id="export-link" target="_blank" href="filedownload.html?action=exportReport" title="Export an HTML report">Report</a>
            </div>
        </div>
        <div class="results-toolbar-right">
            <button type="button" class="results-view-btn results-view-active" title="List view"><i class="bi-list-ul"></i></button>
            <button type="button" class="results-view-btn" title="Grid view"><i class="bi-grid-3x3-gap"></i></button>
            <button type="button" class="results-view-btn" title="Compact view"><i class="bi-layout-text-sidebar-reverse"></i></button>
            <button type="button" class="results-view-btn" title="Settings"><i class="bi-gear"></i></button>
        </div>
    </div>

    <!-- Tag Selected / Tag Page / Tag All forms -->
    <div id="tag-selected" class="tag-box-modern">
        <span class="tag-box-label">Tag Selected:</span>
        <select class="tag-input-modern form-control" onchange="if(this.value){document.getElementById('tag-selected-text').value=this.value;this.value='';}">
            <option value="">Select predefined tag...</option>
            <option>Responsive</option>
            <option>Privileged</option>
            <option>Hot</option>
            <option>Needs Review</option>
            <option>Confidential</option>
        </select>
        <input id="tag-selected-text" class="tag-input-modern form-control" type="text" name="tag" onkeypress="newAllTagEnter(tagSelected, event)" placeholder="Or type a custom tag..."/>
        <button type="button" class="tag-action-btn" onclick="tagSelected()">Apply</button>
        <button type="button" class="tag-cancel-btn" onclick="document.getElementById('tag-selected').style.display='none';return false;">Cancel</button>
    </div>

    <div id="tag-all" class="tag-box-modern">
        <span class="tag-box-label">Tag All Results:</span>
        <select class="tag-input-modern form-control" onchange="if(this.value){document.getElementById('tag-all-text').value=this.value;this.value='';}">
            <option value="">Select predefined tag...</option>
            <option>Responsive</option>
            <option>Privileged</option>
            <option>Hot</option>
            <option>Needs Review</option>
            <option>Confidential</option>
        </select>
        <input id="tag-all-text" class="tag-input-modern form-control" type="text" name="tag" onkeypress="newAllTagEnter(tagAll, event)" placeholder="Or type a custom tag..."/>
        <button type="button" class="tag-action-btn" onclick="tagAll()">Apply</button>
        <button type="button" class="tag-cancel-btn" onclick="document.getElementById('tag-all').style.display='none';return false;">Cancel</button>
    </div>

    <div id="tag-page" class="tag-box-modern">
        <span class="tag-box-label">Tag This Page:</span>
        <select class="tag-input-modern form-control" onchange="if(this.value){document.getElementById('tag-page-text').value=this.value;this.value='';}">
            <option value="">Select predefined tag...</option>
            <option>Responsive</option>
            <option>Privileged</option>
            <option>Hot</option>
            <option>Needs Review</option>
            <option>Confidential</option>
        </select>
        <input id="tag-page-text" class="tag-input-modern form-control" type="text" name="tag" onkeypress="newAllTagEnter(tagPage, event)" placeholder="Or type a custom tag..."/>
        <button type="button" class="tag-action-btn" onclick="tagPage()">Apply</button>
        <button type="button" class="tag-cancel-btn" onclick="document.getElementById('tag-page').style.display='none';return false;">Cancel</button>
        </form>
    </div>

    <!-- Results Table -->
    <div class="results-table-wrap">
        <table class="results-table" border="0" cellpadding="0" cellspacing="0">
            <thead>
                <tr>
                    <th class="results-th-check"><input type="checkbox" class="results-check-all" title="Select all on this page" onclick="toggleSelectAll(this)" /></th>
                    <th class="results-th-sort" onclick="sortBy('id')" title="Sort by ID">ID <c:if test="${sortField eq 'id'}"><i class="bi-caret-${sortDir eq 'asc' ? 'up' : 'down'}-fill"></i></c:if></th>
                    <%-- Only 'id' (string, single-valued) is sortable in the current Solr
                         schema. File Name (subject), Custodian, Date are multiValued/text
                         or not indexed for sort, so sorting on them would error. They stay
                         plain until sortable index fields are added + reindexed (issue #74
                         follow-up on the FreeEed processing side). --%>
                    <th>File Name</th>
                    <th>Type</th>
                    <th>Custodian</th>
                    <th>Date</th>
                    <th>Tags</th>
                    <th class="results-th-actions"></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="doc" items="${result.documents}" varStatus="status">
                <tr id="row-${doc.documentId}" class="results-row ${caseView && doc.documentId == caseViewAnchorId ? 'case-anchor-row' : ''}" onclick="selectDocument('${doc.documentId}');showPreviewPanel('${doc.documentId}', '${doc.documentName}', ${status.index + 1}, ${result.totalSize})">
                    <td class="results-td-check"><input type="checkbox" class="result-check" onclick="event.stopPropagation()" /></td>
                    <td><div class="results-cell-id">${doc.documentId}</div></td>
                    <td><div class="results-cell-name">${doc.subject}</div></td>
                    <td><div class="results-cell-type">
                        <c:set var="docNameLower" value="${fn:toLowerCase(doc.documentName)}" />
                        <c:choose>
                            <c:when test="${fn:endsWith(docNameLower, '.eml')}"><span class="file-type-badge file-type-email">EML</span></c:when>
                            <c:when test="${fn:endsWith(docNameLower, '.pdf')}"><span class="file-type-badge file-type-pdf">PDF</span></c:when>
                            <c:when test="${fn:endsWith(docNameLower, '.txt')}"><span class="file-type-badge file-type-text">TXT</span></c:when>
                            <c:when test="${fn:endsWith(docNameLower, '.exe')}"><span class="file-type-badge file-type-exe">EXE</span></c:when>
                            <c:otherwise><span class="file-type-badge">${fn:substringAfter(doc.documentName, '.')}</span></c:otherwise>
                        </c:choose>
                    </div></td>
                    <td><div class="results-cell-custodian">${doc.from}</div></td>
                    <td><div class="results-cell-date">${doc.date}</div></td>
                    <td><div class="results-cell-tags" id="tags-cell-${doc.documentId}">
                        <c:forEach var="tag" items="${doc.tags}">
                            <span class="tag-badge tag-badge-${fn:toLowerCase(fn:replace(tag.value, ' ', '-'))} tag-clickable" onclick="event.stopPropagation();addTagToSearch('${tag.value}')" title="Filter by ${tag.value}">
                                ${tag.value}
                                <i class="bi-x tag-remove-icon" onclick="event.stopPropagation();removeDocTagAjax('${doc.documentId}', '${tag.value}', this)" title="Remove tag"></i>
                            </span>
                        </c:forEach>
                    </div></td>
                    <td class="results-td-actions">
                        <button type="button" class="results-action-dot" onclick="event.stopPropagation();" title="More"><i class="bi-three-dots-vertical"></i></button>
                    </td>
                </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>

    <!-- Pagination -->
    <c:if test="${showPagination}">
    <div class="results-pagination">
        <div class="results-pagination-left">
            <span class="results-page-info">Show</span>
            <select class="results-page-size" id="results-page-size" onchange="changePageSize(this.value)">
                <option value="10" ${pageSize == 10 ? 'selected' : ''}>10</option>
                <option value="25" ${pageSize == 25 ? 'selected' : ''}>25</option>
                <option value="50" ${pageSize == 50 ? 'selected' : ''}>50</option>
                <option value="100" ${pageSize == 100 ? 'selected' : ''}>100</option>
            </select>
            <span class="results-page-info">per page</span>
            <span class="results-page-range">${resultFrom + 1}-${resultFrom + fn:length(result.documents)} of ${result.totalSize}</span>
        </div>
        <div class="results-pagination-right">
            <button type="button" class="page-btn" <c:if test="${!showPrev}">disabled</c:if> onclick="changePage(1)" title="First page"><i class="bi-chevron-bar-left"></i></button>
            <button type="button" class="page-btn" <c:if test="${!showPrev}">disabled</c:if> onclick="changePage(${currentPage - 1})" title="Previous"><i class="bi-chevron-left"></i></button>
            <span class="page-label">Page</span>
            <input class="page-num-input" type="text" value="${currentPage}" onkeypress="if(event.keyCode==13){changePage(parseInt(this.value));}" />
            <span class="page-label"></span>
            <button type="button" class="page-btn" <c:if test="${!showNext}">disabled</c:if> onclick="changePage(${currentPage + 1})" title="Next"><i class="bi-chevron-right"></i></button>
            <button type="button" class="page-btn" <c:if test="${!showNext}">disabled</c:if> onclick="changePage(totalPage)" title="Last page"><i class="bi-chevron-bar-right"></i></button>
        </div>
    </div>
    </c:if>

    <!-- Hidden document detail panels (for selectDocument JS) -->
    <div class="result-details" style="display:none;">
    <c:forEach var="doc" items="${result.documents}">
         <input type="hidden" id="solrid" class="solrid" value="${doc.documentId}"/>
         <div class="result-box" id="doc-${doc.documentId}" style="display:none">

             <div class="document-tags">
               <div class="document-tags-label">
                 Tags (<span id="tags-total-${doc.documentId}">${fn:length(doc.tags)}</span>)
               </div>
               <div class="document-tags-table" id="tags-box-${doc.documentId}">
                   <table id="tags-table-${doc.documentId}" border="0" cellpadding="0" cellspacing="0">
                       <c:forEach var="tag" items="${doc.tags}">
                           <input type="hidden" class="doc-tag-${doc.documentId}" value="${tag.value}"/>
                           <tr class="document-tags-row">
                              <td><div class="document-tags-tag">${tag.value}</div></td>
                              <td><a href="#" onclick="deleteTag('${doc.documentId}', this, '${tag.name}')"><img src="../images/delete.gif"/></a></td>
                           </tr>
                       </c:forEach>
                   </table>
               </div>
            </div> 

            <div class="operations-box">
                <!-- Hidden trigger clicked by showPreviewPanel() to load the document into the preview panel -->
                <a id="preview-${doc.documentId}" class="html-preview" style="display:none" fileName="${fn:escapeXml(doc.documentName)}" data="${fn:escapeXml(doc.documentPath)}" uid="${doc.uniqueId}">Preview</a>
                <div class="operation-link">
                    <a href="javascript:;" class="operation-link-text action-button" onclick="showMetadataModal('${doc.documentId}')">Metadata</a>
                </div>
                <div class="operation-link">
                    <a href="javascript:;" class="operation-link-text action-button" onclick="$('#tag-doc-${doc.documentId}').slideToggle(200);">Tag</a>
                </div>
                <div class="operation-link">
                    <a class="operation-link-text action-button" href="filedownload.html?action=exportNative&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export native</a>
                </div>
                <div class="operation-link">
                    <a class="operation-link-text action-button" href="filedownload.html?action=exportImage&docPath=${doc.documentPath}&uniqueId=${doc.uniqueId}">Export image</a>
                </div>
            </div>
            <div id="tag-doc-${doc.documentId}" class="tag-box details">
                <input id="tag-doc-field-${doc.documentId}" class="tag-doc-field-cl form-control" type="text" name="tag" onkeypress="newTagEnter('${doc.documentId}', event)"/>
                <input type="button" class="action-button" value="Tag" onclick="newTag('${doc.documentId}')"/>
                <input type="button" class="action-button" value="Cancel" onclick="document.getElementById('tag-doc-${doc.documentId}').style.display='none';return false;"/>
            </div>
            <div class="inline-preview-container" id="inline-preview-${doc.documentId}">
                <div class="inline-preview-loading">Loading preview...</div>
            </div>
            <div id="metadata-content-${doc.documentId}" style="display:none">
            <table border="0" class="metadata-table">
                <c:forEach var="entry" items="${doc.entries}">
                    <tr>
                      <c:choose>
                        <c:when test="${entry.key != 'text'}">
                          <td class="result-box-key">${entry.key}</td>
                          <td><div class="result-box-value">${entry.value}</div></td>
                        </c:when>
                        <c:otherwise>
                            <td colspan="2" class="result-box-text">
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
        </div>
    </c:forEach>
    </div>

  </c:when>
  <c:otherwise>
    <div class="no-result-modern">
        <i class="bi-search no-result-icon"></i>
        <p class="no-result-text">No results found</p>
        <p class="no-result-sub">Try adjusting your search terms or filters</p>
    </div>
  </c:otherwise>
</c:choose>

<script>
function showPreviewPanel(docId, docName, idx, total) {
    var panel = document.getElementById('preview-panel');
    panel.style.display = 'flex';
    document.getElementById('preview-doc-title').textContent = docName || docId;
    document.getElementById('preview-nav-counter').textContent = idx + ' of ' + total;
    
    // Trigger the preview click for the selected document
    setTimeout(function() {
        var previewBtn = document.getElementById('preview-' + docId);
        if (previewBtn) {
            previewBtn.click();
        }
    }, 100);
}

// NOTE: applyQuickTag() lives in search.js (multi-doc: tags every checked row,
// falling back to the selected row). An older single-doc copy used to live here
// and, because this fragment loads via AJAX after search.js, it silently
// redefined and clobbered the good one -- so "Tag Selected" only ever tagged one
// document (issue #78 regression). Removed; do not re-add a definition here.

// Update tag badges in the results table row
function updateRowTagBadges(docId, tagName) {
    var cell = document.getElementById('tags-cell-' + docId);
    if (!cell) return;
    // Check if tag already exists in the cell
    var existing = cell.querySelectorAll('.tag-badge');
    for (var i = 0; i < existing.length; i++) {
        if (existing[i].textContent.trim() === tagName) return;
    }
    var cssClass = 'tag-badge-' + tagName.toLowerCase().replace(/\s+/g, '-');
    var badge = document.createElement('span');
    badge.className = 'tag-badge ' + cssClass + ' tag-clickable';
    badge.textContent = tagName;
    badge.title = 'Filter by ' + tagName;
    badge.onclick = function(e) { e.stopPropagation(); addTagToSearch(tagName); };
    cell.appendChild(badge);
}

// Toggle the custom tag input
function toggleCustomTagInput() {
    var wrap = document.getElementById('custom-tag-input-wrap');
    if (wrap.style.display === 'none') {
        wrap.style.display = 'flex';
        document.getElementById('custom-tag-input').focus();
    } else {
        wrap.style.display = 'none';
    }
}
</script>