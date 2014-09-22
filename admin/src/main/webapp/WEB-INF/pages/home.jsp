<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js sidebar-large lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js sidebar-large lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js sidebar-large lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js sidebar-large"> <!--<![endif]-->

<head>
    <meta charset="utf-8">
    <title>Ortolang Diffusion Admin - Tableau de bord</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta content="" name="description" />
    <meta content="ortolang" name="author" />
    <jsp:include page="styles.jsp"/>
    <script src="plugins/modernizr/modernizr-2.6.2-respond-1.1.0.min.js"></script>
</head>

<body data-page="dashboard">
    <jsp:include page="topmenu.jsp"/>
    <div id="wrapper">
        <jsp:include page="sidebar.jsp"/>
		<div id="main-content">
            <div class="row">
                <div class="col-lg-12">
                    <h1>Ortolang Diffusion Admin - <small>Tableau de bord</small></h1>
                    <br><br><br>
                </div>
            </div>
        </div>
    </div>
	<jsp:include page="scripts.jsp"/>
    <script src="js/application.js"></script>
</body>

</html>