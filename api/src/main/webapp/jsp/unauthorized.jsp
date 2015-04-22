<%@ page language="java" pageEncoding="UTF-8" session="false"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>Access Denied : insufficient permissions</title>
</head>
<body>
	<h1>Access Denied</h1>
	Access has been denied to this resource due to insufficient permissions: ${message}<br/>
	<br/>
	Try to <a href="https://localhost:8443/auth/realms/ortolang/tokens/login">authenticate</a> using another account in order to access this resource.
</body>
</html> 