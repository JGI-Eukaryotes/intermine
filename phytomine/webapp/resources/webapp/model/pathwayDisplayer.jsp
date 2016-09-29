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

<div class="body" id="pathway_div"/>

<div id="pathway_expression_div" class="collection-table">
<h3>Gene Expression </h3>
</div>

<script type="text/javascript" charset="utf-8">

  jQuery(window).load(function () {
     loadPathway("#pathway_div",${jsonPathway});
     jQuery("#pathway_div").show();
     loadExpressionTable("#pathway_expression_div",${jsonExpression});
     jQuery("#pathway_expression_div").show();
  });
</script>


<!-- /pathwayDisplayer.jsp -->

