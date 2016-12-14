<!-- pathwayDisplayer.jsp -->

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<h2> Pathway ${pathwayName} </h2>

<link rel="stylesheet" type="text/css" href="model/pathway.css" />
<link rel="stylesheet" type="text/css" href="model/canvasXpress/css/canvasXpress.css" />

<div id="pathway-widget" align="start">
     <div id="diagram-and-ancillary" class="split split-vertical">
       	  <div id="pathway-diagram" class="split split-horizontal">
	       <button id="save">Save SVG</button>
	  </div>
	  <div id="pathway-ancillary-info" class="split split-horizontal">
             <canvas id="canvas" width="200" height="200"></canvas>

      </div>
      </div>
      <div id="pathway-expression-table" class="split split-vertical">
	<h3>Gene Expression</h3>
      </div>
</div><!--#pathway-widget-->

<script type="text/javascript" charset="utf-8">

    var getScripts = function (scripts, callback) {
      var progress = 0;
      scripts.forEach(function(script) { 
        jQuery.getScript(script, function () {
          if (++progress === scripts.length) callback();
	}); 
      });
    };

    var ensureDataReceived = function(dataName){
      var deferred = jQuery.Deferred();
      var wait = setTimeout(function () {
        if (typeof dataName !== "undefined"){
      	  return deferred.resolve();
        } else {
          wait();
        }
      }, 200);
      return deferred.promise();
    };


    jQuery.ajaxSetup({ cache: true });

    var pathwayScripts = [ "model/d3.v4.min.js",
  		    	   "model/pathway.js",
		    	   "model/canvasXpress/js/canvasXpress.min.js"];

    getScripts(pathwayScripts, function(){
                jQuery.getScript("model/pathway-helpers.js"); // contains d3 plugins that are immediately invoked, so execute in callback to ensure d3's loaded
		jQuery.when(ensureDataReceived(${jsonPathway}), 
    			    ensureDataReceived(${jsonExpression}))
	  	      .done(function () {
    		      		     	 loadPathway("#pathway-diagram",${jsonPathway});
    					 loadExpressionTable("#pathway-expression-table",${jsonExpression});
    					 });
      });

</script>

<!--html> ${jsonUrl} </html-->


<!-- /pathwayDisplayer.jsp -->
