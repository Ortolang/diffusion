<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

    <xsl:output method="text"/>
    
    <xsl:template match="/">
        <xsl:variable name="result">
            <object>
                <audio-filename>
                    <xsl:for-each select="/Trans/@audio_filename">
                        <value>
                            <xsl:apply-templates select="."/>
                        </value>
                    </xsl:for-each>
                </audio-filename>
                <speakers>
                    <xsl:for-each select="/Trans/Speakers/Speaker">
                        <value>
                            <xsl:apply-templates select="@name"/>
                            <xsl:if test="@dialect">
                                <xsl:text> (</xsl:text>
                                <xsl:apply-templates select="@dialect"/>
                                <xsl:text>)</xsl:text>
                            </xsl:if>
                        </value>
                    </xsl:for-each>
                </speakers>
                <speakerNumber>
                    <xsl:for-each select="/Trans/Speakers">
                        <value>
                            <xsl:value-of select="count(Speaker)"/>
                        </value>
                    </xsl:for-each>
                </speakerNumber>
                <episodeNumber>
                    <value>
                        <xsl:value-of select="count(/Trans/Episode)"/>
                    </value>
                </episodeNumber>
                <turnNumber>
                    <value>
                        <xsl:value-of select="count(/Trans//Turn)"/>
                    </value>
                </turnNumber>
                <speechTime>
                    <value>
                        <xsl:value-of select="floor(sum(/Trans/Episode/Section/Turn/@endTime) - sum(/Trans/Episode/Section/Turn/@startTime))"/>
                    </value>
                </speechTime>
                <speakersSpeechTime>
                    <xsl:for-each select="/Trans/Speakers/Speaker">
                        <value>
                            <xsl:apply-templates select="@name"/>
                            <xsl:text>: </xsl:text>
                            <xsl:value-of select="floor(sum(/Trans/Episode/Section/Turn[@speaker=current()/@id]/@endTime) - sum(/Trans/Episode/Section/Turn[@speaker=current()/@id]/@startTime))"/>
                        </value>
                    </xsl:for-each>
                </speakersSpeechTime>
                <words>
                    <value>
                        <xsl:variable name="join" select="string-join(/Trans/Episode/Section/Turn, ' ')"/>
                        <xsl:value-of select="string-length(normalize-space($join)) - string-length(translate(normalize-space($join),' ','')) + 1"/>
                    </value>
                </words>
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
            <xsl:text>"trans:</xsl:text><xsl:value-of select="local-name()"/><xsl:text>"</xsl:text>
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
