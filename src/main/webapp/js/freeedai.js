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
            newHTMLContent = '<div class="answer">' + data.answer + '</p>';
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

function sendQuestion(event)
{
    $(".question_input").val(event.srcElement.innerText);
    onSubmit(event);
}

$(document).ready(function() {


});