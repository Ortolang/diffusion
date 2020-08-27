<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:x="http://www.tei-c.org/ns/1.0">
	<xsl:output method="text" />
	<xsl:strip-space elements="*" />

	<xsl:template match="x:text/x:body//text()">
		<xsl:value-of select="concat(.,'&#xA;')" />
	</xsl:template>
	<xsl:template match="text()" />
</xsl:stylesheet>