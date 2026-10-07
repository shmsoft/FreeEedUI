var lastDocId = null;
var documentsMap = new Object();
var allTags = new Object();

function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val || '-';
}

function selectDocument(docId) {
    if (docId == lastDocId) {
        return;
    }

    $("#row-" + docId).addClass("result-list-row-selected");
    $("#doc-" + docId).show();

    if (lastDocId != null) {
        $("#row-" + lastDocId).removeClass("result-list-row-selected");
        $("#doc-" + lastDocId).hide();
    }

    lastDocId = docId;
}

function initPage(docId) {
    selectDocument(docId);
    initTags();
    if (typeof restoreSelectionState === 'function') restoreSelectionState();
}

function newTagEnter(docId, e) {
    var charCode;

    if (e && e.which) {
        charCode = e.which;
    } else if (window.event) {
        e = window.event;
        charCode = e.keyCode;
    }

    if (charCode != 13) {
        return;
    }



    newTag(docId);
}

function newTag(docId) {
    var tag = $("#tag-doc-field-" + docId).val();
    if (tag == null || tag.length == 0) {
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'newtag', docid: docId, tag: tag},
        success: function (data) {
            if (data != 'SUCCESS') {
                return;
            }

            displayTag(docId, tag);

            $("#tag-doc-" + docId).hide();
            $("#tag-doc-field-" + docId).val('');
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function displayTag(docId, tag) {
    addCaseTag(tag);

    if (documentsMap[docId][tag] != null) {
        return;
    } else {
        documentsMap[docId][tag] = 1;
    }

    var docIdParam = '"' + docId + '"';
    var tagParam = '"' + tag + '"';
    $("#tags-table-" + docId).append("<tr class='document-tags-row'>" +
        "<td><div class='document-tags-tag'>" + tag + "</div></td>" +
        "<td><a href='#' onclick='deleteTag(" + docIdParam + ", this, " + tagParam + ")'><img src='images/delete.gif'/></a></td>" +
        "</tr>");
    var total = parseInt($("#tags-total-" + docId).html()) + 1;
    $("#tags-total-" + docId).html(total);
}

function deleteTag(docId, el, tag) {
    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'deletetag', docid: docId, tag: tag},
        success: function (data) {
            $(el).parent().parent().remove();
            var total = parseInt($("#tags-total-" + docId).html()) - 1;
            $("#tags-total-" + docId).html(total);
            if (total == 0) {
                $("#tags-box-" + docId).hide();
            }
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function removeDocTagAjax(docId, tag, element) {
    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'deletetag', docid: docId, tag: tag},
        success: function (data) {
            // Remove the badge from the UI
            var badge = element.closest('.tag-badge');
            if (badge) badge.remove();
            
            // Also update preview panel if it's open for this doc
            if (lastDocId === docId) {
                var previewBadges = document.querySelectorAll('#ptc-tag-list .tag-badge');
                for (var i = 0; i < previewBadges.length; i++) {
                    if (previewBadges[i].textContent.trim() === tag) {
                        previewBadges[i].closest('.ptc-tag-item').remove();
                        break;
                    }
                }
                var count = document.getElementById('preview-tags-count');
                if (count) count.textContent = Math.max(0, parseInt(count.textContent) - 1);
            }
            
            // Update filter counts globally
            if (typeof updateFilterCounts === 'function') updateFilterCounts();
        },
        error: function () {
            alert("Failed to remove tag. Please try again.");
        }
    });
}

function applyQuickTag(tag, explicitDocIds) {
    if (!tag || tag.trim() === '') return;
    tag = tag.trim();

    // Target documents: an explicit id list (e.g. the cross-page selection from
    // tagSelected), else the checked rows on this page, else the current row.
    var docIds = [];
    if (explicitDocIds && explicitDocIds.length > 0) {
        docIds = explicitDocIds.slice();
    } else {
        var checkedRows = document.querySelectorAll('.results-row input.result-check:checked');
        if (checkedRows.length > 0) {
            for (var i = 0; i < checkedRows.length; i++) {
                var row = checkedRows[i].closest('.results-row');
                var idCell = row.querySelector('.results-cell-id');
                if (idCell) docIds.push(idCell.textContent.trim());
            }
        } else if (lastDocId) {
            docIds.push(lastDocId);
        } else {
            alert('Please select a document to tag.');
            return;
        }
    }

    // Add to allTags so it persists in the filter list if it's a new tag
    if (typeof allTags !== 'undefined') {
        allTags[tag] = 1;
    }

    // Tag the documents ONE AT A TIME, not in parallel. The tag endpoint is not
    // safe under concurrent writes to the same case: firing the newtag POSTs
    // simultaneously loses all but one of them (only the last-committed doc keeps
    // its tag), even though the backend guards tagging with a lock. Chaining the
    // requests -- each starting only after the previous finished -- makes every
    // selected document actually persist its tag (issue #78).
    var idx = 0;
    function _tagNextDoc() {
        if (idx >= docIds.length) {
            _finishApplyingTags();
            return;
        }
        var docId = docIds[idx++];
        $.ajax({
            type: 'POST',
            url: 'tag.html',
            data: {action: 'newtag', docid: docId, tag: tag},
            success: function (data) {
                if (data === 'SUCCESS') {
                    // Add badge to grid
                    var tagsCell = document.getElementById('tags-cell-' + docId);
                    if (tagsCell) {
                        var exists = false;
                        var badges = tagsCell.querySelectorAll('.tag-badge');
                        for (var b = 0; b < badges.length; b++) {
                            if ((badges[b].getAttribute('title') || '').replace('Filter by ', '').trim() === tag) {
                                exists = true; break;
                            }
                        }
                        if (!exists) {
                            var badgeClass = 'tag-badge-' + tag.toLowerCase().replace(/\s+/g,'-');
                            var safeTag = tag.replace(/'/g, "\\'");
                            var span = document.createElement('span');
                            span.className = 'tag-badge ' + badgeClass + ' tag-clickable';
                            span.setAttribute('onclick', "event.stopPropagation();addTagToSearch('" + safeTag + "')");
                            span.setAttribute('title', "Filter by " + tag);
                            span.innerHTML = _escapeHtmlJs(tag) + ' <i class="bi-x tag-remove-icon" onclick="event.stopPropagation();removeDocTagAjax(\'' + docId + '\', \'' + safeTag + '\', this)" title="Remove tag"></i>';
                            tagsCell.appendChild(span);
                        }
                    }
                }
                _tagNextDoc();
            },
            error: function () {
                _tagNextDoc();
            }
        });
    }
    _tagNextDoc();
}

function _finishApplyingTags() {
    if (typeof updateFilterCounts === 'function') updateFilterCounts();
    // Refresh preview panel tags tab if active
    var tagsTab = document.getElementById('preview-tags-tab');
    if (tagsTab && tagsTab.classList.contains('preview-tab-active')) {
        if (typeof switchPreviewTab === 'function') switchPreviewTab(tagsTab, 'tags');
    }
}

function _escapeHtmlJs(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

function search() {

    var queryStr = $("#search-query").val();
    if (!queryStr || queryStr.trim() === '') {
        queryStr = '*:*';
    }

    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'search', query: queryStr},
        success: function (data) {
            lastDocId = null;
            // A new query is a new result set -- previous cross-page selections no
            // longer apply, so reset them.
            selectedDocs = {};

            $("#result-ajax").html(data);

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }

            $("#search-query").val('');

            const exportLink = document.getElementById('export-link');
            if(exportLink)
            {
                const uniqueId = $("#case_select option:selected").text();
                exportLink.setAttribute('download', `report_${uniqueId}.html`);
            }

            if (typeof updateFilterCounts === 'function') updateFilterCounts();
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function addTagToSearch(tag) {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'tagsearch', tag: tag},
        success: function (data) {
            lastDocId = null;

            $("#result-ajax").html(data);

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }

            if (typeof updateFilterCounts === 'function') updateFilterCounts();
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function deleteCaseTag(el, tag) {
    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'deleteCasetag', tag: tag},
        success: function (data) {
            $(el).parent().remove();
            removeSearch(-1);
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function changePage(page, fromNavigation) {
    var lastSelectedPage = currentPage;
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'changepage', page: page},
        success: function (data) {
            lastDocId = null;

            $("#result-ajax").html(data);

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            if(fromNavigation)
            {
                currentIndex = lastSelectedPage < page ? 0 : documents.length - 1;
                var docId = documents[currentIndex].documentId;
                selectDocument(docId);
                $("#preview-" + docId).click();
            }
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

// Re-sort the results list by a column (issue #74). Toggles asc/desc when the
// same column is clicked again; the sort is held server-side in the session.
function sortBy(field) {
    var dir = (typeof currentSortField !== 'undefined'
               && currentSortField === field
               && currentSortDir === 'asc') ? 'desc' : 'asc';
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'sort', sort: field, dir: dir},
        success: function (data) {
            lastDocId = null;
            $("#result-ajax").html(data);
            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

// Change how many documents show per page (issue #74). Returns to page 1.
function changePageSize(size) {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'pagesize', pageSize: size},
        success: function (data) {
            lastDocId = null;
            $("#result-ajax").html(data);
            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

// Enter Case View (issue #76): browse the whole case in natural order,
// centered on the currently-selected document (or the first result if none is
// selected), so the reviewer can page to the documents before and after it.
function enterCaseView() {
    var anchorId = lastDocId;
    if (!anchorId && typeof documents !== 'undefined' && documents.length > 0) {
        anchorId = documents[0].documentId;
    }
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'caseview', id: anchorId || ''},
        success: function (data) {
            lastDocId = null;
            $("#result-ajax").html(data);
            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            // Select and scroll the anchor doc into view so its neighbours show.
            if (caseViewAnchorId) {
                selectDocument(caseViewAnchorId);
                var row = document.getElementById('row-' + caseViewAnchorId);
                if (row && row.scrollIntoView) {
                    row.scrollIntoView({block: 'center'});
                }
            }
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

// Leave Case View, back to the filtered search results (issue #76).
function exitCaseView() {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'searchview'},
        success: function (data) {
            lastDocId = null;
            $("#result-ajax").html(data);
            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function removeSearch(id) {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'remove', id: id},
        success: function (data) {
            lastDocId = null;

            $("#result-ajax").html(data);

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }

            if (typeof updateFilterCounts === 'function') updateFilterCounts();
            if (typeof highlightSearchResults === 'function') highlightSearchResults();
            
            // If the removal resulted in an empty result set (no documents), do a default search
            if (documents.length === 0) {
                search();
            }
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function removeAllSearch() {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'removeall'},
        success: function (data) {
            $("#search-query").val('');
            // Clearing all filters means we want to see ALL documents.
            search();
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function initTags() {
    $(".document-tags-table").hide();
    $(".document-tags-label").click(function () {
        $(this).next(".document-tags-table").slideToggle(200);
    });

    $(".solrid").each(function (index) {
        var docId = $(this).val();
        documentsMap[docId] = new Object();
        $(".doc-tag-" + docId).each(function (index) {
            var tag = $(this).val();
            documentsMap[docId][tag] = 1;
        });
    });

    $(".tag-doc-field-cl").autocomplete({source: "tagauto.html"});
    $("#tag-all-text").autocomplete({source: "tagauto.html"});
    $("#tag-page-text").autocomplete({source: "tagauto.html"});
}

// ---- Persistent selection across pages -------------------------------------
// Selection must survive pagination/sort so a reviewer can build a set spanning
// pages, then Tag/Export it. The DOM only holds the current page, so the real
// selection lives here, keyed by document id; the value carries the path +
// uniqueId needed for export, resolved from the page's `documents` array when the
// row is checked. A new search() clears it; changePage/sort/changePageSize
// preserve it (same result set, different view).
var selectedDocs = (typeof selectedDocs !== 'undefined' && selectedDocs) ? selectedDocs : {};

function _rowDocId(row) {
    var c = row && row.querySelector('.results-cell-id');
    return c ? c.textContent.trim() : null;
}

function _docById(docId) {
    if (typeof documents === 'undefined' || !docId) return null;
    for (var i = 0; i < documents.length; i++) {
        if (documents[i].documentId === docId) return documents[i];
    }
    return null;
}

function _setSelected(docId, on) {
    if (!docId) return;
    if (on) {
        var d = _docById(docId);
        selectedDocs[docId] = d
            ? {documentId: docId, documentPath: d.documentPath, uniqueId: d.uniqueId}
            : (selectedDocs[docId] || {documentId: docId});
    } else {
        delete selectedDocs[docId];
    }
}

function selectedCount() {
    return Object.keys(selectedDocs).length;
}

// Clear the whole cross-page selection (Clear button).
function clearSelection() {
    selectedDocs = {};
    var boxes = document.querySelectorAll('.results-row input.result-check');
    for (var i = 0; i < boxes.length; i++) boxes[i].checked = false;
    updateSelectionUI();
}

// Header "Select all" checkbox: select/deselect every row on the CURRENT page,
// updating the persistent store so the choice carries across pages.
function toggleSelectAll(headerCb) {
    var rows = document.querySelectorAll('.results-row');
    for (var i = 0; i < rows.length; i++) {
        var box = rows[i].querySelector('input.result-check');
        if (!box) continue;
        box.checked = headerCb.checked;
        _setSelected(_rowDocId(rows[i]), headerCb.checked);
    }
    updateSelectionUI();
}

// Reflect selection state: header checked/indeterminate for THIS page, and a
// running "(N selected)" count across all pages.
function updateSelectionUI() {
    var header = document.querySelector('.results-check-all');
    if (header) {
        var boxes = document.querySelectorAll('.results-row input.result-check');
        var checked = document.querySelectorAll('.results-row input.result-check:checked');
        header.checked = boxes.length > 0 && checked.length === boxes.length;
        header.indeterminate = checked.length > 0 && checked.length < boxes.length;
    }
    var counter = document.getElementById('selected-count');
    if (counter) {
        var n = selectedCount();
        counter.textContent = n > 0 ? '(' + n + ' selected)' : '';
    }
}

// After each AJAX re-render (page change / sort / page-size), restore the
// checkboxes on the newly-shown page from the persistent store. Called by initPage().
function restoreSelectionState() {
    var rows = document.querySelectorAll('.results-row');
    for (var i = 0; i < rows.length; i++) {
        var box = rows[i].querySelector('input.result-check');
        if (!box) continue;
        box.checked = !!selectedDocs[_rowDocId(rows[i])];
    }
    updateSelectionUI();
}

// Keep the store + header in sync when an individual row is toggled. Delegated on
// document so it survives the AJAX re-render of the results table.
document.addEventListener('change', function (e) {
    var t = e.target;
    if (!t || !t.classList || !t.classList.contains('result-check')) return;
    _setSelected(_rowDocId(t.closest('.results-row')), t.checked);
    updateSelectionUI();
});

function tagSelectedBox() {
    $("#tag-selected").slideToggle(200);
    $("#tag-all").hide();
    $("#tag-page").hide();
}

function tagAllBox() {
    $("#tag-all").slideToggle(200);
    $("#tag-page").hide();
    $("#tag-selected").hide();
}

function tagPageBox() {
    $("#tag-page").slideToggle(200);
    $("#tag-all").hide();
    $("#tag-selected").hide();
}

function newAllTagEnter(callFunc, e) {
    var charCode;

    if (e && e.which) {
        charCode = e.which;
    } else if (window.event) {
        e = window.event;
        charCode = e.keyCode;
    }

    if (charCode != 13) {
        return;
    }

    callFunc();
}

// Tag only the checked documents (issue #78). Reuses applyQuickTag(), which
// applies a tag to every checked row and updates their badges in place.
function tagSelected() {
    var tag = $("#tag-selected-text").val();
    if (tag == null || tag.trim().length == 0) {
        return;
    }
    var ids = Object.keys(selectedDocs);
    if (ids.length === 0) {
        alert('Please check one or more documents first, then click Apply.');
        return;
    }
    // Tag the whole cross-page selection, not just the rows visible now.
    applyQuickTag(tag.trim(), ids);
    $("#tag-selected").hide();
    $("#tag-selected-text").val('');
}

// Export only the checked documents as native files (issue #79 / #78 companion).
// Backend action exportNativeSelected already exists; collect the checked rows'
// paths + uniqueIds from the page's `documents` array and POST them.
// Export the checked documents. `action` picks the format:
//   exportNativeSelected -> native files (zip)
//   exportPdfSelected    -> one combined PDF
function _exportSelectedAs(action) {
    // Drive from the cross-page selection store (path + uniqueId were captured
    // when each row was checked), so export covers docs selected on other pages.
    var ids = Object.keys(selectedDocs);
    if (ids.length === 0) {
        alert('Please check one or more documents to export.');
        return;
    }
    var paths = [], uids = [];
    for (var i = 0; i < ids.length; i++) {
        var d = selectedDocs[ids[i]];
        if (d && d.documentPath && d.uniqueId) {
            paths.push(d.documentPath);
            uids.push(d.uniqueId);
        }
    }
    if (paths.length === 0) {
        alert('Could not resolve the selected documents. Try again.');
        return;
    }
    // Trigger the download via a POST form (paths/uids can be long).
    var form = document.createElement('form');
    form.method = 'POST';
    form.action = 'filedownload.html';
    function addField(name, value) {
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        form.appendChild(input);
    }
    addField('action', action);
    addField('docPaths', paths.join('|||'));
    addField('uniqueIds', uids.join('|||'));
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
}

function exportSelected() {
    _exportSelectedAs('exportNativeSelected');
}

// Combine the checked documents' PDF renditions into one PDF (issue #75).
function exportPdfSelected() {
    _exportSelectedAs('exportPdfSelected');
}

function tagAll() {
    tagDocuments("tag-all-text", "tag-all", "tagall");
}

function tagPage() {
    tagDocuments("tag-page-text", "tag-page", "tagpage");
}

function tagDocuments(textId, boxId, action) {
    var tag = $("#" + textId).val();
    if (tag == null || tag.length == 0) {
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: action, tag: tag},
        success: function (data) {
            if (data != 'SUCCESS') {
                return;
            }

            $("#" + boxId).hide();
            $("#" + textId).val('');

            // Refresh the results so the new tags appear in the TAGS column
            // (Tag All / Tag Page wrote to Solr; re-run the current page's
            // search to re-render with the tags). Without this the tags are
            // applied but invisible until the user searches again.
            changePage((typeof currentPage !== 'undefined' && currentPage > 0) ? currentPage : 1);
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function addCaseTag(tag) {
    if (allTags[tag] == null) {
        allTags[tag] = 1;
        appendCaseTag(tag);
    }
}

function appendCaseTag(tag) {
    $(".case-tags-box-body").append("<div id='" + tag + "' class='case-tag'><div class='case-tags-box-row' onclick='addTagToSearch(\"" + tag + "\")'>" + tag +
        "</div><a href='#' onclick='deleteCaseTag(this,\"" + tag + "\")'><img src='images/delete.gif'/></a></div>");
}

function getUrlVars() {
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for (var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
    }
    return vars;
}

// Get the target container for preview content (inline panel or modal fallback)
function getPreviewTarget() {
    var panel = document.getElementById('preview-panel-body');
    if (panel) return panel;
    return document.querySelector('#html_preview_modal_content');
}

function _showPreviewFallback(target) {
    var textContent = (typeof _getDocText === 'function') ? _getDocText() : '';
    if (textContent && textContent.trim().length > 0) {
        target.innerHTML = '<div style="padding:16px;font-size:12px;line-height:1.7;color:#334155;white-space:pre-wrap;word-wrap:break-word;font-family:ui-monospace,monospace;background:#f8fafc;height:100%;overflow:auto;box-sizing:border-box;">' +
            textContent.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div>';
    } else {
        target.innerHTML = '<div style="padding:32px;text-align:center;color:#94a3b8;">' +
            '<div style="font-size:36px;margin-bottom:12px;">&#128196;</div>' +
            '<p style="font-weight:600;color:#64748b;margin:0 0 8px">Preview not available</p>' +
            '<p style="font-size:12px;margin:0">Use the <strong>Text/OCR</strong> or <strong>Metadata</strong> tabs to review this document.</p></div>';
    }
}

function showPreviewPanelIfNeeded() {
    var panel = document.getElementById('preview-panel');
    if (panel) panel.style.display = 'flex';
}

function loadIframeContent(htmlContent) {
    var target = getPreviewTarget();
    // Detect full-page layout responses (error pages from server)
    if (htmlContent.indexOf('class="wrapper"') >= 0 || htmlContent.indexOf('class="left"') >= 0) {
        _showPreviewFallback(target);
        showPreviewPanelIfNeeded();
        return;
    }
    target.innerHTML = '';
    var iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = 'none';
    iframe.style.minHeight = '400px';
    target.appendChild(iframe);
    var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
    iframeDoc.open();
    iframeDoc.write(htmlContent);
    iframeDoc.close();
    showPreviewPanelIfNeeded();
}

function loadPdfInIframe(pdfUrl) {
    var target = getPreviewTarget();
    target.innerHTML = '';
    var iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = 'none';
    iframe.style.minHeight = '400px';
    iframe.src = pdfUrl;
    iframe.onload = function() {
        try {
            var body = iframe.contentDocument && iframe.contentDocument.body;
            if (body && (body.querySelector('.left') || body.querySelector('.menulink'))) {
                _showPreviewFallback(target);
            }
        } catch(e) { /* cross-origin */ }
    };
    target.appendChild(iframe);
    showPreviewPanelIfNeeded();
}

function loadImageInIframe(imageUrl) {
    var target = getPreviewTarget();
    target.innerHTML = '';
    target.style.textAlign = 'center';
    var img = document.createElement('img');
    img.src = imageUrl;
    img.style.width = '100%';
    img.style.height = 'auto';
    target.appendChild(img);
    showPreviewPanelIfNeeded();
}

function loadTxtInIframe(txtUrl) {
    var target = getPreviewTarget();
    target.innerHTML = '';
    fetch(txtUrl)
        .then(function(response) { return response.text(); })
        .then(function(text) {
            var pre = document.createElement('pre');
            pre.textContent = text;
            pre.style.whiteSpace = 'pre-wrap';
            pre.style.wordWrap = 'break-word';
            pre.style.padding = '14px';
            pre.style.backgroundColor = '#f8f9fa';
            pre.style.border = '1px solid #e5e7eb';
            pre.style.borderRadius = '8px';
            pre.style.fontSize = '12px';
            pre.style.lineHeight = '1.6';
            target.appendChild(pre);
            showPreviewPanelIfNeeded();
        })
        .catch(function(error) { console.error('Error loading TXT file:', error); });
}

function loadTiffInIframe(imageUrl) {
    var target = getPreviewTarget();
    target.innerHTML = '';
    fetch(imageUrl)
        .then(function(response) { return response.arrayBuffer(); })
        .then(function(buffer) {
            var tiff = new Tiff({ buffer: buffer });
            var canvas = tiff.toCanvas();
            canvas.style.maxWidth = '100%';
            canvas.style.height = 'auto';
            target.style.textAlign = 'center';
            target.appendChild(canvas);
            showPreviewPanelIfNeeded();
        })
        .catch(function(error) { console.error('Error loading TIFF file:', error); });
}
function getIndexById(uniqueId) {
    for (var i = 0; i < documents.length; i++) {
        if (documents[i].uniqueId === uniqueId) {
            return i;
        }
    }
    return -1; // Return -1 if the unique ID is not found
}
var currentIndex = 0;

function nextDocument() {

    if (currentIndex < documents.length - 1) {
        currentIndex++;
        var docId = documents[currentIndex].documentId;
        selectDocument(docId);
        $("#preview-" + docId).click();
    }
    else
    {
        if(showNext) {
            changePage(currentPage + 1, true);
        }
    }
}

function prevDocument() {
    if (currentIndex > 0) {
        currentIndex--;
        var docId = documents[currentIndex].documentId;
        selectDocument(docId);
        $("#preview-" + docId).click();
    }
    else
    {
        if(showPrev)        {
            changePage(currentPage - 1, true);
        }
    }
}

$(document).ready(function () {

    var queryString = getUrlVars();
    var query = queryString['query'];
    var caseId = queryString['caseid'];
    if (queryString && Object.keys(queryString).length > 0 && query) {
        $("#search-query").val(query);
        $("#case_select").val(caseId);
    }
    
    // Always perform an initial search on page load to display all results
    search();

    $("body").bind({
        ajaxStart: function () {
            $(this).addClass("loading");
        },
        ajaxStop: function () {
            $(this).removeClass("loading");
        }
    });

    $('#search-query').keypress(function (e) {
        if (e.keyCode == 13) {
            search();
        }
    });

    for (var t in allTags) {
        appendCaseTag(t);
    }
    $("body").on("click", ".html-preview", function () {
        var docId = $(this).attr("data");
        var uId = $(this).attr("uid");
        var docName = $(this).attr("fileName");
        currentIndex = getIndexById(uId);
        // Remember the open document so the Redact (FOIA) tab can act on it.
        window._previewDoc = {docPath: docId, uniqueId: uId, docName: docName};

        // Reset zoom/rotation and switch to Document tab
        if (typeof resetPreviewTransform === 'function') resetPreviewTransform();
        var docTab = document.querySelector('.preview-tabs .preview-tab:first-child');
        if (docTab) { docTab.click(); }

        // Update preview panel header
        var titleEl = document.getElementById('preview-doc-title');
        if (titleEl) titleEl.textContent = docName || uId;
        var counterEl = document.getElementById('preview-nav-counter');
        if (counterEl && documents.length > 0) {
            counterEl.textContent = (currentIndex + 1) + ' of ' + documents.length;
        }

        // Populate metadata sidebar from hidden doc detail panel
        var docBox = document.getElementById('doc-' + lastDocId);
        if (docBox) {
            var entries = docBox.querySelectorAll('.result-div table tr');
            var meta = {};
            for (var i = 0; i < entries.length; i++) {
                var cells = entries[i].querySelectorAll('td');
                if (cells.length >= 2 && cells[0].className === 'result-box-key') {
                    meta[cells[0].textContent.trim()] = cells[1].textContent.trim();
                }
            }
            // Email details
            setText('pm-from', meta['Message-From'] || meta['dc:creator'] || '-');
            setText('pm-to', meta['Message-To'] || '-');
            setText('pm-cc', meta['Message-Cc'] || '-');
            setText('pm-bcc', meta['Message-Bcc'] || '-');
            setText('pm-date', meta['dcterms:created'] || meta['Creation-Date'] || '-');
            setText('pm-subject', meta['dc:subject'] || meta['subject'] || '-');
            // File details
            var rName = meta['resourceName'] || docName || '-';
            var ext = rName.split('.').pop().toUpperCase();
            setText('pm-filetype', ext === 'EML' ? 'Email (EML)' : ext);
            setText('pm-filesize', meta['Content-Length'] ? (Math.round(parseInt(meta['Content-Length'])/1024*10)/10 + ' KB') : '-');
            setText('pm-created', meta['dcterms:created'] || meta['Creation-Date'] || '-');
            setText('pm-modified', meta['dcterms:modified'] || meta['Last-Modified'] || '-');
            setText('pm-hash', meta['X-TIKA:digest:MD5'] || meta['Content-MD5'] || '-');
            // Custodian / Path
            setText('pm-custodian', meta['Message-From'] || meta['dc:creator'] || '-');
            setText('pm-collection', '-');
            setText('pm-path', meta['document_original_path'] || meta['resourceName'] || '-');
            // Tags count
            var tagCell = document.getElementById('tags-cell-' + lastDocId);
            var tagsCount = tagCell ? tagCell.querySelectorAll('.tag-badge').length : 0;
            var tagsCountEl = document.getElementById('preview-tags-count');
            if (tagsCountEl) tagsCountEl.textContent = tagsCount;

            // Notes count
            var notesCount = (window._docNotes && window._docNotes[lastDocId]) ? window._docNotes[lastDocId].length : 0;
            var notesCountEl = document.getElementById('preview-notes-count');
            if (notesCountEl) notesCountEl.textContent = notesCount;
        }

        var target = getPreviewTarget();
        target.innerHTML = '<div class="preview-loading"><div class="preview-spinner"></div>Loading preview...</div>';

        var parts = docId.split('.');
        var extension = "";
        if (parts.length > 1) {
            extension = parts.pop();
        }
        if(extension == "pdf") {
            var url = "filedownload.html?action=exportNative&ispreviewpdf=1&docPath=" + docId + "&uniqueId=" + uId;
            loadPdfInIframe(url);
        }
        else if(extension == 'jpg' || extension == 'jpeg' || extension == 'png' || extension == 'tiff' || extension == 'tif') {
            var url = "filedownload.html?action=exportNative&ispreviewimage=1&docPath=" + docId + "&uniqueId=" + uId;
            if (extension == 'tiff' || extension == 'tif') {
                loadTiffInIframe(url);
            } else {
                loadImageInIframe(url);
            }
        }
        else if(extension == 'txt') {
            var url = "filedownload.html?action=exportNative&ispreviewpdf=1&docPath=" + docId + "&uniqueId=" + uId;
            loadTxtInIframe(url);
        }
        else {
            $.ajax({
                type: 'GET',
                url: 'filedownload.html',
                data: {action: 'exportHtml', docPath: docId, uniqueId: uId, docName: docName},
                success: function (data) {
                    // Detect if the server returned a full layout page (error case) instead of document HTML
                    var isFullPage = data.indexOf('class="wrapper"') >= 0 || data.indexOf('class="left"') >= 0 || data.indexOf('class="menulink"') >= 0;
                    if (isFullPage) {
                        // Server returned the full app page (file not found / misconfigured)
                        // Show extracted text content from the already-loaded Solr data instead
                        var textContent = (typeof _getDocText === 'function') ? _getDocText() : '';
                        var t = getPreviewTarget();
                        if (textContent && textContent.trim().length > 0) {
                            t.innerHTML = '<div style="padding:16px;font-size:12px;line-height:1.7;color:#334155;white-space:pre-wrap;word-wrap:break-word;font-family:ui-monospace,monospace;background:#f8fafc;height:100%;overflow:auto;box-sizing:border-box;">' +
                                textContent.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div>';
                        } else {
                            t.innerHTML = '<div style="padding:32px;text-align:center;color:#94a3b8;">' +
                                '<div style="font-size:36px;margin-bottom:12px;">&#128196;</div>' +
                                '<p style="font-weight:600;color:#64748b;margin:0 0 8px">Preview not available</p>' +
                                '<p style="font-size:12px;margin:0">Use the <strong>Text/OCR</strong> or <strong>Metadata</strong> tabs to review this document.</p></div>';
                        }
                        showPreviewPanelIfNeeded();
                    } else {
                        loadIframeContent(data);
                    }
                },
                error: function () {
                    var t = getPreviewTarget();
                    t.innerHTML = '<div style="padding:32px;text-align:center;color:#ef4444;font-size:13px;">Failed to load document preview.</div>';
                    showPreviewPanelIfNeeded();
                }
            });
        }
    });


});

/* =====================================================================
 * FOIA manual redaction (docs/decisions/redaction.md)
 * The "Redact" preview tab lets a reviewer draw labeled exemption boxes on
 * the document's rasterized PDF rendition. Boxes are a separate annotation
 * layer (stored server-side, keyed by uniqueId); they are only burned into a
 * copy when a redacted release set is exported. Nothing here modifies the
 * original or the rendition.
 * ===================================================================== */
var _red = {
    uniqueId: null, docPath: null, docName: null,
    page: 1, pages: 0,
    boxes: [],            // {page,x,y,w,h,code} in normalized (0..1) coords
    exemptions: null,     // [{name, codes:[{code,label}]}]
    currentCode: '',
    dirty: false
};

function _redLoadExemptions(cb) {
    if (_red.exemptions) { cb(); return; }
    $.ajax({
        type: 'GET', url: 'redaction.html', data: {action: 'exemptions'}, dataType: 'json',
        success: function (data) { _red.exemptions = (data && data.profiles) ? data.profiles : []; cb(); },
        error: function () { _red.exemptions = []; cb(); }
    });
}

// Entry point, called from switchPreviewTab(this,'redact').
function enterRedactionMode(docArea) {
    var d = window._previewDoc;
    if (!d || !d.uniqueId) {
        docArea.innerHTML = '<div style="padding:32px;text-align:center;color:#94a3b8;font-size:13px;">'
            + 'Open a document first, then switch to Redact.</div>';
        return;
    }
    _red.uniqueId = d.uniqueId; _red.docPath = d.docPath; _red.docName = d.docName;
    _red.page = 1; _red.pages = 0; _red.boxes = []; _red.dirty = false;

    docArea.innerHTML =
        '<div class="redaction-workspace">'
        + '  <div class="redaction-toolbar">'
        + '    <label class="red-lbl">Exemption</label>'
        + '    <select id="red-code" class="red-select"></select>'
        + '    <span class="red-sep"></span>'
        + '    <button class="red-btn" onclick="redPrevPage()">&#8249; Prev</button>'
        + '    <span id="red-page-info" class="red-page-info">Page 1</span>'
        + '    <button class="red-btn" onclick="redNextPage()">Next &#8250;</button>'
        + '    <span class="red-sep"></span>'
        + '    <button class="red-btn red-btn-primary" onclick="redSave()">Save redactions</button>'
        + '    <span class="red-sep"></span>'
        + '    <button class="red-btn" onclick="exportRedactedSelected()" title="Burn redactions into the checked documents">Export redacted (selected)</button>'
        + '    <button class="red-btn" onclick="exportRedactedAll()" title="Burn redactions into all results">Export redacted (all)</button>'
        + '    <span id="red-status" class="red-status"></span>'
        + '  </div>'
        + '  <div class="redaction-hint">Drag on the page to draw a redaction. Each box prints its exemption code and is permanently burned in on export (no recoverable text underneath).</div>'
        + '  <div class="redaction-scroll"><div id="red-wrap" class="redaction-wrap">'
        + '    <img id="red-img" class="redaction-img" alt="page" />'
        + '    <div id="red-overlay" class="redaction-overlay"></div>'
        + '  </div></div>'
        + '</div>';

    _redLoadExemptions(function () {
        var sel = document.getElementById('red-code');
        if (sel) {
            sel.innerHTML = '';
            if (!_red.exemptions || _red.exemptions.length === 0) {
                var o = document.createElement('option'); o.value = ''; o.textContent = '(no codes configured)';
                sel.appendChild(o);
            }
            for (var p = 0; p < _red.exemptions.length; p++) {
                var prof = _red.exemptions[p];
                var og = document.createElement('optgroup'); og.label = prof.name;
                for (var c = 0; c < prof.codes.length; c++) {
                    var opt = document.createElement('option');
                    opt.value = prof.codes[c].code;
                    opt.textContent = prof.codes[c].code + ' — ' + prof.codes[c].label;
                    og.appendChild(opt);
                }
                sel.appendChild(og);
            }
            _red.currentCode = sel.value;
            sel.onchange = function () { _red.currentCode = this.value; };
        }
        // Discover page count, load existing boxes, render page 1.
        $.ajax({
            type: 'GET', url: 'filedownload.html',
            data: {action: 'renditionInfo', uniqueId: _red.uniqueId, docName: _red.docPath},
            dataType: 'json',
            success: function (info) {
                _red.pages = (info && info.pages) ? info.pages : 0;
                _redLoadBoxes(function () { redRenderPage(1); });
            },
            error: function () { _red.pages = 0; redRenderPage(1); }
        });
    });
}

function _redLoadBoxes(cb) {
    $.ajax({
        type: 'GET', url: 'redaction.html', data: {action: 'list', uniqueId: _red.uniqueId}, dataType: 'json',
        success: function (data) {
            _red.boxes = (data && data.redactions) ? data.redactions : [];
            cb();
        },
        error: function () { _red.boxes = []; cb(); }
    });
}

function redRenderPage(n) {
    if (_red.pages > 0) {
        if (n < 1) n = 1;
        if (n > _red.pages) n = _red.pages;
    }
    _red.page = n;
    var img = document.getElementById('red-img');
    var info = document.getElementById('red-page-info');
    if (info) info.textContent = 'Page ' + n + (_red.pages ? ' of ' + _red.pages : '');
    if (!img) return;
    if (_red.pages === 0) {
        var ov = document.getElementById('red-overlay');
        if (ov) ov.innerHTML = '';
        img.removeAttribute('src');
        img.alt = 'No PDF rendition for this document. Enable "Create PDF Images" and reprocess the case.';
        _redStatus('No PDF rendition — enable "Create PDF Images" and reprocess.', true);
        return;
    }
    img.onload = function () {
        var overlay = document.getElementById('red-overlay');
        if (overlay) { overlay.style.width = img.clientWidth + 'px'; overlay.style.height = img.clientHeight + 'px'; }
        redDrawBoxes();
        _redBindDrawing();
    };
    img.src = 'filedownload.html?action=renditionPage&uniqueId=' + encodeURIComponent(_red.uniqueId)
        + '&docName=' + encodeURIComponent(_red.docPath || '') + '&page=' + n + '&_=' + Date.now();
}

function redPrevPage() { if (_red.page > 1) redRenderPage(_red.page - 1); }
function redNextPage() { if (!_red.pages || _red.page < _red.pages) redRenderPage(_red.page + 1); }

function redDrawBoxes() {
    var overlay = document.getElementById('red-overlay');
    if (!overlay) return;
    overlay.innerHTML = '';
    for (var i = 0; i < _red.boxes.length; i++) {
        var b = _red.boxes[i];
        if (b.page !== _red.page) continue;
        var div = document.createElement('div');
        div.className = 'redaction-box';
        div.style.left = (b.x * 100) + '%';
        div.style.top = (b.y * 100) + '%';
        div.style.width = (b.w * 100) + '%';
        div.style.height = (b.h * 100) + '%';
        var lbl = document.createElement('span');
        lbl.className = 'redaction-box-label';
        lbl.textContent = b.code || '';
        div.appendChild(lbl);
        var del = document.createElement('span');
        del.className = 'redaction-box-del';
        del.textContent = '×';
        del.title = 'Remove this redaction';
        (function (idx) {
            del.onmousedown = function (ev) { ev.stopPropagation(); };
            del.onclick = function (ev) { ev.stopPropagation(); redDeleteBox(idx); };
        })(i);
        div.appendChild(del);
        overlay.appendChild(div);
    }
}

function redDeleteBox(idx) {
    _red.boxes.splice(idx, 1);
    _red.dirty = true;
    redDrawBoxes();
    _redStatus('Unsaved changes', false);
}

function _redBindDrawing() {
    var overlay = document.getElementById('red-overlay');
    if (!overlay || overlay._redBound) return;
    overlay._redBound = true;
    var temp = null, startX = 0, startY = 0;
    overlay.addEventListener('mousedown', function (e) {
        if (e.button !== 0) return;
        var rect = overlay.getBoundingClientRect();
        startX = e.clientX - rect.left; startY = e.clientY - rect.top;
        temp = document.createElement('div');
        temp.className = 'redaction-box redaction-box-temp';
        temp.style.left = startX + 'px'; temp.style.top = startY + 'px';
        temp.style.width = '0px'; temp.style.height = '0px';
        overlay.appendChild(temp);
        e.preventDefault();
    });
    overlay.addEventListener('mousemove', function (e) {
        if (!temp) return;
        var rect = overlay.getBoundingClientRect();
        var cx = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
        var cy = Math.max(0, Math.min(e.clientY - rect.top, rect.height));
        temp.style.left = Math.min(startX, cx) + 'px';
        temp.style.top = Math.min(startY, cy) + 'px';
        temp.style.width = Math.abs(cx - startX) + 'px';
        temp.style.height = Math.abs(cy - startY) + 'px';
    });
    function finish(e) {
        if (!temp) return;
        var rect = overlay.getBoundingClientRect();
        var left = parseFloat(temp.style.left), top = parseFloat(temp.style.top);
        var w = parseFloat(temp.style.width), h = parseFloat(temp.style.height);
        overlay.removeChild(temp); temp = null;
        if (w < 6 || h < 6 || rect.width === 0 || rect.height === 0) { return; }
        if (!_red.currentCode) { _redStatus('Pick an exemption code first.', true); return; }
        _red.boxes.push({
            page: _red.page,
            x: left / rect.width, y: top / rect.height,
            w: w / rect.width, h: h / rect.height,
            code: _red.currentCode
        });
        _red.dirty = true;
        redDrawBoxes();
        _redStatus('Unsaved changes', false);
    }
    overlay.addEventListener('mouseup', finish);
    overlay.addEventListener('mouseleave', finish);
}

function redSave() {
    if (!_red.uniqueId) return;
    var parts = [];
    for (var i = 0; i < _red.boxes.length; i++) {
        var b = _red.boxes[i];
        parts.push(b.page + ',' + b.x + ',' + b.y + ',' + b.w + ',' + b.h + ',' + (b.code || ''));
    }
    $.ajax({
        type: 'POST', url: 'redaction.html',
        data: {action: 'save', uniqueId: _red.uniqueId, boxes: parts.join('|||')},
        dataType: 'json',
        success: function (data) {
            _red.dirty = false;
            _redStatus('Saved ' + ((data && typeof data.count === 'number') ? data.count : _red.boxes.length) + ' redaction(s).', false);
        },
        error: function () { _redStatus('Save failed — try again.', true); }
    });
}

function _redStatus(msg, isError) {
    var el = document.getElementById('red-status');
    if (!el) return;
    el.textContent = msg;
    el.style.color = isError ? '#b91c1c' : '#047857';
}

// Burn redactions into a release set. Reuses the selection store for "selected".
function exportRedactedSelected() {
    if (_red.dirty) { if (!confirm('You have unsaved redactions. Export without saving them?')) return; }
    _exportSelectedAs('exportRedactedSelected');
}
function exportRedactedAll() {
    if (_red.dirty) { if (!confirm('You have unsaved redactions. Export without saving them?')) return; }
    var form = document.createElement('form');
    form.method = 'POST';
    form.action = 'filedownload.html';
    var input = document.createElement('input');
    input.type = 'hidden'; input.name = 'action'; input.value = 'exportRedactedAll';
    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
}