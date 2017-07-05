$(document).ready(function () {
    $("body").bind({
        ajaxStart: function () {
            $(this).addClass("loading");
        },
        ajaxStop: function () {
            $(this).removeClass("loading");
        }
    });

    $('#uploadfile').ajaxfileupload({
        'action': 'fileupload.html',
        'onComplete': function (json) {
            var res = JSON.parse(json);
            if (res.status == 'error') {
                alert("Error uploading file!");
            } else {
                $("#uploadedFileId").val(res.file);
                $("#uploadedFileNameId").text(res.fileShort);
                $("#uploadedFileBoxId").show();
            }

            $(".body").removeClass("loading");
        },
        'onStart': function () {
            $(".body").addClass("loading");
        },
        'onCancel': function () {
            $(".body").removeClass("loading");
        }
    });

    $('#uploadLoadFilebutton').on('click', function () {
        if ($('input[type=file]')[1].files[0] == undefined) {
            $('#choosefile').show();
            return false;
        }
        $('#choosefile').hide();
        var formData = new FormData();
        formData.append('filetype', 'loadfile');
        formData.append('case', document.getElementsByName('name')[0].value);
        formData.append('file', $('input[type=file]')[1].files[0]);
        $.ajax({
            url: 'fileupload.html',
            type: 'POST',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,

            success: function (data, status, xhttp) {
                if (status == "success") {
                    $('#success').show();
                    $("#fail").hide();
                }
                else {
                    $("#success").hide();
                    $("#fail").show();
                }
            }
        });
    });
});