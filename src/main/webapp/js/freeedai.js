function onSubmit(event)
{
    event.preventDefault();
    let container = $(".chat-wrapper")[0];
    var $questionInput = $(".question_input");
    var question = $questionInput.val();
    let newHTMLContent = '<div class="question">' + question + '</p>';
    container.innerHTML += newHTMLContent;
    $('#send_question').prop('disabled', true);
    $questionInput.prop('disabled', true);

    var allCases = $("#allCasesCheckbox").prop("checked");

    var aiApiUrl = $("#aiApiUrl").val();

    // dropdown value is DB id; option has data-project-id="case_3" etc.
    var selectedCaseDbId = $('.your-case-select').val();
    var selectedOpt = $('#case_Select option:selected');
    var selectedCaseProjectId = selectedOpt.data('project-id') || $("#aiProjectId").val();

    function resetInputs() {
        $questionInput.val('');
        $('#send_question').prop('disabled', false);
        $questionInput.prop('disabled', false);
    }

    function renderError(msg, context) {
        console.error("AI Advisor error", {context: context, message: msg});
        container.innerHTML += '<div class="answer"><b>AI error (' + context + '):</b> <pre style="white-space:pre-wrap">' + msg + '</pre></div>';
        resetInputs();
    }

    function renderAnswer(data, usedCaseId) {
        var sourcesHtml = '';
        try {
            if (data && data.answer && typeof data.answer === 'string' && data.answer.indexOf('No matching documents') >= 0 && usedCaseId) {
                data.answer = data.answer + ' (case_id=' + usedCaseId + ')';
            }
        } catch (e) {}

        if(allCases)
        {
            // /question_cases returns JSON; some implementations return a single object, some return {answers:[...]}
            var answers = Array.isArray(data) ? data : (data && data.answers ? data.answers : null);
            if (answers && Array.isArray(answers)) {
                answers.forEach(function(answer) {
                    var caseId = answer.case_id;
                    if(answer.sources && answer.sources.length > 0)
                    {
                        sourcesHtml = '<small class="source">';
                        for (let index = 0; index < answer.sources.length; index++) {
                            var source = answer.sources[index];
                            source = '<a target="_blank" href="' +  getCurrentUrl() + '/search.html?caseid=' + encodeURIComponent(caseId) + '&query=UPI:' + encodeURIComponent(source) + '">' + source + '</a>'
                            sourcesHtml = sourcesHtml +  source + ' | ';
                        }
                        sourcesHtml = sourcesHtml + '</small>';
                    }
                    newHTMLContent = '<div class="answer">' + (answer.answer || JSON.stringify(answer)) + sourcesHtml + '</div>';
                    container.innerHTML += newHTMLContent;
                });
            } else {
                // fallback: show whatever object came back
                container.innerHTML += '<div class="answer">' + (data && data.answer ? data.answer : JSON.stringify(data)) + '</div>';
            }
        }
        else
        {
            var caseId = $("#case_Select").val();
            if(data && data.sources && data.sources.length > 0)
            {
                sourcesHtml = '<small class="source">';
                for (let index = 0; index < data.sources.length; index++) {
                    var source = data.sources[index];
                    source = '<a target="_blank" href="' +  getCurrentUrl() + '/search.html?caseid=' + encodeURIComponent(caseId) + '&query=UPI:' + encodeURIComponent(source) + '">' + source + '</a>'
                    sourcesHtml = sourcesHtml +  source + ' | ';
                }
                sourcesHtml = sourcesHtml + '</small>';
            }

            newHTMLContent = '<div class="answer">' + ((data && data.answer) ? data.answer : JSON.stringify(data)) + sourcesHtml + '</div>';
            container.innerHTML += newHTMLContent;
        }

        resetInputs();
    }

    function toNumericCaseId(value) {
        if (!value) return null;
        var m = String(value).match(/(\d+)$/);
        return m ? m[1] : null;
    }

    // Show server-side collection list for debugging
    $.ajax({
        type: 'GET',
        url: aiApiUrl + '/describe_index/',
        dataType: 'json',
        success: function(indexInfo) {
            try {
                var names = [];
                if (Array.isArray(indexInfo)) {
                    indexInfo.forEach(function(x) { if (x && x.name) names.push(x.name + '(' + x.count + ')'); });
                }
                if (names.length > 0) {
                    $("#aiIndexDebug").remove();
                    $('.chat-wrapper').prepend('<div id="aiIndexDebug" class="answer"><small>FastAPI collections: ' + names.join(', ') + '</small></div>');
                }
            } catch (e) {}
        }
    });

    if (allCases) {
        var options = $(".your-case-select").find('option');
        var caseIds = [];
        options.each(function() {
            var dbId = $(this).val();
            var projectId = $(this).data('project-id');
            var numeric = toNumericCaseId(projectId) || toNumericCaseId(dbId);
            if (numeric) {
                caseIds.push(String(numeric));
            }
        });

        if (!caseIds.length) {
            renderError('Could not derive numeric case ids from dropdown/projectId values.', 'case_id');
            return;
        }

        $("#aiDebug").remove();
        $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI case_ids (numeric): ' + caseIds.join(', ') + '</small></div>');

        $.ajax({
            type: 'GET',
            url: aiApiUrl + '/question_cases/',
            data: {question: question, case_ids: caseIds},
            traditional: true,
            dataType: 'json',
            success: function(resp) {
                renderAnswer(resp, caseIds.join(','));
            },
            error: function(xhr) {
                var msg = (xhr && xhr.responseText) ? xhr.responseText : ("HTTP " + (xhr ? xhr.status : "") );
                renderError(msg, 'GET');
            }
        });
        return;
    }

    var numericCaseId = toNumericCaseId(selectedCaseProjectId) || toNumericCaseId(selectedCaseDbId);
    if (!numericCaseId) {
        renderError('Could not derive numeric case_id from selected case (dbId=' + selectedCaseDbId + ', projectId=' + selectedCaseProjectId + ')', 'case_id');
        return;
    }

    $("#aiDebug").remove();
    $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI case_id (numeric): ' + numericCaseId + '</small></div>');

    $.ajax({
        type: 'GET',
        url: aiApiUrl + '/question_case/',
        data: {question: question, case_id: String(numericCaseId)},
        traditional: true,
        dataType: 'json',
        success: function(resp) {
            renderAnswer(resp, String(numericCaseId));
        },
        error: function(xhr) {
            var msg = (xhr && xhr.responseText) ? xhr.responseText : ("HTTP " + (xhr ? xhr.status : "") );
            renderError(msg, 'GET');
        }
    });

}

function getCurrentUrl()
{
    const url = new URL(window.location.href); // new URL(document.URL)
    let path = url.pathname.split("/");
    path.pop(); // remove the last
    return path.join("/")
}
function matterTypeSelect(value)
{
    if(value != 'generic')
    {
        var select = document.querySelector('#matterType');
        select.value = 'generic';
        alert("Specialized reports are under development. \nCurrently you can vote for which report you would like to ne developed first. \nCast your ballot by writing to info by writing to info@scaia.ai. \nThank you!");
    }
}

function sendQuestion(event)
{
    alert("Specialized reports are under development. \nCurrently you can vote for which report you would like to ne developed first. \nCast your ballot by writing to info by writing to info@scaia.ai. \nThank you!");
    return;
    $(".question_input").val(event.srcElement.innerText);
    onSubmit(event);
}

$(document).ready(function() {

    $("#allCasesCheckbox").change(function() {
        if(this.checked) {
            $("#case_Select").prop('disabled', true);
        } else {
            $("#case_Select").prop('disabled', false);
        }
    });

    // Auto-submit case change so the backend session updates selectedCase
    $("#case_Select").change(function() {
        if (document.change) {
            document.change.submit();
        }
    });
});