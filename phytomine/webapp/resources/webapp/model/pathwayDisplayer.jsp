<!-- pathwayDisplayer.jsp -->

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<h2> Pathway ${pathwayName} </h2>

<script type="text/javascript" src="model/d3.v4.min.js" charset="utf-8"/>
<script type="text/javascript" src="model/pathway.js" charset="utf-8"/>
<link rel="stylesheet" type="text/css" href="model/pathway.css" />

<div id="pathway-widget" align="start">
     <div id="diagram-and-ancillary" class="split split-vertical">
       	  <div id="pathway-diagram" class="split split-horizontal">
	       <button id="save">Save SVG</button>
	  </div>
	  <div id="pathway-ancillary-info" class="split split-horizontal"></div>
      </div>
      <div id="pathway-expression-table" class="split split-vertical">
	<h3>Gene Expression</h3>
      </div>
</div><!--#pathway-widget-->

<script type="text/javascript" charset="utf-8">

  jQuery(window).load(function () {
     loadPathway("#pathway-diagram",${jsonPathway});
     loadExpressionTable("#pathway-expression-table",${jsonExpression});
     Split(["#diagram-and-ancillary", "#pathway-expression-table"], {
	                                      direction: "vertical",
                                              sizes: [65, 35],
	                                      gutterSize: 8,
                                              cursor: "row-resize"
	                            	      });
     Split(["#pathway-diagram", "#pathway-ancillary-info"], {
	                                     direction: "horizontal",
                                       	     sizes: [50, 50],
	                                     gutterSize: 8,
                                      	     cursor: "col-resize"
	                            	     });
     jQuery("#pathway-diagram").show();
     jQuery("#pathway-expression-table").show();
  });
</script>


<!-- /pathwayDisplayer.jsp -->
