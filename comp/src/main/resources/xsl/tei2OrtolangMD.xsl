<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0" version="2.0">

	<xsl:output method="text"/>

	<xsl:template match="/">
		<xsl:variable name="result">
			<object>
				<title>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:titleStmt/tei:title">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</title>
				<author>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:titleStmt/tei:author">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</author>
				<languages>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:langUsage/tei:language">
						<value>
							<xsl:apply-templates select="."/>
							<xsl:if test="normalize-space(@ident)">
								<xsl:text> (</xsl:text>
								<xsl:apply-templates select="@ident"/>
								<xsl:text>)</xsl:text>
							</xsl:if>
						</value>
					</xsl:for-each>
				</languages>
				<recordingDate>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:sourceDesc/tei:recordingStmt/tei:recording/tei:date">
						<value>
							<xsl:choose>
								<xsl:when test="normalize-space(@when)">
									<xsl:apply-templates select="@when"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:apply-templates select="."/>
								</xsl:otherwise>
							</xsl:choose>
						</value>
					</xsl:for-each>
				</recordingDate>
				<abstract>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:abstract">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</abstract>
				<creation>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:creation">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</creation>
				<creationDate>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:creation/tei:date">
						<value>
							<xsl:choose>
								<xsl:when test="normalize-space(@when)">
									<xsl:apply-templates select="@when"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:apply-templates select="."/>
								</xsl:otherwise>
							</xsl:choose>
						</value>
					</xsl:for-each>
				</creationDate>
				<publicationStmt>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:publicationStmt">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</publicationStmt>
				<notes>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:notesStmt/*">
						<value>
							<xsl:apply-templates select="."/>
							<xsl:if test="normalize-space(@type)">
								<xsl:text> (</xsl:text>
								<xsl:apply-templates select="@type"/>
								<xsl:text>)</xsl:text>
							</xsl:if>
						</value>
					</xsl:for-each>
				</notes>
				<availability>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:publicationStmt/tei:availability">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</availability>
				<availabilityStatus>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:publicationStmt/tei:availability/@status">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</availabilityStatus>
				<availabilityRef>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:publicationStmt/tei:availability//tei:ref/@target">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</availabilityRef>
				<projectDesc>
					<xsl:for-each select="//tei:teiHeader/tei:encodingDesc/tei:projectDesc">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</projectDesc>
				<sourceDesc>
					<xsl:for-each select="//tei:teiHeader/tei:fileDesc/tei:sourceDesc">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</sourceDesc>
				<settingDesc>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:settingDesc">
						<value>
							<xsl:apply-templates select="."/>
						</value>
					</xsl:for-each>
				</settingDesc>
				<particDesc>
					<xsl:for-each select="//tei:teiHeader/tei:profileDesc/tei:particDesc">
						<xsl:choose>
							<xsl:when test="count(//tei:person) &gt; 0">
								<xsl:for-each select="//tei:person">
									<value>
										<xsl:choose>
											<xsl:when test="tei:persName">
												<xsl:choose>
													<xsl:when test="count(tei:persName//tei:name) &gt; 0">
														<xsl:apply-templates select="tei:persName//tei:name"/>
													</xsl:when>
													<xsl:otherwise>
														<xsl:apply-templates select="."/>
													</xsl:otherwise>
												</xsl:choose>
											</xsl:when>
											<xsl:when test="normalize-space(@name)">
												<xsl:apply-templates select="@name"/>
											</xsl:when>
										</xsl:choose>
										<xsl:if test="normalize-space(@role)">
											<xsl:text> (</xsl:text>
											<xsl:apply-templates select="@role"/>
											<xsl:text>)</xsl:text>
										</xsl:if>
									</value>
								</xsl:for-each>
							</xsl:when>
							<xsl:otherwise>
								<value>
									<xsl:apply-templates select="."/>
								</value>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
				</particDesc>
			</object>
		</xsl:variable>
		<xsl:apply-templates select="$result" mode="JSON"/>
	</xsl:template>

	<xsl:template match="/" mode="JSON">
		<xsl:apply-templates select="*" mode="JSON"/>
	</xsl:template>

	<xsl:template match="object" mode="JSON">
		<xsl:text>{</xsl:text>
		<xsl:for-each select="node()">
			<xsl:if test="normalize-space(.)">
				<xsl:apply-templates select="." mode="JSON"/>
				<xsl:if test="not(position() = last())">
					<xsl:text>, </xsl:text>
				</xsl:if>
			</xsl:if>
		</xsl:for-each>
		<xsl:text>}</xsl:text>
	</xsl:template>

	<xsl:template match="*" mode="JSON">
		<xsl:if test="*">
			<xsl:text>"tei:</xsl:text><xsl:value-of select="local-name()"/><xsl:text>"</xsl:text>
			<xsl:text> : </xsl:text>
			<xsl:choose>
				<xsl:when test="count(value) &gt; 1">
					<xsl:text>[</xsl:text>
					<xsl:for-each select="value/node()">
						<xsl:if test="normalize-space(.)">
							<xsl:apply-templates select="."  mode="JSON"/>
							<xsl:if test="not(position() = last())">
								<xsl:text>, </xsl:text>
							</xsl:if>
						</xsl:if>
					</xsl:for-each>
					<xsl:text>]</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates select="value/node()" mode="JSON"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<xsl:param name="dblQuote">"</xsl:param>
	<xsl:param name="dblQuoteRep">\\"</xsl:param>

	<xsl:template match="text()" mode="JSON">
		<xsl:text>"</xsl:text><xsl:copy-of select="normalize-space(replace(., $dblQuote, $dblQuoteRep))"/><xsl:text>"</xsl:text>
	</xsl:template>

	<xsl:template match="*">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="text()">
		<xsl:copy-of select="."/>
	</xsl:template>

</xsl:stylesheet>
