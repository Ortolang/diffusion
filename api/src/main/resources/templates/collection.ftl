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
			<th valign="top"><img src="${url.ctx}/icons/blank.png" alt="[ICO]"></th>
			<th><a href="?C=N${(sort eq 'N' && asc)?'&O=D':'&O=A'}">Name</a></th>
			<th><a href="?C=M${(sort eq 'M' && asc)?'&O=D':'&O=A'}">Last modified</a></th>
			<th><a href="?C=S${(sort eq 'S' && asc)?'&O=D':'&O=A'}">Size</a></th>
			<th><a href="?C=T${(sort eq 'T' && asc)?'&O=D':'&O=A'}">Type</a></th>
		</tr>
		<tr><th colspan="5"><hr></th></tr>
		<#if parent != null>
			<tr>
				<td><img src="${url.ctx}/icons/back.png"/></td>
				<td><a href="${url.base}${parent}">..</a></td>
				<td></td>
				<td></td>
			</tr>
		</#if>
		<#list elements as element>
			<tr>
				<td><img src="${ctx}/icons/${element.type}.png" alt="[${element.type}]"/></td>
				<td></td>
				<td></td>
				<td></td>
			</tr>
		<#list>
		<tr><th colspan="5"><hr></th></tr>
	</table>
	<address>Ortolang/@project.version@ Diffusion Server</address>
</body>
</html>

 