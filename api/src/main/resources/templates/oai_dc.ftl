<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex">
    <title>ORTOLANG : DublinCore OAI record with identifier ${recordRepresentation.identifier}</title>
    <link rel="stylesheet" type="text/css" href="${context}/css/style.css">
</head>
<body>
<h2>DublinCore metadata</h2>
<div class="wrapper">
    <section>
      <h4>General informations</h4>
    	<article>
			<#list xmlDocument.values as xmlValue>
          <dl>
            <dt>${xmlValue.name}</dt>
            <dd>${xmlValue.value}</dd>
          </dl>
        </#list>
    </section>
</div>
</body>
</html>