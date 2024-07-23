$(document).ready(function() {

    let input = document.getElementById("uploadfile");
    let fileName = document.getElementById("fileName")

    input.addEventListener("change", ()=>{
        let inputFile = document.querySelector("input[type=file]").files[0];
        fileName.innerText = inputFile.name;
    })

    $("body").bind({
        ajaxStart: function() { 
            $(this).addClass("loading"); 
        },
        ajaxStop: function() { 
            $(this).removeClass("loading"); 
        }
    });
     $('input[name="fileOption"]').change(toggleRows);
     toggleRows();

    
    $('#uploadfile').ajaxfileupload({
          'action': 'fileupload.html',
          'onComplete': function(json) {
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
          'onStart': function() {
            $(".body").addClass("loading"); 
          },
          'onCancel': function() {
            $(".body").removeClass("loading");
          }
    })
});

 function toggleRows() {
     const fileOption = $('input[name="fileOption"]:checked').val();
     if (fileOption === 'uploadFile') {
         $('#uploadFileRow').removeClass('hidden');
         $('#serverFolderRow').addClass('hidden');
     } else if (fileOption === 'serverFolder') {
         $('#uploadFileRow').addClass('hidden');
         $('#serverFolderRow').removeClass('hidden');
     }
 }