<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex">
    <title>ORTOLANG : ${path}</title>
    <link rel="stylesheet" type="text/css" href="${context}/css/style.css">
</head>
<body>
<h2>Fiche de métadonnées</h2>
<div class="wrapper">
	<section>
		<article>
			<p>Vous pouvez retrouver toutes les informations du projet à l'adresse : <a href="//www.ortolang.fr/market/item/${alias}" target="_BLANK">www.ortolang.fr/market/item/${alias}</a>.</p>
		</article>
	</section>
    <section>
    	<h4>Informations générales</h4>
    	<article>
			<#list dcDocument.title as titleValue>
				<dl>
					<dt>Titre</dt>
					<dd>${titleValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.description as dcValue>
				<dl>
					<dt>Description</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.creator as dcValue>
				<dl>
					<dt>Créateur</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.publisher as dcValue>
				<dl>
					<dt>Editeur</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.contributor as dcValue>
				<dl>
					<dt>Contributeur</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.rights as dcValue>
				<dl>
					<dt>Droits</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.subject as dcValue>
				<dl>
					<dt>Mot-clés</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.date as dcValue>
				<dl>
					<dt>Date</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.type as dcValue>
				<dl>
					<dt>Type</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.format as dcValue>
				<dl>
					<dt>Format</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.identifier as dcValue>
				<dl>
					<dt>Identifiant</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.source as dcValue>
				<dl>
					<dt>Source</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.language as dcValue>
				<dl>
					<dt>Langue</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.relation as dcValue>
				<dl>
					<dt>Relation</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
			<#list dcDocument.coverage as dcValue>
				<dl>
					<dt>Couverture</dt>
					<dd>${dcValue.value}</dd>
				</dl>
			</#list>
    	</article>
    </section>
    <section>
    	<h4>Contenu</h4>
    	<article>
    		Vous pouvez télécharger l'ensemble des fichiers contenus dans ce répertoire 
    		<a class="btn btn-primary" href="${context}${base}/export?path=${path}">ici</a>.
    	</article>
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
    </section>
</div>
<address>Ortolang/@project.version@ Diffusion Server</address>
</body>
</html>

 