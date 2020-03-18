<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title><tiles:getAsString name="title"/></title>
    <link rel="stylesheet" type="text/css" href="//stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="styles.css"/>
    <link rel="stylesheet" href="//code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css"/>
    <script src="//code.jquery.com/jquery-3.4.1.min.js"></script>
    <script src="js/jquery-ui-1.9.2.custom.js"></script>
</head>
<body class="body">
<tiles:insertAttribute name="header"/>
<div class="container-fluid">
    <div class="row">
        <div class="col-md-12">
            <tiles:insertAttribute name="body"/>
        </div>

    </div>
    <div class="row">
        <div class="footer">
            <tiles:insertAttribute name="footer"/>
        </div>
    </div>
</div>
<div class="modal-loading"></div>
<script src="//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.4.1/js/bootstrap.bundle.js"></script>
</body>
</html>