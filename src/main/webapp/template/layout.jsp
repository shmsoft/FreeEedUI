<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%-- Cache-bust the app's own CSS/JS with the build SHA so an upgrade never serves a
     stale copy (e.g. the redaction styles missing after a 10.8.7 -> 10.8.8 upgrade). --%>
<% String assetV = org.freeeed.search.web.Version.getAssetVersion(); %>
<html>
    <head>
        <meta charset="UTF-8">
        <link rel="icon" type="image/x-icon" href="images/freeeedfavicon.ico" />
        <title><tiles:getAsString name="title" /></title>
        <%-- Inter is vendored locally (css/fonts.css -> fonts/inter-variable.woff2); no
             external font load. "Nothing leaves the machine" -- works offline / air-gapped. --%>
        <link rel="stylesheet" type="text/css" href="css/fonts.css" />
        <link rel="stylesheet" type="text/css" href="css/bootstrap.css" />
        <link rel="stylesheet" type="text/css" href="css/bootstrap-responsive.css" />
        <link rel="stylesheet" type="text/css" href="css/bootstrap-icons.css" />
        <link rel="stylesheet" type="text/css" href="css/styles.css?v=<%=assetV%>" />
        <link rel="stylesheet" type="text/css" href="css/search-modern.css?v=<%=assetV%>" />
        <link rel="stylesheet" href="css/vendor/jquery-ui-1.9.2.css" />
        <script src="js/jquery-1.8.3.js"></script>
        <script src="js/jquery-ui-1.9.2.custom.js"></script>
        <script>
            $(document).ready(function(){
                $(".menu_button").click(function(){
                    if($(".left").css('left') == "0px")
                    {
                         $( ".left" ).animate({
                            left: "-260px"
                          
                        }, {
                            duration: 200
                        });
                        $( ".content" ).animate({
                            marginLeft: 0,
                            width:  $(document).width() - 12
                           
                        }, {
                            duration: 200
                        });
                    }
                    else
                    {
                         $( ".left" ).animate({
                            left: "0px",
                           
                        }, {
                            duration: 200
                        });
                        $( ".content" ).animate({
                            marginLeft: 260,
                            width: $(document).width() - 276,
                         
                        }, {
                            duration: 200
                        });
                    }
                

                })

            });
        </script>
    </head>
    <body class="body" style="overflow: <c:if test="${loggedVisitor == null}">hidden</c:if>">
    
    <div class="wrapper">
        <div class="header">
            <tiles:insertAttribute name="header" />
        </div>
        <div class="left" style="position: <c:if test="${loggedVisitor == null}">inherit</c:if>">
            <tiles:insertAttribute name="menu" />
        </div>
        <div class="content">
            <tiles:insertAttribute name="body" />
        </div>
    </div>
    <div class="modal-loading"></div>
    
    <script src="js/bootstrap.js"></script>
    </body>
</html>