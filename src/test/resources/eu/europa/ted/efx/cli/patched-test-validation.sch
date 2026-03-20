<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:efx="http://eforms.ted.europa.eu/efx" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">

    <title>Test dynamic validation</title>

    <ns prefix="efx" uri="http://eforms.ted.europa.eu/efx"/>

    <let name="apiUrl-default" value="'http://localhost:39155/v1'"/>

    <xsl:function as="xs:integer" name="efx:call-api">
        <xsl:param as="xs:string" name="endpoint-url"/>
        <xsl:param as="xs:string" name="function"/>
        <xsl:param as="xs:string*" name="args"/>
        <xsl:variable name="base-url" select="concat(             if (ends-with($endpoint-url, '/')) then $endpoint-url else concat($endpoint-url, '/'),             $function)"/>
        <xsl:variable name="query-params" select="string-join(             for $i in 1 to count($args)             return concat('arg', $i, '=', encode-for-uri(string($args[$i]))),             '&amp;')"/>
        <xsl:variable name="url" select="if ($query-params != '') then concat($base-url, '?', $query-params) else $base-url"/>
        <xsl:variable name="response" select="             if ($endpoint-url = '' or not(unparsed-text-available($url)))             then '-1'             else unparsed-text($url)"/>
        <xsl:value-of select="if ($response castable as xs:integer) then xs:integer($response) else -1"/>
    </xsl:function>

    <pattern id="TEST-PATTERN">
        <rule context="/notice">
            <let name="__apiResult" value="efx:call-api($apiUrl-default, 'check', (id/normalize-space(text())))"/>
            <assert id="R-TEST-ERR" role="ERROR" test="$__apiResult != -1">API call failed for id '<value-of select="id"/>'</assert>
            <assert id="R-TEST-001" role="ERROR" test="$__apiResult = 1">Check failed for id '<value-of select="id"/>'</assert>
        </rule>
    </pattern>

</schema>