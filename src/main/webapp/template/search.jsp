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

<!-- ═══ MAIN 2-PANEL SEARCH LAYOUT ═══ -->
<div class="search-layout">

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
        <!-- Tabs -->
        <div class="preview-tabs">
            <button class="preview-tab preview-tab-active" onclick="switchPreviewTab(this,'doc')">Document</button>
            <button class="preview-tab" onclick="switchPreviewTab(this,'text')">Text/OCR</button>
            <button class="preview-tab" onclick="switchPreviewTab(this,'meta')">Metadata</button>
            <button class="preview-tab" id="preview-tags-tab" onclick="switchPreviewTab(this,'tags')">Tags (<span id="preview-tags-count">0</span>)</button>
            <button class="preview-tab" id="preview-notes-tab" onclick="switchPreviewTab(this,'notes')">Notes (<span id="preview-notes-count">0</span>)</button>
        </div>
        <!-- Document preview body -->
        <div class="preview-panel-body" id="preview-panel-body">
            <!-- Document content loaded dynamically -->
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

/* ───── Preview Tab switching ───── */
function switchPreviewTab(el, tabId) {
    // Highlight active tab
    var tabs = el.parentNode.querySelectorAll('.preview-tab');
    for (var i = 0; i < tabs.length; i++) tabs[i].classList.remove('preview-tab-active');
    el.classList.add('preview-tab-active');

    var docArea = document.getElementById('preview-panel-body');
    if (!docArea) return;

    if (tabId === 'doc') {
        // Show the document iframe (restore it if it was replaced)
        if (window._savedDocContent) {
            docArea.innerHTML = '';
            docArea.appendChild(window._savedDocContent);
        }
    }
    else if (tabId === 'text') {
        // Save current doc content
        _saveDocContent(docArea);
        
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
        var quickTags = ['Responsive','Privileged','Hot','Needs Review','Confidential'];
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

/* ───── Reset preview on new doc ───── */
function resetPreviewTransform() {
    window._savedDocContent = null;
}

/* ───── Populate tag filters from allTags ───── */
$(document).ready(function() {
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