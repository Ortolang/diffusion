<%@ page language="java" pageEncoding="UTF-8" session="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/ortolang.tld" prefix="ortolang"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>Index of ${path}</title>
</head>
<body>
	<h1>Index of ${path}</h1>
	<table>
		<tr>
			<th valign="top"><img src="${ctx}/icons/blank.png" alt="[ICO]"></th>
			<th><a href="?C=N${(sort eq 'N' && asc)?'&O=D':'&O=A'}">Name</a></th>
			<th><a href="?C=M${(sort eq 'M' && asc)?'&O=D':'&O=A'}">Last modified</a></th>
			<th><a href="?C=S${(sort eq 'S' && asc)?'&O=D':'&O=A'}">Size</a></th>
			<th><a href="?C=T${(sort eq 'T' && asc)?'&O=D':'&O=A'}">Type</a></th>
		</tr>
		<tr><th colspan="5"><hr></th></tr>
		<c:if test="${parent != null}">
			<tr>
				<td><img src="${ctx}/icons/back.png"/></td>
				<td><a href="${ctx}/content${parent}">..</a></td>
				<td></td>
				<td></td>
			</tr>
		</c:if>
		<jsp:useBean id="date" class="java.util.Date"/>  
    	<c:forEach var="element" items="${elements}">
			<jsp:setProperty name="date" property="time" value="${element.modification}"/>    
    		<tr>
				<td><img src="${ctx}/icons/${element.type}.png" alt="[${element.type}]"/></td>
				<td><a href="${ctx}/content${path}/${element.name}">${element.name}</a></td>
				<td align="right"><fmt:formatDate type="both" dateStyle="short" timeStyle="short" value="${date}"/></td>
				<c:choose>
					<c:when test="${element.type eq 'object'}">
						<td align="right"><ortolang:fbytes value="${element.size}"/></td>
					</c:when>
					<c:otherwise>
						<td align="right">-</td>
					</c:otherwise>
				</c:choose>
				<td>${element.mimeType}</td>
			</tr>
		</c:forEach>
		<tr><th colspan="5"><hr></th></tr>
	</table>
	<address>Ortolang/1.6.0 (Ubuntu) Server at locahost Port 80</address>
</body>
</html> 