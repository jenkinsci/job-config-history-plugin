<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				version="1.0"
				xmlns:redirect="http://xml.apache.org/xalan/redirect"
				extension-element-prefixes="redirect"
				xmlns:xalan="http://xml.apache.org/xslt">

	<!-- strip whitespace -->
	<xsl:strip-space elements="*" />


	<!-- sort tags -->
	<xsl:template match="node()|@*">
		<xsl:copy>
			<xsl:apply-templates select="@*">
				<xsl:sort select="name()" />
			</xsl:apply-templates>

			<xsl:apply-templates select="node()">
				<xsl:sort select="name()" />
			</xsl:apply-templates>
		</xsl:copy>
	</xsl:template>

	<!-- pretty print -->
	<xsl:output method="xml" indent="yes" xalan:indent-amount="2"/>

	<!--Skip skipping empty node...-->
	<!-- skip empty node -->
	<!--xsl:template match="*[not(node())] | *[not(string())]" /-->

</xsl:stylesheet>
