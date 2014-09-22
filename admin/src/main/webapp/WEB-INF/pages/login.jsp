<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<!--[if lt IE 7]>      <html class="no-js sidebar-large lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js sidebar-large lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js sidebar-large lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js sidebar-large"> <!--<![endif]-->

<head>
    <meta charset="utf-8">
    <title>Ortolang Diffusion Admin - Login</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta content="" name="description" />
    <meta content="ortolang" name="author" />
    <jsp:include page="styles.jsp"/>
    <script src="plugins/modernizr/modernizr-2.6.2-respond-1.1.0.min.js"></script>
</head>

<body class="login fade-in" data-page="login">
    <div class="container" id="login-block">
        <div class="row">
            <div class="col-sm-6 col-md-4 col-sm-offset-3 col-md-offset-4">
                <div class="login-box clearfix animated flipInY">
                    <div class="page-icon animated bounceInDown">
                        <img src="img/account/user-icon.png" alt="Key icon">
                    </div>
                    <div class="login-logo">
                        <a href="#?login-theme-3">
                            <img src="img/account/login-logo.png" alt="Ortolang Logo">
                        </a>
                    </div>
                    <hr>
                    <div class="login-form">
                        <div class="alert alert-danger hide">
                            <button type="button" class="close" data-dismiss="alert">×</button>
                            <h4>${errortitle} !</h4>
                            ${errormessage}
                        </div>
                        <form action="login.do" method="post">
                        	<input type="hidden" name="action" value="login" />
                            <input type="text" placeholder="Username" name="username" class="input-field form-control user" />
                            <input type="password" placeholder="Password" name="password" class="input-field form-control password" />
                            <button type="submit" class="btn btn-login">Login</button>
                        </form>
                        <div class="login-links">
                            <a href="#">Forgot password?</a>
                            <br>
                            <a href="#">Don't have an account? <strong>Sign Up</strong></a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <jsp:include page="scripts.jsp"/>
    <script src="plugins/backstretch/backstretch.min.js"></script>
    <script src="js/account.js"></script>
</body>

</html>