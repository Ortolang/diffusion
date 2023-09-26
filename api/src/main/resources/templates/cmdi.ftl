<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="robots" content="noindex">
    <title>ORTOLANG : CMDI OAI record with identifier ${recordRepresentation.identifier}</title>
    <link rel="stylesheet" type="text/css" href="${context}/css/style.css">
</head>
<body>
<h2>CMDI metadata</h2>
<div class="wrapper">
    <section>
      <article>
        <p>Get this metadata file from the PID : <a href="${xmlDocument.mdSelfLink}" target="_BLANK">${xmlDocument.mdSelfLink}</a>.</p>
      </article>
    </section>
    <section>
      <h4>CMDI Components</h4>
      <article>
        <#list xmlDocument.values as cmdiValue>
          <dl>
            <dt>${cmdiValue.name}</dt>
            <dd>${cmdiValue.value}</dd>
          </dl>
        </#list>
      </article>
    </section>
</div>
</body>
</html>