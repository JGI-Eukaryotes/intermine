<!-- pathwayDisplayer.jsp -->

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<link rel="stylesheet" type="text/css" href="model/pathway/css/pathway.css" />

<div id="pathway">
  <div id="pathway-widget" align="start">
     <div id="diagram-and-ancillary" class="split split-vertical">
       	  <div id="pathway-diagram" class="split split-horizontal">
	       <button id="save">Save SVG</button>
	  </div>
	  <div id="pathway-ancillary-info" class="split split-horizontal">
          </div>
      </div>
      <div id="pathway-expression-table" class="split split-vertical">
	<h3>Gene Expression</h3>
      </div>
  </div><!--#pathway-widget-->
</div><!--#pathway-->

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


    jQuery.ajaxSetup({ cache: false }); // flip to true for production!

    var pathwayScripts = [ "model/pathway/js/d3.v4.min.js",
  		    	   "model/pathway/js/pathway.js"];


    getScripts(pathwayScripts, function(){
                jQuery.getScript("model/pathway/js/d3-context-menu.js");
                jQuery.getScript("model/pathway/js/split.js");
		jQuery.when(ensureDataReceived(${jsonPathway}), 
    			    ensureDataReceived(${jsonExpression}))
	  	      .done(function () {
    		      		     	 loadPathway("#pathway-diagram",${jsonPathway});
    					 loadExpressionTable("#pathway-expression-table",${jsonExpression});
          				 setPathwayEventHandlers();
          				 Split(["#diagram-and-ancillary", "#pathway-expression-table"], {
                    			 	  direction: "vertical",
                    				  sizes: [65, 35],
                    				  gutterSize: 8,
                    				  cursor: "row-resize"
          					  });
          				 Split(["#pathway-diagram", "#pathway-ancillary-info"], {
                   			 	  direction: "horizontal",
                   				  sizes: [65, 35],
                   				  gutterSize: 8,
                   				  cursor: "col-resize"
          				 });

    		      });
      });

</script>

<!--html> ${jsonUrl} </html-->


<!-- /pathwayDisplayer.jsp -->
