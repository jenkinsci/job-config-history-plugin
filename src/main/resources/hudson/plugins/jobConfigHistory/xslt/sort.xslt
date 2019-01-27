<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<!-- strip whitespace -->
	<xsl:strip-space elements="*" />

	<!-- pretty print -->
	<xsl:output method="xml" indent="yes" />

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

	<!-- skip empty node -->
	<xsl:template match="*[not(node())] | *[not(string())]" />

</xsl:stylesheet>
