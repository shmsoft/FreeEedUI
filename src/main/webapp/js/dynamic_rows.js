var counter = 1;
$(document).ready(function () {

    $("#addnewtag").on("click", function () {
        var newRow = $("<tr>");
        var cols = "";
        cols += '<td></td>><td><input type="text" placeholder="Enter New Tag" id="tag' + counter + '"  name="tag' + counter + '"/></td>';
        cols += '<td><input type="button" class="ibtnDel btn btn-danger btn-link" style="color: firebrick" value="Delete"></td>';
        newRow.append(cols);
        $("#settings").append(newRow);
        counter++;
    });

    $("#settings").on("click", ".ibtnDel", function (event) {
        $(this).closest("tr").remove();
        counter -= 1
    });
});

function renderTag(tag) {
    var newRow = $("<tr>");
    var cols = "";
    cols += '<td></td>><td><input type="text" value=' + tag + ' id="tag' + counter + '"  name="tag' + counter + '"/></td>';
    cols += '<td><input type="button" class="ibtnDel btn btn-danger btn-link" style="color: firebrick" value="Delete"></td>';
    newRow.append(cols);
    $("#settings").append(newRow);
    counter++;
}