function onSubmit(event)
{
    event.preventDefault();
    let container = $(".chat-wrapper")[0];
    var question = $(".question_input").val();
    let newHTMLContent = '<div class="question">' + question + '</p>';
    container.innerHTML += newHTMLContent;
    $('#send_question').prop('disabled', true);
    $('.question_input').prop('disabled', true);

    var allCases = $("#allCasesCheckbox").prop("checked");
    var url;
    var data = {};

    var aiApiKey = $("#aiApiKey").val();
    var aiApiUrl = $("#aiApiUrl").val();
    var selectedCaseDbId = $('.your-case-select').val();

    // Prefer projectId for FastAPI collection naming
    var selectedOpt = $('#case_Select option:selected');
    var selectedCaseProjectId = selectedOpt.data('project-id') || $("#aiProjectId").val();

    if(allCases)
    {
        url = aiApiUrl + '/question_cases/';
        data = {
            question: $(".question_input").val()
        };
        var cases = $(".your-case-select").find('option');
        var casesName = [];

        cases.each(function() {
            var projectId = $(this).data('project-id');
            var suffix = (projectId && projectId.length > 0) ? projectId : $(this).val();
            casesName.push('freeeed_' + aiApiKey + '_' + suffix);
        });
        data["case_ids"] = casesName.join(',');
        console.log("AI Advisor all-cases query", {url: url, case_ids: data["case_ids"]});
        $("#aiDebug").remove();
        $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI collections: ' + data["case_ids"] + '</small></div>');
    }
    else {
        url = aiApiUrl + '/question_case/';
        var aiCollectionSuffix = (selectedCaseProjectId && selectedCaseProjectId.length > 0) ? selectedCaseProjectId : selectedCaseDbId;
        data = {
            case_id: 'freeeed_' + aiApiKey + '_' + aiCollectionSuffix,
            question: $(".question_input").val()
        };
        console.log("AI Advisor single-case query", {url: url, selectedCaseDbId: selectedCaseDbId, selectedCaseProjectId: selectedCaseProjectId, case_id: data.case_id});
        $("#aiDebug").remove();
        $('.chat-wrapper').prepend('<div id="aiDebug" class="answer"><small>AI collection: ' + data.case_id + '</small></div>');
    }

    $.ajax({
            type: 'GET',
            headers: {
                'Access-Control-Allow-Origin': '*'
            },
            url: url,
            data: data,
            success:function(data) {
                var sourcesHtml = '';

                if(allCases)
                {
                    data.forEach(function(answer) {
                        var caseId = answer.case_id;
                        if(answer.sources && answer.sources.length > 0)
                        {
                            sourcesHtml = '<small class="source">';
                            for (let index = 0; index < answer.sources.length; index++) {
                                var source = answer.sources[index];
                                source = '<a target="_blank" href="' +  getCurrentUrl() + '/search.html?caseid=' + caseId + '&query=UPI:' + source + '">' + source + '</a>'
                                sourcesHtml = sourcesHtml +  source + ' | ';
                            }
                            sourcesHtml = sourcesHtml + '</small>';
                        }
                        newHTMLContent = '<div class="answer">' + answer.answer + sourcesHtml + '</div>';
                        container.innerHTML += newHTMLContent;
                     });
                }
                else
                {
                    var caseId = $("#case_Select").val();
                    if(data.sources && data.sources.length > 0)
                    {
                        sourcesHtml = '<small class="source">';
                        for (let index = 0; index < data.sources.length; index++) {
                            var source = data.sources[index];
                            source = '<a target="_blank" href="' +  getCurrentUrl() + '/search.html?caseid=' + caseId + '&query=UPI:' + source + '">' + source + '</a>'
                            sourcesHtml = sourcesHtml +  source + ' | ';
                        }
                        sourcesHtml = sourcesHtml + '</small>';
                    }

                    newHTMLContent = '<div class="answer">' + data.answer + sourcesHtml + '</div>';
                    container.innerHTML += newHTMLContent;
                }



                $(".question_input").val('');
                $('#send_question').prop('disabled', false);
                $('.question_input').prop('disabled', false);
            },
            error: function(xhr, status, error) {
                console.error(xhr.responseText);
                alert("Technical error, try that again in a few moments!");
                $('#send_question').prop('disabled', false);
                $('.question_input').prop('disabled', false);
                $(".question_input").val('');
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