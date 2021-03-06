<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex">
    <title>Index of ${path}</title>
    <link rel="stylesheet" type="text/css" href="${context}/css/style.css">
</head>
<body>
<h2>Index of ${path}</h2>
<div class="wrapper">
    <table>
        <tr>
            <th valign="top"><img src="${context}/icons/blank.png" alt="[ICO]"></th>
        <#if asc == true>
            <th><a href="?C=N&O=D">Name</a></th>
            <th><a href="?C=T&O=D">Type</a></th>
            <th><a href="?C=M&O=D">Last modified</a></th>
            <th><a href="?C=S&O=D">Size</a></th>
        <#else>
            <th><a href="?C=N&O=A">Name</a></th>
            <th><a href="?C=T&O=A">Type</a></th>
            <th><a href="?C=M&O=A">Last modified</a></th>
            <th><a href="?C=S&O=A">Size</a></th>
        </#if>
        </tr>
    <#if parentPath?length gt 0>
        <tr>
            <td><img src="${context}/icons/folder-parent-old.png"/></td>
            <td><a href="${context}${base}${parentPath}">..</a></td>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </#if>
    <#if path?length == 1>
        <#assign path=''>
    </#if>
    <#list elements as element>
        <tr>
            <td><img src="${context}/icons/${fileicon(element.name, element.mimeType)}" alt="[${element.type}]"/></td>
            <#if linkbykey == true>
                <td><a href="${context}${base}/${element.key}">${element.name}</a></td>
            <#else>
                <td><a href="${context}${base}${path}/${element.name}">${element.name}</a></td>
            </#if>
            <td>${element.mimeType}</td>
            <#if element.modification gt 0>
                <td align="right">${element.modification?number_to_datetime}</td>
            <#else>
                <td align="right">-</td>
            </#if>
            <#if element.type == 'object'>
                <td align="right">${formatsize(element.size)}</td>
            <#else>
                <td align="right">-</td>
            </#if>
        </tr>
    </#list>
    </table>
</div>
<address>Ortolang/@project.version@ Diffusion Server</address>
</body>
</html>

 