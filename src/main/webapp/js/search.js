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

function applyQuickTag(tag) {
    if (!tag || tag.trim() === '') return;
    tag = tag.trim();

    // Determine target documents: checked rows, or the currently selected row (lastDocId)
    var docIds = [];
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

    // Add to allTags so it persists in the filter list if it's a new tag
    if (typeof allTags !== 'undefined') {
        allTags[tag] = 1;
    }

    var pending = docIds.length;
    for (var i = 0; i < docIds.length; i++) {
        (function(docId) {
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
                    pending--;
                    if (pending === 0) _finishApplyingTags();
                },
                error: function () {
                    pending--;
                    if (pending === 0) _finishApplyingTags();
                }
            });
        })(docIds[i]);
    }
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
        queryStr = '*';
    }

    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'search', query: queryStr},
        success: function (data) {
            lastDocId = null;

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

function tagAllBox() {
    $("#tag-all").slideToggle(200);
    $("#tag-page").hide();
}

function tagPageBox() {
    $("#tag-page").slideToggle(200);
    $("#tag-all").hide();
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

            for (var docId in documentsMap) {
                displayTag(docId, tag);
            }

            $("#" + boxId).hide();
            $("#" + textId).val('');
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

function showPreviewPanelIfNeeded() {
    var panel = document.getElementById('preview-panel');
    if (panel) panel.style.display = 'flex';
}

function loadIframeContent(htmlContent) {
    var target = getPreviewTarget();
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
                    loadIframeContent(data);
                },
                error: function () {
                    alert("Technical error, try that again in a few moments!");
                }
            });
        }
    });


});