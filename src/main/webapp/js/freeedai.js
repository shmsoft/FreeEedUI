function onSubmit(event)
{
    event.preventDefault();
    let container = $(".chat-wrapper")[0];
    let newHTMLContent = '<div class="question">' + $(".question_input").val() + '</p>';
    container.innerHTML += newHTMLContent;
    $('#send_question').prop('disabled', true);
    $('.question_input').prop('disabled', true);
    $.ajax({
        type: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*'
        },
        url: $("#aiApiUrl").val() + '/question_case/',
        data: { case_id: 'freeeed_' + $("#aiApiKey").val() + '_' + $('.your-case-select').val(), question: $(".question_input").val()},
        success:function(data) {
            var sourcesHtml = '';
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


});