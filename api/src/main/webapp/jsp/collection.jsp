<%@ page language="java" pageEncoding="UTF-8" session="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>Collection content</title>
</head>
<body>
	<h1>Content of directory ${path}</h1>
	<hr/>
	<table width="640px">
		<thead>
			<tr>
				<td>type</td>
				<td>name</td>
				<td>size</td>
				<td>last modified</td>
			</tr>
		</thead>
		<c:forEach var="element" items="${collection.elements}">
			<tr>
				<td><img src="../img/icon/${element.mimeType}"/></td>
				<td><a href="${path}/${element.name}">${element.name}</a></td>
				<td>${element.size}</td>
				<td><fmt:formatDate type="both" dateStyle="long" timeStyle="long" value="${element.modification}" /></td>
			</tr>
		</c:forEach>
	</table>
	<hr/>
	<i>Ortolang Server</i>
</body>
</html> 