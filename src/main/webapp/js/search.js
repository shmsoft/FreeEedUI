var lastDocId = null;
var documentsMap = new Object();
var allTags = new Object();

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
    var obj = documentsMap[docId];
    var allTags = tag;
    for (var key in obj) {
        allTags += "," + key;
    }
    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'newtag', docid: docId, tag: allTags},
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
    $("#tags-table-" + docId).append("<tr id='document-tags-row-" + tag + "-" + docId + "' class='document-tags-row'>" +
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
            if (data == 'false') {
                $("#" + tag + "").parent().remove();
            }
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function deleteTagFromAllDocs(tag) {
    $.ajax({
        type: 'POST',
        url: 'tag.html',
        data: {action: 'deletetagfromall', tag: tag},
        success: function (data) {
            $("#" + tag + "").parent().remove();
            var partialId = "document-tags-row-" + tag;
            var tagElems = $('[id*=' + partialId + ']');
            for (var i = 0; i < tagElems.length; i++) {
                var e = tagElems[i];
                var split = e.id.split("-");
                var docId = split[split.length - 1];
                if (docId) {
                    var total = parseInt($("#tags-total-" + docId).html()) - 1;
                    $("#tags-total-" + docId).html(total);
                    var hiddenElemes = $(".doc-tag-" + docId);
                    if (hiddenElemes && hiddenElemes.length > 0) {
                        hiddenElemes[0].remove();
                    }
                }
                e.remove();

            }
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
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

            var esId = $("#esid").val();
            if (esId != null) {
                initPage(esId);
            }

            $("#search-query").val('');
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

            var esId = $("#esid").val();
            if (esId != null) {
                initPage(esId);
            }
        },
        error: function () {
            alert("Technical error, try that again in a few moments!");
        }
    });
}

function changePage(page) {
    $.ajax({
        type: 'POST',
        url: 'dosearch.html',
        data: {action: 'changepage', page: page},
        success: function (data) {
            lastDocId = null;

            $("#result-ajax").html(data);

            var esId = $("#esid").val();
            if (esId != null) {
                initPage(esId);
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

            var esId = $("#esid").val();
            if (esId != null) {
                initPage(esId);
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

            var esId = $("#esid").val();
            if (esId != null) {
                initPage(esId);
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
        $(this).next(".document-tags-table").slideToggle(500);
    });

    $(".esid").each(function (index) {
        var docId = $(this).val();
        documentsMap[docId] = new Object();
        $(".doc-tag-" + docId).each(function (index) {
            var tag = $(this).val();
            documentsMap[docId][tag] = 1;
        });
    });

    //$(".tag-doc-field-cl").autocomplete({source : "tagauto.html"});
    $("#tag-all-text").autocomplete({source: "tagauto.html", delay: 500});
    $("#tag-page-text").autocomplete({source: "tagauto.html", delay: 500});
}

function tagAllBox() {
    $("#tag-all").slideToggle(500);
    $("#tag-page").hide();
}

function tagPageBox() {
    $("#tag-page").slideToggle(500);
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
    $(".case-tags-box-body").append("<div class='tag-table'><div id='" + tag + "' class='case-tags-box-row' onclick='addTagToSearch(\"" + tag + "\")'>" + tag + "</div><div><a href='#' onclick='deleteTagFromAllDocs(\"" + tag + "\")'><img src='images/delete.gif'/></a></div></div>");
}

$(document).ready(function () {
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

        $.ajax({
            type: 'GET',
            url: 'filedownload.html',
            data: {action: 'exportHtml', docPath: docId, uniqueId: uId},
            success: function (data) {
                $("#html_preview_modal_content").html(data);
                $('#html_preview_modal').modal('show');
            },
            error: function () {
                alert("Technical error, try that again in a few moments!");
            }
        });
    });
});