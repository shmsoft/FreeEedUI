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
    if(allCases)
    {
        url = $("#aiApiUrl").val() + '/question_cases/';
        data = {
            question: $(".question_input").val()
        };
        var cases = $(".your-case-select").find('option');
        var casesName = [];

        cases.each(function(item) {
            casesName.push('freeeed_' + $("#aiApiKey").val() + '_' + $(this).val());
        });
        data["case_ids"] = casesName.join(',');
    }
    else {
        url = $("#aiApiUrl").val() + '/question_case/';
        data = {
            case_id: 'freeeed_' + $("#aiApiKey").val() + '_' + $('.your-case-select').val(),
            question: $(".question_input").val()
        };
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
});