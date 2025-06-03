<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="asmgen">
  <html>
    <style>
      table, tr, td {
      text-align: center;
      vertical-align: top;
      }
    </style>
    <body>
      <table>
	<tr>
	  <xsl:apply-templates select="codechunk"/>
	</tr>
      </table>
     </body>
  </html>
</xsl:template>

<xsl:template match="codechunk">
  <td bgcolor="DDDDDD">
    <table>
      <tr>
	<td bgcolor="EECF00">
	  <xsl:apply-templates select="frame"/>
	</td>
      </tr>
      <tr>
	<td bgcolor="EECF00">
	  <nobr>
	    prologue=<font style="font-family:courier new"><xsl:value-of select="@prologuelabel"/></font>
	    body=<font style="font-family:courier new"><xsl:value-of select="@bodylabel"/></font>
	    epilogue=<font style="font-family:courier new"><xsl:value-of select="@epiloguelabel"/></font>
	  </nobr>
	</td>
      </tr>
      <xsl:apply-templates select="instr"/>
    </table>
  </td>
</xsl:template>

<xsl:template match="instr">
  <tr>
    <xsl:apply-templates select="imc"/>
  </tr>
  <tr>
  </tr>
</xsl:template>

<xsl:template match="instr">
  <xsl:text></xsl:text>
  <xsl:value-of select="@label"/>
  <xsl:if test="@spec != ''">
    <xsl:text> (</xsl:text>
    <xsl:value-of select="@spec"/>
    <xsl:text>)</xsl:text>
  </xsl:if>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="frame">
  <table width="100%">
    <tr>
      <td>
	<nobr>
	  label=<font style="font-family:courier new"><xsl:value-of select="@label"/></font>
	  depth=<xsl:value-of select="@depth"/> 
	  size=<xsl:value-of select="@size"/> 
	  locs=<xsl:value-of select="@locssize"/>
	  args=<xsl:value-of select="@argssize"/>
    	  FP=<xsl:value-of select="@FP"/>
	  RV=<xsl:value-of select="@RV"/>
	</nobr>
      </td>
    </tr>
  </table>
</xsl:template>

<xsl:template match="imc">
  <td>
    <table width="100%">
      <tr>
	<td bgcolor="C7C232" colspan="1000">
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	  <xsl:value-of select="@instruction"/>
	  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="imc"/>
      </tr>
    </table>
  </td>
</xsl:template>

</xsl:stylesheet>
