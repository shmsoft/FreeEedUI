<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<script src="js/tiff.min.js"></script>
<script src="js/search.js?v=3"></script>

<script>
<c:forEach var="t" items="${tags}">
    allTags['${t}'] = 1;  
</c:forEach>
</script>

    <!-- Old preview modal removed - using inline preview panel -->
    <div id="html_preview_modal" class="modal fade" style="display:none !important;">
      <div class="modal-dialog modal-wide">
        <div class="modal-content">
          <div class="modal-body" id="html_preview_modal_content"></div>
        </div>
      </div>
    </div>

<!-- Hidden case form (still needed for backend) -->
<div style="display:none;">
    <form name="change" method="post" action="search.html">
        <input type="hidden" name="action" value="changecase"/>
        <select id="case_select" name="id" onchange="document.change.submit()">
            <c:forEach var="c" items="${cases}">
                <option value="${c.id}" ${(selectedCase != null && selectedCase.id == c.id) ? 'selected' : ''}>${c.name}</option>
            </c:forEach>
        </select>
    </form>
</div>

<!-- ═══ MAIN 3-PANEL SEARCH LAYOUT ═══ -->
<div class="search-layout">

    <!-- ─── LEFT: FILTER SIDEBAR ─── -->
    <div class="filter-sidebar">
        <div class="filter-sidebar-header">
            <h3 class="filter-sidebar-title">Filters</h3>
            <a href="javascript:;" class="filter-reset-link" onclick="removeAllSearch()">Reset</a>
        </div>

        <!-- File Type Filter -->
        <div class="filter-group">
            <div class="filter-group-header" onclick="toggleFilterGroup(this)">
                <i class="bi-file-earmark filter-group-icon"></i>
                <span class="filter-group-title">File Type</span>
                <i class="bi-chevron-up filter-chevron"></i>
            </div>
            <div class="filter-group-body" id="filetype-filter-body">
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="email" checked />
                    <span class="filter-cb-label">Email</span>
                    <span class="filter-cb-count" id="fc-email">0</span>
                </label>
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="pdf" checked />
                    <span class="filter-cb-label">PDF</span>
                    <span class="filter-cb-count" id="fc-pdf">0</span>
                </label>
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="text" checked />
                    <span class="filter-cb-label">Text</span>
                    <span class="filter-cb-count" id="fc-text">0</span>
                </label>
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="excel" checked />
                    <span class="filter-cb-label">Excel</span>
                    <span class="filter-cb-count" id="fc-excel">0</span>
                </label>
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="image" checked />
                    <span class="filter-cb-label">Image</span>
                    <span class="filter-cb-count" id="fc-image">0</span>
                </label>
                <label class="filter-checkbox-row">
                    <input type="checkbox" class="filter-cb" data-filter="filetype" value="other" checked />
                    <span class="filter-cb-label">Other</span>
                    <span class="filter-cb-count" id="fc-other">0</span>
                </label>
            </div>
        </div>

        <!-- Tags Filter -->
        <div class="filter-group">
            <div class="filter-group-header" onclick="toggleFilterGroup(this)">
                <i class="bi-tags filter-group-icon"></i>
                <span class="filter-group-title">Tags</span>
                <i class="bi-chevron-up filter-chevron"></i>
            </div>
            <div class="filter-group-body">
                <div class="case-tags-filter-list">
                    <!-- Tags populated dynamically -->
                </div>
                <a href="javascript:;" class="filter-show-more">Show more</a>
            </div>
        </div>

        <!-- Custodian Filter -->
        <div class="filter-group">
            <div class="filter-group-header" onclick="toggleFilterGroup(this)">
                <i class="bi-people filter-group-icon"></i>
                <span class="filter-group-title">Custodian</span>
                <i class="bi-chevron-up filter-chevron"></i>
            </div>
            <div class="filter-group-body" id="custodian-filter-body">
                <!-- Populated dynamically from search results -->
            </div>
        </div>

        <!-- Date Range Filter -->
        <div class="filter-group">
            <div class="filter-group-header" onclick="toggleFilterGroup(this)">
                <i class="bi-calendar3 filter-group-icon"></i>
                <span class="filter-group-title">Date Range</span>
                <i class="bi-chevron-up filter-chevron"></i>
            </div>
            <div class="filter-group-body">
                <input type="date" id="filter-date-start" class="filter-date-input" placeholder="Start date" style="margin-bottom:8px;width:100%;padding:4px;border:1px solid #cbd5e1;border-radius:4px" />
                <input type="date" id="filter-date-end" class="filter-date-input" placeholder="End date" style="width:100%;padding:4px;border:1px solid #cbd5e1;border-radius:4px" />
            </div>
        </div>

        <!-- Saved Searches -->
        <div class="filter-group">
            <div class="filter-group-header" onclick="toggleFilterGroup(this)">
                <i class="bi-bookmark filter-group-icon"></i>
                <span class="filter-group-title">Saved Searches</span>
                <i class="bi-chevron-up filter-chevron"></i>
            </div>
            <div class="filter-group-body">
                <a href="javascript:;" class="saved-search-link"><i class="bi-clock-history"></i> My Recent Searches</a>
                <a href="javascript:;" class="saved-search-link"><i class="bi-envelope-exclamation"></i> Important Emails</a>
                <a href="javascript:;" class="saved-search-link"><i class="bi-file-earmark-text"></i> Contracts Review</a>
                <a href="javascript:;" class="saved-search-link"><i class="bi-shield-check"></i> Privilege Review</a>
            </div>
        </div>

        <div class="filter-sidebar-footer">
            <span class="filter-version-label">v10.8.0</span>
            <a href="javascript:;" class="filter-collapse-btn" title="Collapse sidebar"><i class="bi-chevron-bar-left"></i></a>
        </div>
    </div>

    <!-- ─── CENTER: SEARCH + RESULTS ─── -->
    <div class="search-center">

        <!-- Search Tabs -->
        <div class="search-tabs-bar">
            <button class="search-tab search-tab-active" id="tab-search" onclick="switchSearchTab('search')">Search</button>
            <button class="search-tab" id="tab-tagsearch" onclick="switchSearchTab('tagsearch')">Search by Tags</button>
        </div>

        <!-- Search Input -->
        <div class="search-input-area">
            <div class="search-input-row">
                <input id="search-query" class="search-input-field" type="text" name="query" value="" placeholder="Enter keyword or query..." onkeypress="if(event.keyCode==13){event.preventDefault();search();}" />
                <input type="button" class="search-btn" name="Search" value="Search" onclick="search()"/>
                <button type="button" class="search-clear-btn" onclick="document.getElementById('search-query').value='';removeAllSearch();">Clear</button>
            </div>
            <div class="search-save-row">
                <a href="javascript:;" class="search-save-link"><i class="bi-star"></i> Save Search</a>
            </div>
        </div>

        <!-- Tags Search Panel (hidden by default) -->
        <div class="tags-search-panel" id="tags-search-panel" style="display:none;">
            <div class="case-tags-box-body"></div>
        </div>

        <!-- Active Keyword Chips -->
        <div id="result-ajax">
            <div class="delimiter3">
            </div>
        </div>
    </div>

    <!-- ─── RIGHT: DOCUMENT PREVIEW ─── -->
    <div class="preview-panel" id="preview-panel" style="display:none;">
        <div class="preview-panel-header">
            <span class="preview-doc-title" id="preview-doc-title">Document</span>
            <div class="preview-nav">
                <a href="javascript:;" onclick="prevDocument()" class="preview-nav-link"><i class="bi-chevron-left"></i> Previous</a>
                <span class="preview-nav-counter" id="preview-nav-counter">1 of 6</span>
                <a href="javascript:;" onclick="nextDocument()" class="preview-nav-link">Next <i class="bi-chevron-right"></i></a>
            </div>
            <button class="preview-close-btn" onclick="closePreviewPanel()"><i class="bi-x-lg"></i></button>
        </div>
        <!-- Viewer toolbar -->
        <div class="preview-viewer-toolbar">
            <button class="pvt-btn" title="Zoom In" onclick="previewZoomIn()"><i class="bi-zoom-in"></i></button>
            <button class="pvt-btn" title="Zoom Out" onclick="previewZoomOut()"><i class="bi-zoom-out"></i></button>
            <select class="pvt-zoom-select" onchange="previewZoomSet(this.value)"><option>100%</option><option>75%</option><option>125%</option><option>150%</option><option>200%</option></select>
            <div class="pvt-sep"></div>
            <button class="pvt-btn" title="Rotate Left" onclick="previewRotateLeft()"><i class="bi-arrow-counterclockwise"></i></button>
            <button class="pvt-btn" title="Rotate Right" onclick="previewRotateRight()"><i class="bi-arrow-clockwise"></i></button>
            <div class="pvt-sep"></div>
            <button class="pvt-btn" title="Fit Width" onclick="previewZoomSet(100)"><i class="bi-arrows-expand"></i></button>
            <button class="pvt-btn" title="Full Screen" onclick="previewFullscreen()"><i class="bi-fullscreen"></i></button>
            <div class="pvt-sep"></div>
            <button class="pvt-btn" title="Download" onclick="previewDownload()"><i class="bi-download"></i></button>
            <button class="pvt-btn" title="Print" onclick="previewPrint()"><i class="bi-printer"></i></button>
        </div>
        <!-- Tabs -->
        <div class="preview-tabs">
            <button class="preview-tab preview-tab-active" onclick="switchPreviewTab(this,'doc')">Document</button>
            <button class="preview-tab" onclick="switchPreviewTab(this,'text')">Text/OCR</button>
            <button class="preview-tab" onclick="switchPreviewTab(this,'meta')">Metadata</button>
            <button class="preview-tab" id="preview-tags-tab" onclick="switchPreviewTab(this,'tags')">Tags (<span id="preview-tags-count">0</span>)</button>
            <button class="preview-tab" id="preview-notes-tab" onclick="switchPreviewTab(this,'notes')">Notes (<span id="preview-notes-count">0</span>)</button>
        </div>
        <!-- Two-column body: document + metadata -->
        <div class="preview-split">
            <div class="preview-doc-area" id="preview-panel-body">
                <!-- Document content loaded dynamically -->
            </div>
            <div class="preview-meta-area" id="preview-meta-area">
                <div class="preview-meta-section">
                    <h4 class="preview-meta-title">EMAIL DETAILS</h4>
                    <div id="pm-email-details">
                        <div class="preview-meta-row"><span class="preview-meta-key">From</span><span class="preview-meta-val" id="pm-from">-</span></div>
                        <div class="preview-meta-row"><span class="preview-meta-key">To</span><span class="preview-meta-val" id="pm-to">-</span></div>
                        <div class="preview-meta-row"><span class="preview-meta-key">Cc</span><span class="preview-meta-val" id="pm-cc">-</span></div>
                        <div class="preview-meta-row"><span class="preview-meta-key">Bcc</span><span class="preview-meta-val" id="pm-bcc">-</span></div>
                        <div class="preview-meta-row"><span class="preview-meta-key">Date</span><span class="preview-meta-val" id="pm-date">-</span></div>
                        <div class="preview-meta-row"><span class="preview-meta-key">Subject</span><span class="preview-meta-val" id="pm-subject">-</span></div>
                    </div>
                </div>
                <div class="preview-meta-section">
                    <h4 class="preview-meta-title">FILE DETAILS</h4>
                    <div class="preview-meta-row"><span class="preview-meta-key">File Type</span><span class="preview-meta-val" id="pm-filetype">-</span></div>
                    <div class="preview-meta-row"><span class="preview-meta-key">File Size</span><span class="preview-meta-val" id="pm-filesize">-</span></div>
                    <div class="preview-meta-row"><span class="preview-meta-key">Created</span><span class="preview-meta-val" id="pm-created">-</span></div>
                    <div class="preview-meta-row"><span class="preview-meta-key">Modified</span><span class="preview-meta-val" id="pm-modified">-</span></div>
                    <div class="preview-meta-row"><span class="preview-meta-key">MD5 Hash</span><span class="preview-meta-val" id="pm-hash">-</span></div>
                </div>
                <div class="preview-meta-section">
                    <h4 class="preview-meta-title">CUSTODIAN</h4>
                    <div class="preview-meta-row"><span class="preview-meta-val" id="pm-custodian">-</span></div>
                </div>
                <div class="preview-meta-section">
                    <h4 class="preview-meta-title">COLLECTION</h4>
                    <div class="preview-meta-row"><span class="preview-meta-val" id="pm-collection">-</span></div>
                </div>
                <div class="preview-meta-section">
                    <h4 class="preview-meta-title">PATH</h4>
                    <div class="preview-meta-row"><span class="preview-meta-val pm-path" id="pm-path">-</span></div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
// Force an automatic search on page load to display all results for the case
$(document).ready(function() {
    setTimeout(function() {
        var queryStr = $("#search-query").val();
        if (!queryStr || queryStr.trim() === '') {
            $("#search-query").val('*');
        }
        search();
        // Clear the visible * after triggering
        if ($("#search-query").val() === '*') {
            $("#search-query").val('');
        }
    }, 100);
});

/* ───── Filter sidebar collapse/expand ───── */
function toggleFilterGroup(el) {
    var body = el.nextElementSibling;
    var chevron = el.querySelector('.filter-chevron');
    if (body.style.display === 'none') {
        body.style.display = 'block';
        chevron.className = 'bi-chevron-up filter-chevron';
    } else {
        body.style.display = 'none';
        chevron.className = 'bi-chevron-down filter-chevron';
    }
}

/* ───── Highlight search keywords in results grid ───── */
function highlightSearchResults() {
    // Collect active search keywords from chips
    var keywords = [];
    var chips = document.querySelectorAll('.search-keyword-value');
    for (var i = 0; i < chips.length; i++) {
        var raw = chips[i].textContent.trim();
        // strip "Keyword: " prefix if present
        var val = raw.replace(/^Keyword:\s*/i, '').trim();
        if (val && val !== '*') keywords.push(val);
    }
    if (keywords.length === 0) return;

    // Sort by length descending so longer phrases match first
    keywords.sort(function(a, b) { return b.length - a.length; });
    // Build regex - escape special chars
    var kwRegex = keywords.map(function(kw) {
        return kw.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
    }).join('|');
    var regex = new RegExp('\\b(' + kwRegex + ')\\b', 'gi');

    // 1. Highlight in grid cells (name, custodian, date, id)
    var cells = document.querySelectorAll('.results-cell-name, .results-cell-custodian, .results-cell-date, .results-cell-id');
    for (var c = 0; c < cells.length; c++) {
        var txt = cells[c].textContent;
        if (regex.test(txt)) {
            regex.lastIndex = 0;
            cells[c].innerHTML = txt.replace(regex, '<mark class="search-hit-mark">$1</mark>');
        }
    }

    // 2. Add snippet rows with keyword-in-context from hidden detail panels
    var rows = document.querySelectorAll('.results-row');
    for (var r = 0; r < rows.length; r++) {
        var rowId = rows[r].id; // e.g. "row-xxx"
        var docId = rowId.replace('row-', '');
        var docBox = document.getElementById('doc-' + docId);
        if (!docBox) continue;

        // Get the text content from the hidden detail panel
        var textEl = docBox.querySelector('.result-box-text');
        var docText = textEl ? textEl.textContent.trim() : '';
        if (!docText) continue;

        // Find the first keyword match and extract a snippet around it
        regex.lastIndex = 0;
        var match = regex.exec(docText);
        if (!match) continue;

        // Mark this row as having a hit
        rows[r].classList.add('results-row-has-hit');

        // Build snippet: ~80 chars before and after the match
        var start = Math.max(0, match.index - 80);
        var end = Math.min(docText.length, match.index + match[0].length + 80);
        var snippet = (start > 0 ? '...' : '') + docText.substring(start, end) + (end < docText.length ? '...' : '');

        // Escape HTML then highlight keywords
        var escaped = snippet.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        regex.lastIndex = 0;
        var highlighted = escaped.replace(regex, '<mark class="search-hit-mark">$1</mark>');

        // Insert snippet row after the result row
        var snippetRow = document.createElement('tr');
        snippetRow.className = 'results-snippet-row';
        snippetRow.onclick = rows[r].onclick;
        var colSpan = rows[r].querySelectorAll('td').length;
        snippetRow.innerHTML = '<td colspan="' + colSpan + '" class="results-snippet-cell"><i class="bi-search" style="margin-right:6px;font-size:10px;opacity:.5"></i>' + highlighted + '</td>';
        rows[r].parentNode.insertBefore(snippetRow, rows[r].nextSibling);
    }
}

/* ───── Update filter counts from actual results ───── */
function updateFilterCounts() {
    var rows = document.querySelectorAll('.results-row');
    // File type counts
    var typeMap = { email:['eml','msg'], pdf:['pdf'], text:['txt'], excel:['xls','xlsx','csv'], image:['jpg','jpeg','png','gif','tiff','tif','bmp'] };
    var typeCounts = { email:0, pdf:0, text:0, excel:0, image:0, other:0 };
    // Custodian counts
    var custCounts = {};

    for (var r = 0; r < rows.length; r++) {
        // File type
        var badge = rows[r].querySelector('.file-type-badge');
        if (badge) {
            var ext = badge.textContent.trim().toLowerCase();
            var matched = false;
            for (var t in typeMap) {
                if (typeMap[t].indexOf(ext) >= 0) { typeCounts[t]++; matched = true; break; }
            }
            if (!matched) typeCounts.other++;
        }
        // Custodian
        var custCell = rows[r].querySelector('.results-cell-custodian');
        if (custCell) {
            var cust = custCell.textContent.trim();
            if (cust && cust.length > 0) {
                custCounts[cust] = (custCounts[cust] || 0) + 1;
            }
        }
    }

    // Update file type counts
    for (var ft in typeCounts) {
        var el = document.getElementById('fc-' + ft);
        if (el) el.textContent = typeCounts[ft];
    }
    // Hide file type rows with 0 count (optional: show all but grey out)
    var ftRows = document.querySelectorAll('#filetype-filter-body .filter-checkbox-row');
    for (var i = 0; i < ftRows.length; i++) {
        var cb = ftRows[i].querySelector('.filter-cb');
        if (cb) {
            var count = typeCounts[cb.value] || 0;
            ftRows[i].style.display = count > 0 ? '' : 'none';
        }
    }

    // Build custodian checkboxes dynamically
    var custBody = document.getElementById('custodian-filter-body');
    if (custBody) {
        custBody.innerHTML = '';
        var sorted = Object.keys(custCounts).sort(function(a,b){ return custCounts[b]-custCounts[a]; });
        for (var s = 0; s < sorted.length; s++) {
            var name = sorted[s];
            var label = document.createElement('label');
            label.className = 'filter-checkbox-row';
            label.innerHTML = '<input type="checkbox" class="filter-cb" data-filter="custodian" value="' + name.toLowerCase().replace(/"/g,'') + '" checked />' +
                '<span class="filter-cb-label">' + name + '</span>' +
                '<span class="filter-cb-count">' + custCounts[name] + '</span>';
            custBody.appendChild(label);
        }
        // Re-attach change listeners
        $(custBody).find('.filter-cb').on('change', applyAllFilters);
    }

    // Update tag counts in sidebar
    var tagCounts = {};
    var tagBadges = document.querySelectorAll('.results-cell-tags .tag-badge');
    for (var tb = 0; tb < tagBadges.length; tb++) {
        var titleAttr = tagBadges[tb].getAttribute('title') || '';
        var tv = titleAttr.replace('Filter by ', '').trim();
        if (tv) tagCounts[tv] = (tagCounts[tv] || 0) + 1;
    }
    var tagRows = document.querySelectorAll('.case-tags-filter-list .filter-tag-row');
    for (var tr = 0; tr < tagRows.length; tr++) {
        var tagLabel = tagRows[tr].querySelector('.filter-cb-label');
        var tagCount = tagRows[tr].querySelector('.filter-cb-count');
        if (tagLabel && tagCount) {
            tagCount.textContent = tagCounts[tagLabel.textContent.trim()] || 0;
        }
    }
}

/* ───── Search / Tags tabs ───── */
function switchSearchTab(tab) {
    document.getElementById('tab-search').classList.remove('search-tab-active');
    document.getElementById('tab-tagsearch').classList.remove('search-tab-active');
    document.getElementById('tab-' + tab).classList.add('search-tab-active');
    document.getElementById('tags-search-panel').style.display = (tab === 'tagsearch') ? 'block' : 'none';
}

/* ───── Close preview panel ───── */
function closePreviewPanel() {
    document.getElementById('preview-panel').style.display = 'none';
}

/* ───── Preview zoom state ───── */
var _previewZoom = 100;
var _previewRotation = 0;

/* ───── Preview Tab switching (fully functional) ───── */
function switchPreviewTab(el, tabId) {
    // Highlight active tab
    var tabs = el.parentNode.querySelectorAll('.preview-tab');
    for (var i = 0; i < tabs.length; i++) tabs[i].classList.remove('preview-tab-active');
    el.classList.add('preview-tab-active');

    var docArea = document.getElementById('preview-panel-body');
    var metaArea = document.getElementById('preview-meta-area');
    if (!docArea) return;

    if (tabId === 'doc') {
        // Show the document iframe (restore it if it was replaced)
        if (window._savedDocContent) {
            docArea.innerHTML = '';
            docArea.appendChild(window._savedDocContent);
        }
        metaArea.style.display = '';
    }
    else if (tabId === 'text') {
        // Save current doc content
        _saveDocContent(docArea);
        metaArea.style.display = 'none';
        
        var textContent = _getDocText();
        var escapedText = '';
        var keywords = [];
        var activeChips = document.querySelectorAll('.search-keyword-value');
        for (var i = 0; i < activeChips.length; i++) {
            var val = activeChips[i].textContent.trim();
            if (val && val !== '*') keywords.push(val);
        }
        var searchInput = document.getElementById('search-query');
        if (searchInput && searchInput.value.trim() && searchInput.value.trim() !== '*') {
            keywords.push(searchInput.value.trim());
        }

        if (textContent && keywords.length > 0) {
            // Sort keywords by length descending to match longest phrases first
            keywords.sort(function(a, b) { return b.length - a.length; });
            var kwRegexStr = keywords.map(function(kw) { 
                return kw.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"); 
            }).join('|');
            var regex = new RegExp('\\b(' + kwRegexStr + ')\\b', 'gi');
            
            var parts = textContent.split(regex);
            for (var p = 0; p < parts.length; p++) {
                if (p % 2 === 1) { // Matched part
                    escapedText += '<mark style="background-color: #fef08a; padding: 0 2px; border-radius: 2px; font-weight: bold; color: #854d0e;">' + _escapeHtml(parts[p]) + '</mark>';
                } else { // Non-matched part
                    escapedText += _escapeHtml(parts[p]);
                }
            }
        } else {
            escapedText = _escapeHtml(textContent || 'No text content available for this document.');
        }

        // Show extracted text from hidden detail panel
        docArea.innerHTML = '<div class="preview-tab-content"><h4 class="ptc-title">Extracted Text / OCR</h4>' +
            '<pre class="ptc-text" style="white-space: pre-wrap; word-wrap: break-word;">' + escapedText + '</pre></div>';
    }
    else if (tabId === 'meta') {
        _saveDocContent(docArea);
        metaArea.style.display = 'none';
        // Build metadata table from hidden detail panel
        var meta = _getAllMeta();
        var html = '<div class="preview-tab-content"><h4 class="ptc-title">Document Metadata</h4><table class="ptc-meta-table">';
        html += '<tr><th>Key</th><th>Value</th></tr>';
        for (var key in meta) {
            html += '<tr><td>' + _escapeHtml(key) + '</td><td>' + _escapeHtml(meta[key]) + '</td></tr>';
        }
        html += '</table></div>';
        docArea.innerHTML = html;
    }
    else if (tabId === 'tags') {
        _saveDocContent(docArea);
        metaArea.style.display = 'none';
        // Build tag management UI
        var docId = lastDocId;
        var html = '<div class="preview-tab-content"><h4 class="ptc-title">Document Tags</h4>';
        html += '<div class="ptc-tag-list" id="ptc-tag-list">';
        if (docId) {
            var tagCell = document.getElementById('tags-cell-' + docId);
            var tagEls = tagCell ? tagCell.querySelectorAll('.tag-badge') : [];
            for (var i = 0; i < tagEls.length; i++) {
                var tv = (tagEls[i].getAttribute('title') || '').replace('Filter by ', '').trim();
                if (!tv) continue;
                html += '<div class="ptc-tag-item"><span class="tag-badge tag-badge-' + tv.toLowerCase().replace(/\s+/g,'-') + '">' + _escapeHtml(tv) + '</span>' +
                    '<button class="ptc-tag-remove" onclick="removeDocTagFromPreview(\'' + _escapeHtml(docId) + '\',\'' + _escapeHtml(tv) + '\',this)" title="Remove">&times;</button></div>';
            }
            if (tagEls.length === 0) html += '<p class="ptc-empty">No tags assigned yet.</p>';
        }
        html += '</div>';
        html += '<div style="margin-top: 16px;">';
        html += '<div class="ptc-add-tag" style="display:flex; gap:8px; margin-bottom:12px;">' +
            '<select id="ptc-predefined-tag-select" class="form-control" style="flex:1; padding:6px 12px; border:1px solid #cbd5e1; border-radius:4px; font-size:13px;" onchange="if(this.value){applyQuickTag(this.value);this.value=\'\';switchPreviewTab(document.querySelector(\'#preview-tags-tab\'),\'tags\')}">' +
            '<option value="">Select predefined tag...</option>';
        var quickTags = ['Responsive','Privileged','Hot','Needs Review','Confidential','Relevant'];
        for (var q = 0; q < quickTags.length; q++) {
            html += '<option value="' + quickTags[q] + '">' + quickTags[q] + '</option>';
        }
        html += '</select></div>';
        html += '<div class="ptc-add-tag" style="display:flex; gap:8px;">' +
            '<input type="text" class="ptc-tag-input" id="ptc-tag-input" style="flex:1" placeholder="Or type a custom tag..." onkeypress="if(event.keyCode===13){addTagFromPreview();}" />' +
            '<button class="tag-action-btn" style="white-space:nowrap" onclick="addTagFromPreview()">Add Tag</button></div></div></div>';
        docArea.innerHTML = html;
    }
    else if (tabId === 'notes') {
        _saveDocContent(docArea);
        metaArea.style.display = 'none';
        var html = '<div class="preview-tab-content"><h4 class="ptc-title">Notes</h4>';
        html += '<div class="ptc-notes-list" id="ptc-notes-list">';
        var notes = window._docNotes && window._docNotes[lastDocId] ? window._docNotes[lastDocId] : [];
        if (notes.length === 0) html += '<p class="ptc-empty">No notes yet. Add one below.</p>';
        for (var n = 0; n < notes.length; n++) {
            html += '<div class="ptc-note-item"><div class="ptc-note-text">' + _escapeHtml(notes[n].text) + '</div><div class="ptc-note-time">' + notes[n].time + '</div></div>';
        }
        html += '</div>';
        html += '<div class="ptc-add-note"><textarea class="ptc-note-textarea" id="ptc-note-textarea" placeholder="Write a note..." rows="3"></textarea>' +
            '<button class="tag-action-btn" onclick="addNoteFromPreview()">Save Note</button></div></div>';
        docArea.innerHTML = html;
    }
}

function _saveDocContent(docArea) {
    if (!window._savedDocContent && docArea.firstChild && !docArea.querySelector('.preview-tab-content')) {
        var frag = document.createDocumentFragment();
        while (docArea.firstChild) frag.appendChild(docArea.firstChild);
        window._savedDocContent = frag;
    }
}

function _getDocText() {
    if (!lastDocId) return '';
    var docBox = document.getElementById('doc-' + lastDocId);
    if (!docBox) return '';
    var textEl = docBox.querySelector('.result-box-text');
    return textEl ? textEl.textContent.trim() : '';
}

function _getAllMeta() {
    var meta = {};
    if (!lastDocId) return meta;
    var docBox = document.getElementById('doc-' + lastDocId);
    if (!docBox) return meta;
    var entries = docBox.querySelectorAll('.result-div table tr');
    for (var i = 0; i < entries.length; i++) {
        var cells = entries[i].querySelectorAll('td');
        if (cells.length >= 2 && cells[0].className === 'result-box-key') {
            meta[cells[0].textContent.trim()] = cells[1].textContent.trim();
        }
    }
    return meta;
}

function _escapeHtml(str) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

/* ───── Tag management from preview Tags tab ───── */
function addTagFromPreview() {
    var input = document.getElementById('ptc-tag-input');
    if (!input || !input.value.trim()) return;
    applyQuickTag(input.value.trim());
    input.value = '';
    // Refresh the tags tab after a short delay
    setTimeout(function() {
        var tagsTab = document.getElementById('preview-tags-tab');
        if (tagsTab) switchPreviewTab(tagsTab, 'tags');
    }, 500);
}

function removeDocTagFromPreview(docId, tag, btn) {
    if (typeof removeDocTagAjax === 'function') {
        removeDocTagAjax(docId, tag, btn);
    }
}

/* ───── Notes ───── */
if (!window._docNotes) window._docNotes = {};

function addNoteFromPreview() {
    var ta = document.getElementById('ptc-note-textarea');
    if (!ta || !ta.value.trim() || !lastDocId) return;
    if (!window._docNotes[lastDocId]) window._docNotes[lastDocId] = [];
    window._docNotes[lastDocId].push({ text: ta.value.trim(), time: new Date().toLocaleString() });
    ta.value = '';
    // Also save via annotation toolbar if the note input is there
    var annoInput = document.querySelector('.anno-note-input');
    if (annoInput) annoInput.value = '';
    
    // Update count
    var notesCountEl = document.getElementById('preview-notes-count');
    if (notesCountEl) notesCountEl.textContent = window._docNotes[lastDocId].length;

    // Refresh notes tab
    var notesTab = document.getElementById('preview-notes-tab');
    if (notesTab && notesTab.classList.contains('preview-tab-active')) switchPreviewTab(notesTab, 'notes');
}

/* ───── Viewer Toolbar: Zoom ───── */
function previewZoomIn() {
    _previewZoom = Math.min(_previewZoom + 25, 300);
    _applyPreviewTransform();
    _updateZoomSelect();
}
function previewZoomOut() {
    _previewZoom = Math.max(_previewZoom - 25, 25);
    _applyPreviewTransform();
    _updateZoomSelect();
}
function previewZoomSet(val) {
    _previewZoom = parseInt(val) || 100;
    _applyPreviewTransform();
}
function _updateZoomSelect() {
    var sel = document.querySelector('.pvt-zoom-select');
    if (sel) sel.value = _previewZoom + '%';
}

/* ───── Viewer Toolbar: Rotate ───── */
function previewRotateLeft() {
    _previewRotation = (_previewRotation - 90) % 360;
    _applyPreviewTransform();
}
function previewRotateRight() {
    _previewRotation = (_previewRotation + 90) % 360;
    _applyPreviewTransform();
}

function _applyPreviewTransform() {
    var docArea = document.getElementById('preview-panel-body');
    if (!docArea) return;
    var el = docArea.querySelector('iframe') || docArea.querySelector('img') || docArea.querySelector('pre') || docArea.querySelector('canvas');
    if (!el) return;
    el.style.transform = 'scale(' + (_previewZoom / 100) + ') rotate(' + _previewRotation + 'deg)';
    el.style.transformOrigin = 'top left';
}

/* ───── Viewer Toolbar: Fullscreen ───── */
function previewFullscreen() {
    var docArea = document.getElementById('preview-panel-body');
    if (!docArea) return;
    if (docArea.requestFullscreen) docArea.requestFullscreen();
    else if (docArea.webkitRequestFullscreen) docArea.webkitRequestFullscreen();
}

/* ───── Viewer Toolbar: Download ───── */
function previewDownload() {
    if (!lastDocId) return;
    var docBox = document.getElementById('doc-' + lastDocId);
    if (!docBox) return;
    var previewLink = docBox.querySelector('.html-preview');
    if (previewLink) {
        var docPath = previewLink.getAttribute('data');
        var uid = previewLink.getAttribute('uid');
        window.open('filedownload.html?action=exportNative&docPath=' + docPath + '&uniqueId=' + uid, '_blank');
    }
}

/* ───── Viewer Toolbar: Print ───── */
function previewPrint() {
    var docArea = document.getElementById('preview-panel-body');
    if (!docArea) return;
    var iframe = docArea.querySelector('iframe');
    if (iframe && iframe.contentWindow) {
        iframe.contentWindow.print();
    } else {
        window.print();
    }
}

/* ───── Apply All Client-Side Filters (File Type, Custodian, Date) ───── */
function applyAllFilters() {
    var rows = document.querySelectorAll('.results-row');
    if (!rows || rows.length === 0) return;

    // File Type Filter State
    var ftChecked = document.querySelectorAll('.filter-cb[data-filter="filetype"]:checked');
    var ftAll = document.querySelectorAll('.filter-cb[data-filter="filetype"]');
    var types = [];
    for (var i = 0; i < ftChecked.length; i++) types.push(ftChecked[i].value);
    var filterFileType = (types.length > 0 && types.length < ftAll.length);
    var typeMap = { 'email': ['eml','msg'], 'pdf': ['pdf'], 'text': ['txt'], 'excel': ['xls','xlsx','csv'], 'image': ['jpg','jpeg','png','gif','tiff','tif','bmp'] };
    var allowedExts = [];
    if (filterFileType) {
        for (var t = 0; t < types.length; t++) {
            if (typeMap[types[t]]) allowedExts = allowedExts.concat(typeMap[types[t]]);
        }
    }

    // Custodian Filter State
    var custChecked = document.querySelectorAll('.filter-cb[data-filter="custodian"]:checked');
    var custAll = document.querySelectorAll('.filter-cb[data-filter="custodian"]');
    var names = [];
    for (var i = 0; i < custChecked.length; i++) names.push(custChecked[i].value.toLowerCase());
    var filterCustodian = (names.length > 0 && names.length < custAll.length);

    // Date Range Filter State
    var startDateInputs = document.querySelectorAll('.filter-date-input');
    var startDate = null;
    var endDate = null;
    if (startDateInputs.length >= 2) {
        if (startDateInputs[0].value) startDate = new Date(startDateInputs[0].value);
        if (startDateInputs[1].value) {
            endDate = new Date(startDateInputs[1].value);
            endDate.setHours(23, 59, 59, 999);
        }
    }
    var filterDate = (startDate !== null || endDate !== null);

    // Apply to rows
    for (var r = 0; r < rows.length; r++) {
        var show = true;

        if (filterFileType && show) {
            var badge = rows[r].querySelector('.file-type-badge');
            if (badge) {
                var ext = badge.textContent.trim().toLowerCase();
                var matched = allowedExts.indexOf(ext) >= 0;
                if (!matched && types.indexOf('other') >= 0) {
                    // Check if it's considered 'other'
                    var isKnown = false;
                    for (var k in typeMap) { if (typeMap[k].indexOf(ext) >= 0) isKnown = true; }
                    if (!isKnown) matched = true;
                }
                if (!matched) show = false;
            }
        }

        if (filterCustodian && show) {
            var custCell = rows[r].querySelector('.results-cell-custodian');
            if (custCell) {
                var custName = custCell.textContent.trim().toLowerCase();
                var cMatch = false;
                for (var n = 0; n < names.length; n++) {
                    if (custName.indexOf(names[n]) >= 0) { cMatch = true; break; }
                }
                if (!cMatch) show = false;
            }
        }

        if (filterDate && show) {
            var dateCell = rows[r].querySelector('.results-cell-date');
            if (dateCell) {
                var rowDateStr = dateCell.textContent.trim();
                if (rowDateStr && rowDateStr !== '-') {
                    var rowDate = new Date(rowDateStr);
                    if (!isNaN(rowDate.getTime())) {
                        if (startDate && rowDate < startDate) show = false;
                        if (endDate && rowDate > endDate) show = false;
                    }
                }
            }
        }

        rows[r].style.display = show ? '' : 'none';
    }
}

/* ───── Annotation toolbar: Save Note ───── */
function saveAnnotationNote() {
    var input = document.querySelector('.anno-note-input');
    if (!input || !input.value.trim() || !lastDocId) { alert('Please select a document and enter a note.'); return; }
    if (!window._docNotes) window._docNotes = {};
    if (!window._docNotes[lastDocId]) window._docNotes[lastDocId] = [];
    window._docNotes[lastDocId].push({ text: input.value.trim(), time: new Date().toLocaleString() });
    input.value = '';
    // Visual feedback
    var btn = document.querySelector('.anno-save-note-btn');
    if (btn) { btn.textContent = 'Saved!'; setTimeout(function(){ btn.textContent = 'Save Note'; }, 1500); }
    
    // Update count
    var notesCountEl = document.getElementById('preview-notes-count');
    if (notesCountEl) notesCountEl.textContent = window._docNotes[lastDocId].length;

    // Refresh notes tab
    var notesTab = document.getElementById('preview-notes-tab');
    if (notesTab && notesTab.classList.contains('preview-tab-active')) switchPreviewTab(notesTab, 'notes');
}

/* ───── Reset preview zoom/rotation on new doc ───── */
function resetPreviewTransform() {
    _previewZoom = 100;
    _previewRotation = 0;
    _updateZoomSelect();
    window._savedDocContent = null;
}

/* ───── Populate tag filters from allTags ───── */
$(document).ready(function() {
    var tagColors = {
        'Responsive': '#10b981',
        'Privileged': '#f59e0b',
        'Hot': '#ef4444',
        'Confidential': '#8b5cf6',
        'Needs Review': '#06b6d4',
        'Relevant': '#3b82f6'
    };
    var defaultColor = '#6b7280';
    var container = $('.case-tags-filter-list');
    for (var t in allTags) {
        var color = tagColors[t] || defaultColor;
        container.append(
            '<div class="filter-tag-row" onclick="addTagToSearch(\'' + t.replace(/'/g, "\\'") + '\')">' +
            '<span class="filter-tag-dot" style="background:' + color + '"></span>' +
            '<span class="filter-cb-label">' + t + '</span>' +
            '<span class="filter-cb-count"></span>' +
            '</div>'
        );
    }

    // Attach filter change listeners
    $(document).on('change', '.filter-cb[data-filter="filetype"]', applyAllFilters);
    $(document).on('change', '.filter-cb[data-filter="custodian"]', applyAllFilters);
    $(document).on('change', '.filter-date-input', applyAllFilters);

    // Close dropdowns when clicking outside
    $(document).on('click', function(e) {
        if (!$(e.target).closest('.toolbar-dropdown').length) {
            $('.toolbar-dropdown-menu').hide();
        }
    });

    // Layout view buttons toggle
    $(document).on('click', '.results-view-btn', function() {
        $('.results-view-btn').removeClass('results-view-active');
        $(this).addClass('results-view-active');
        var isCompact = $(this).find('.bi-layout-text-sidebar-reverse').length > 0;
        if (isCompact) {
            $('.results-table').addClass('compact-view');
        } else {
            $('.results-table').removeClass('compact-view');
        }
    });
});
</script>