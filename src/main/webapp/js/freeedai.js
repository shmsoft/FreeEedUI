var lastDocId = null;
var documentsMap = new Object();
var allTags = new Object();


function initPage(docId) {
    selectDocument(docId);
    initTags();
}

function newTagEnter(docId, e) {
    var charCode;
    
    if (e && e.which) {
        charCode = e.which;
    } else if(window.event) {
        e = window.event;
        charCode = e.keyCode;
    }

    if(charCode != 13) {
        return;
    }
    
    newTag(docId);
}

$(document).ready(function() {
    $("body").bind({
        ajaxStart: function() { 
            $(this).addClass("loading"); 
        },
        ajaxStop: function() { 
            $(this).removeClass("loading"); 
        }    
    });
    
    $('#search-query').keypress(function(e) {
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
	      data: { action: 'exportHtml', docPath : docId, uniqueId : uId},
	      success:function(data) {
	        $("#html_preview_modal_content").html(data);
	        $('#html_preview_modal').modal('show');
	      },
	      error:function(){
	        alert("Technical error, try that again in a few moments!");
	      }
    	});
    });
});