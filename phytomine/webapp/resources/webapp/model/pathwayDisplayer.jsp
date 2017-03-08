<!-- pathwayDisplayer.jsp -->

<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<link rel="stylesheet" type="text/css" href="model/pathway/css/pathway.css" />

    <div id="pathway" align="left">
      <div id="pathway-widget" align="start">
        <div id="diagram-and-ancillary" class="split split-vertical">
	  <div id="diagram-controls" class="controls">
	    <button id="zoom-in">
	      <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
	        <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
		<path d="M0 0h24v24H0V0z" fill="none"/>
		<path d="M12 10h-2v2H9v-2H7V9h2V7h1v2h2v1z"/>
	      </svg>
	    </button>
            <button id="zoom-out">
              <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        	<path d="M0 0h24v24H0V0z" fill="none"/>
        	<path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zM7 9h5v1H7z"/>
              </svg>
	    </button>
	    <button id="save">Save SVG</button>
           </div>
           <div id="bar-graph-controls" class="controls">
             <button id="get-info" class="no-display">Help</button>
           </div>
           <div id="pathway-diagram" class="split split-horizontal"></div>
	   <div id="pathway-ancillary-info" class="split split-horizontal"></div>
        </div>
        <div id="pathway-expression-table" class="split split-vertical"></div>
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


    jQuery.ajaxSetup({ cache: true }); // flip to true for production!

    var pathwayScripts = [ "model/pathway/js/d3.v4.min.js",
  		    	   "model/pathway/js/pathway.js",
			   "model/pathway/js/sort-by.min.js",
              "model/pathway/js/d3-gridforce.js"];


    getScripts(pathwayScripts, function(){
                jQuery.getScript("model/pathway/js/d3-context-menu.js");
                jQuery.getScript("model/pathway/js/split.js");
                jQuery.getScript("model/pathway/js/d3-gridforce.js");
		jQuery.when(ensureDataReceived(${jsonPathway}), 
    			    ensureDataReceived(${jsonExpression}))
	  	      .done(function () {
    		      		     	 loadPathway(${jsonPathway}, ${jsonExpression});
          				 //
          				 // construct a menu out of the jsonLinks and stash it in #external-links
                         var linkMenu = "<h3 class=\"goog\">This pathway in other organisms</h3><select onchange=\"openPathway(this.selectedIndex-1)\">";
                         linkMenu = linkMenu.concat("<option>Select another organism</option>");
                         var url = [];
                         for( link in ${jsonLinks}) {
                           url.push(${jsonLinks}[link].url);
                           linkMenu = linkMenu.concat("<option>",${jsonLinks}[link].label ,"</option>");
                         }
                         linkMenu = linkMenu.concat("</select>");
          		 var linkContainer = jQuery("#external-links");
			 linkContainer.html(linkMenu);
                         var urlArrayString = "\"".concat(url.join("\",\"")).concat("\"");
                         var linkScript = "<script> url=[".concat(urlArrayString).concat("]; function openPathway(i) { window.open(url[i]); }<\/script>");
                         linkContainer.append(jQuery(linkScript));			 
    		      });
      });

</script>

<!--html> ${jsonUrl} </html-->


<!-- /pathwayDisplayer.jsp -->
