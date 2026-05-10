var lastDocId = null;
var documentsMap = new Object();
var allTags = new Object();
var documentInfoMap = new Object();

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
    loadInlinePreview(docId);
}

function initPage(docId) {
    selectDocument(docId);
    initTags();
}

function loadInlinePreview(docId) {
    var container = $("#inline-preview-" + docId);
    // Don't reload if already loaded
    if (container.data('loaded')) {
        return;
    }

    var info = documentInfoMap[docId];
    if (!info) {
        container.html('<div class="inline-preview-loading">No preview available</div>');
        return;
    }

    var docPath = info.documentPath;
    var uniqueId = info.uniqueId;
    var docName = info.documentName;

    var parts = docPath.split('.');
    var extension = '';
    if (parts.length > 1) {
        extension = parts.pop().toLowerCase();
    }

    if (extension == 'pdf') {
        var url = 'filedownload.html?action=exportNative&ispreviewpdf=1&docPath=' + docPath + '&uniqueId=' + uniqueId;
        container.html('<iframe src="' + url + '" style="width:100%;height:calc(100vh - 520px);border:none;"></iframe>');
        container.data('loaded', true);
    } else if (extension == 'jpg' || extension == 'jpeg' || extension == 'png') {
        var url = 'filedownload.html?action=exportNative&ispreviewimage=1&docPath=' + docPath + '&uniqueId=' + uniqueId;
        container.html('<div style="text-align:center;max-height:calc(100vh - 520px);overflow-y:auto;"><img src="' + url + '" style="max-width:100%;height:auto;"/></div>');
        container.data('loaded', true);
    } else if (extension == 'tiff' || extension == 'tif') {
        var url = 'filedownload.html?action=exportNative&ispreviewimage=1&docPath=' + docPath + '&uniqueId=' + uniqueId;
        container.html('<div class="inline-preview-loading">Loading TIFF preview...</div>');
        fetch(url)
            .then(function(response) { return response.arrayBuffer(); })
            .then(function(buffer) {
                var tiff = new Tiff({ buffer: buffer });
                var canvas = tiff.toCanvas();
                canvas.style.maxWidth = '100%';
                canvas.style.height = 'auto';
                container.html('');
                container.css({'text-align': 'center', 'max-height': 'calc(100vh - 520px)', 'overflow-y': 'auto'});
                container[0].appendChild(canvas);
                container.data('loaded', true);
            })
            .catch(function(error) {
                container.html('<div class="inline-preview-loading">Error loading TIFF preview</div>');
            });
    } else if (extension == 'txt') {
        var url = 'filedownload.html?action=exportNative&ispreviewpdf=1&docPath=' + docPath + '&uniqueId=' + uniqueId;
        fetch(url)
            .then(function(response) { return response.text(); })
            .then(function(text) {
                container.html('<pre style="white-space:pre-wrap;word-wrap:break-word;max-height:calc(100vh - 520px);overflow-y:auto;padding:10px;background:#f8f9fa;border:1px solid #ddd;">' + $('<span>').text(text).html() + '</pre>');
                container.data('loaded', true);
            })
            .catch(function(error) {
                container.html('<div class="inline-preview-loading">Error loading preview</div>');
            });
    } else {
        // HTML preview (e.g., .eml files)
        $.ajax({
            type: 'GET',
            url: 'filedownload.html',
            data: {action: 'exportHtml', docPath: docPath, uniqueId: uniqueId, docName: docName},
            success: function (data) {
                var iframe = document.createElement('iframe');
                iframe.style.width = '100%';
                iframe.style.height = 'calc(100vh - 520px)';
                iframe.style.border = 'none';
                container.html('');
                container[0].appendChild(iframe);
                var iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                iframeDoc.open();
                iframeDoc.write(data);
                iframeDoc.close();
                container.data('loaded', true);
            },
            error: function () {
                container.html('<div class="inline-preview-loading">Error loading preview</div>');
            }
        });
    }
}

function showMetadataModal(docId) {
    var content = $('#metadata-content-' + docId).html();
    $('#metadata_modal_content').html(content);
    $('#metadata-modal-title').text('Metadata - ' + docId);
    $('#metadata_modal').modal('show');
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

function buildDocumentInfoMap() {
    documentInfoMap = new Object();
    for (var i = 0; i < documents.length; i++) {
        var doc = documents[i];
        documentInfoMap[doc.documentId] = doc;
    }
}

function search() {

    var queryStr = $("#search-query").val();

    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'search', query: queryStr},
        success: function (data) {
            lastDocId = null;

            $("#result-ajax").html(data);
            buildDocumentInfoMap();

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
            buildDocumentInfoMap();

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
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
            buildDocumentInfoMap();

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
            if(fromNavigation)
            {
                currentIndex = lastSelectedPage < page ? 0 : documents.length - 1;
                var docId = documents[currentIndex].documentId;
                selectDocument(docId);
            }
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
            buildDocumentInfoMap();

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
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
            lastDocId = null;

            $("#result-ajax").html(data);
            buildDocumentInfoMap();

            var solrId = $("#solrid").val();
            if (solrId != null) {
                initPage(solrId);
            }
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

function loadIframeContent(htmlContent) {
    // Create the iframe element
    const iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = 'calc(100vh - 240px)';
    iframe.style.border = 'none';

    // Insert the iframe into the modal content
    const modalContent = document.querySelector('#html_preview_modal_content');
    modalContent.appendChild(iframe);
    // Write the HTML content to the iframe
    const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
    iframeDoc.open();
    iframeDoc.write(htmlContent);
    iframeDoc.close();

    $('#html_preview_modal').modal('show');
}

function loadPdfInIframe(pdfUrl) {
    // Create the iframe element
    const iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = 'calc(100vh - 240px)';
    iframe.style.border = 'none';
    iframe.src = pdfUrl;

    // Insert the iframe into the modal content
    const modalContent = document.querySelector('#html_preview_modal_content');
    modalContent.appendChild(iframe);
    $('#html_preview_modal').modal('show');
}

function loadImageInIframe(imageUrl) {
    const modalContent = document.querySelector('#html_preview_modal_content');
    modalContent.innerHTML = ''; // Clear previous content
    modalContent.style.maxHeight = 'calc(100vh - 240px)';
    modalContent.style.overflowY = 'auto';
    modalContent.style.textAlign = 'center'; // Center the image horizontally

    const img = document.createElement('img');
    img.src = imageUrl;  // Replace with your image URL
    img.style.width = '100%';
    img.style.height = 'auto';
    modalContent.appendChild(img);

// Show the modal
    $('#html_preview_modal').modal('show');
}
function loadTxtInIframe(txtUrl)
{
    const modalContent = document.querySelector('#html_preview_modal_content');
    modalContent.innerHTML = ''; // Clear previous content
// Fetch and display the TXT file
    fetch(txtUrl)
        .then(response => response.text()) // Read file content as text
        .then(text => {
            // Create a preformatted container to preserve text formatting
            const pre = document.createElement('pre');
            pre.textContent = text;

            // Style the container
            pre.style.whiteSpace = 'pre-wrap'; // Wrap long lines
            pre.style.wordWrap = 'break-word'; // Prevent overflow
            pre.style.maxHeight = 'calc(100vh - 240px)'; // Limit height
            pre.style.overflowY = 'auto'; // Add vertical scrolling
            pre.style.padding = '10px';
            pre.style.backgroundColor = '#f8f9fa';
            pre.style.border = '1px solid #ddd';

            // Append the preformatted text to the modal
            modalContent.appendChild(pre);

            // Show the modal
            $('#html_preview_modal').modal('show');
        })
        .catch(error => console.error('Error loading TXT file:', error));
}
function loadTiffInIframe(imageUrl) {
    const modalContent = document.querySelector('#html_preview_modal_content');
    modalContent.innerHTML = ''; // Clear previous content


    fetch(imageUrl)
        .then(response => response.arrayBuffer())
        .then(buffer => {
            const tiff = new Tiff({ buffer });
            const canvas = tiff.toCanvas();

            // Style the canvas for scrollable display
            canvas.style.maxWidth = '100%';
            canvas.style.height = 'auto';

            // Wrap in a scrollable container
            modalContent.style.maxHeight = 'calc(100vh - 240px)';
            modalContent.style.overflowY = 'auto';
            modalContent.style.textAlign = 'center';

            modalContent.appendChild(canvas);
            $('#html_preview_modal').modal('show');
        })
        .catch(error => console.error('Error loading TIFF file:', error));
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
    if (queryString.length > 0 && query) {
        $("#search-query").val(query);
        $("#case_select").val(caseId);
        search();
    }


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

        $('.modal-title').html(uId);
        const parts = docId.split('.');
        var extension= "";
        if (parts.length > 1) {
            extension = parts.pop();
        }
        if(extension == "pdf") {
            $('#html_preview_modal_content').html('');
            var url= "filedownload.html?action=exportNative&ispreviewpdf=1&docPath=" + docId + "&uniqueId=" + uId;
            loadPdfInIframe(url)
        }
        else if(extension == 'jpg' || extension == 'jpeg' || extension == 'png' || extension == 'tiff' || extension == 'tif') {
            $('#html_preview_modal_content').html('');
            var url = "filedownload.html?action=exportNative&ispreviewimage=1&docPath=" + docId + "&uniqueId=" + uId;
            if (extension == 'tiff' || extension == 'tif') {
                loadTiffInIframe(url)
            } else {
                loadImageInIframe(url)
            }
        }
        else if(extension == 'txt')
        {
            $('#html_preview_modal_content').html('');
            var url= "filedownload.html?action=exportNative&ispreviewpdf=1&docPath=" + docId + "&uniqueId=" + uId;
            loadTxtInIframe(url);
        }
        else {
            $.ajax({
                type: 'GET',
                url: 'filedownload.html',
                data: {action: 'exportHtml', docPath: docId, uniqueId: uId, docName: docName},
                success: function (data) {
                    $('#html_preview_modal_content').html('');
                    loadIframeContent(data);
                },
                error: function () {
                    alert("Technical error, try that again in a few moments!");
                }
            });
        }
    });


});