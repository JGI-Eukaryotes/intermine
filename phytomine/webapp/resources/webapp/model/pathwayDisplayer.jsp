<!-- pathwayDisplayer.jsp -->

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<h2> Pathway ${pathwayName} </h2>

<script type="text/javascript" src="model/d3.v4.min.js" charset="UTF-8"/>
<script type="text/javascript" src="model/pathway.js" charset="utf-8"/>
<link rel="stylesheet" type="text/css" href="model/pathway.css" />

<div id="pathway-widget" align="center">
  <div id="pathway-diagram"/><!--#pathway-diagram-->
  <div id="pathway-expression-table" class="collection-table">
    <h3>Gene Expression</h3>
  </div><!--#pathway-expression-table-->
</div><!--pathway-widget-->
<script type="text/javascript" charset="utf-8">

  jQuery(window).load(function () {
     loadPathway("#pathway-diagram",${jsonPathway});
     jQuery("#pathway-diagram").show();
     loadExpressionTable("#pathway-expression-table",${jsonExpression});
     jQuery("#pathway-expression-table").show();
  });
</script>


<!-- /pathwayDisplayer.jsp -->

