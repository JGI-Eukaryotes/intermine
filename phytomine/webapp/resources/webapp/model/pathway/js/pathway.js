/**
* Copyright (c)2017 The Regents of The University Of California for the work built here on licensed software.
* Authors: Joe Carlson and Patrick Davidson of the DOE JGI Phytozome Group.
*/

/* This lets the linter know d3 is defined globally and so not throw an error */
/*global d3:true*/

// Initialize shared objects, variables //

var geneLinks = {}; // derived from pathway diagram's json, needed in expression table.

/* Styles for svg or other html manipulated by d3 (e.g. expression
 * tables). Use and manipulate this as the single source (don't inline
 * raw styles!!). Why inline? Because we want a pathway diagram
 * downloaded as a single, dependency free file.
*/

var pathAttrs = {
  stroke: "#444",
  strokeWidth: 1.6,
  fill: "transparent"
};

var markerPathAttrs = {
  stroke: "#444",
  strokeWidth: 0.9,
  fill: "transparent"
};

var fontAttrs = {
  family: "'Lucida Grande', Geneva, Lucida, Helvetica, Arial, sans-serif",
  color: "#222",
  size: "12px",
  baselineShift: "4px"
};

var smallFontAttrs = {
  color: "#222",
  size: "10px"
};

var ecLabelAttrs = {
  diagramColor: "#222", //#d80",
  tableColor: "#222",
  highlightedColor: "#f63"
};

var geneLabelAttrs = {
  diagramColor: "#86a",
  tableColor: "#222",
  highlightedColor: "#f63"
};

var expTableAttrs = {
  minFkpmColor: "#dfe",
  maxFkpmColor: "#2a6"
};

var barGraphAttrs = {
  titleFontSize: "12px",
  labelFontSize: "12px"
};

var coefficientOfVariationAttrs = {
  minCvColor: "#acd",
  maxCvColor: "#00c",
  thresholdColors: ["#00a", "#99c", "#adf", "#cef"]
}

var paneSizeAttrs = {
  mainVertical: [65, 35],
  topHorizontal: [60, 40]
}


var adjustPanesHeight = function(d) {

    var topHeight = (d.pathwaySvgDimensions.height >= d.barGraphDimensions.height) ?
                     d.pathwaySvgDimensions.height : d.barGraphDimensions.height;
    var bottomHeight = d.expressionTableDimensions.height;

    var margin = 100;

    if ( (topHeight + margin) < ((d.viewportHeight * paneSizeAttrs.mainVertical[0]) / 100) ) {

     var pathwayHeight = d.viewportHeight - (d.viewportHeight * 0.02);
     var bottomPerc = Math.round(((bottomHeight + margin) / pathwayHeight) * 100);
     var topPerc = 100 - bottomPerc;

     if ( ((bottomHeight + margin) / d.viewportHeight) < bottomPerc ) {
         pathwayHeight = topHeight + bottomHeight + (margin * 2);
         bottomPerc = Math.round(((bottomHeight + margin) / pathwayHeight) * 100) ;
         topPerc = 100 - bottomPerc;
     }

     splits.mainVertical.setSizes([topPerc, bottomPerc]);
     d3.select("#pathway")
       .style("height", pathwayHeight + "px");

    }

};

var splits = {};

var loadPathway = function(json, expression) {
  loadPathwayDiagram("#pathway-diagram", json);
  setDiagramEventHandlers();

  if (expression.data.length > 0) {
    d3.select("#pathway")
      .style("height", "98vh");

    loadExpressionTable("#pathway-expression-table", expression);

    // these needed for interaction between expression data table / graph and diagram
    setExpressionEventHandlers();

    // this makes Split happy, but want it only applied when there are split panes
    d3.select("#pathway-widget")
      .style("height", "100%")

    splits.mainVertical = Split(["#diagram-and-ancillary", "#pathway-expression-table"], {
              direction: "vertical",
              sizes: paneSizeAttrs.mainVertical,
              gutterSize: 8,
              cursor: "row-resize",
              onDrag: setFlex
    });
    splits.topHorizontal = Split(["#pathway-diagram", "#pathway-ancillary-info"], {
             direction: "horizontal",
             sizes: paneSizeAttrs.topHorizontal,
             gutterSize: 8,
             cursor: "col-resize",
             onDrag: setFlex
    });

    var dimensions = {
      pathwaySvgDimensions: getSvgElementDimensions(d3.select("#pathway-svg")),
      barGraphDimensions: getHtmlElementDimensions("how-to-info"),
      expressionTableDimensions: getHtmlElementDimensions("pathway-expression-table-inner"),
      viewportHeight: document.documentElement.clientHeight
    };

    adjustPanesHeight(dimensions);

    // set whether display: flex should be applied: if the diagram
    // fits in the pane, center it vertically and horizontally. Toggled
    // off if not, since messes up scrolling.
    setFlex();
  }

};

// PATHWAY DIAGRAM //

// Helper functions for pathway diagram //

var getSvgElementDimensions = function(element) {
    return element.node().getBBox();
};

var setSvgElementDimensions = function (element) {
  var bbox = getSvgElementDimensions(element);
  var h = Math.ceil(bbox.height === 0 ? 1000 : bbox.height) + 50;
  var w = Math.ceil(bbox.width === 0 ? 1000 : bbox.width) + 50;
  var x = Math.floor(bbox.x) - 50;
  var y = Math.floor(bbox.y) - 50;
  element.attr("width", w)
         .attr("height", h)
         .attr("viewBox",x+ " "+ y + " " + w + " " + h);
};

var setFlex = function() {
  var pathwayDiagramDimensions = getHtmlElementDimensions("pathway-diagram");
  var pd = d3.select("#pathway-diagram");
  var pathwaySvgDimensions = getSvgElementDimensions(d3.select("#pathway-svg"));
  if ((pathwaySvgDimensions.height + 100 >= pathwayDiagramDimensions.height) ||
      (pathwaySvgDimensions.width + 200 >= pathwayDiagramDimensions.width)) {
    pd.style("display", "block")
      .style("align-items", null);
  } else {
    pd.style("display", "flex")
      .style("align-items", "center");
  }
}


var extractMenuItems = function (d) {
  d.genes.forEach( function(e) {
    var gN = e.name;
    geneLinks[gN] = [];
    e.links.forEach( function(f) {
      geneLinks[gN].push({"label":f.label,"url":f.url});
    } )
  });
}

var labelReactions = function (d) {
  var el = d3.select(this);
  // do not touch the label if there are no ECs and no genes.
  if (d.ecs.length == 0 && d.genes.length == 0 ) return;

  el.text("")
    .attr("font-size", fontAttrs.size);

  var ecData = [];
  d.ecs.forEach( function(dd) {
    ecData.push({ content: dd,
                  class: "highlighter diagram ec",
                  baseColor: ecLabelAttrs.diagramColor,
                  highlightedColor: ecLabelAttrs.highlightedColor }
                );
  });
  var pData = [];
  d.genes.forEach( function(dd) {
    pData.push({ content: dd.name,
                 class: "highlighter diagram gene",
                 baseColor: geneLabelAttrs.diagramColor,
           highlightedColor: geneLabelAttrs.highlightedColor }
               );
  });
  el.selectAll("tspan").data(ecData).enter().append("tspan")
                       .attr("x",el.attr("x"))
                       .attr("dy","1em")
                       .attr("class", function (dd) { return dd.class })
                       .text(function(dd) { return dd.content })
                       .style("fill", function(dd) { return dd.baseColor });

  var geneTspan = el.selectAll("tspan").filter(function (e) { return 0;}).data(pData)
                       .enter().append("tspan")
                       .attr("x",el.attr("x"))
                       .attr("dy","1em")
                       .attr("class", function(dd) { return dd.class })
                       .text(function(dd) { return dd.content })
                       .style("fill", function(dd) { return dd.baseColor } );


  // add elements and class names to hide / show excess ecs & genes in reaction label if necessary.
  // see setPathwayEventHandlers() below for the click handlers.

  if (el.selectAll("tspan").size() > 4) {
          el.classed("has-ellipse", true);

       el.append("tspan")
           .attr("x", el.attr("x"))
           .attr("dy", "1em")
           .attr("text-decoration", "underline")
           .classed("reaction-control", true)
           .text("less...")
           .style("fill", fontAttrs.color)
           .style("font-style", "italic");

          el.selectAll("tspan:nth-child(n+4)")
           .classed("no-display hidable", true);

       el.insert("tspan", ":nth-child(4)")
           .attr("x", el.attr("x"))
           .attr("dy", "1em")
           .attr("text-decoration", "underline")
           .classed("reaction-control hidable", true)
           .text("more...")
           .style("fill", fontAttrs.color)
           .style("font-style", "italic");
  }
}

var renderLabels = function (d) {
  var el = d3.select(this);
  if (d.label) {
    var parent = this.parentElement;
    var lines = d.label.split('<br>');
    var lineCtr = -lines.length/2.;
    lines.forEach( function(e) {
      var lineSize = measureText(e,15);
      d3.select(parent).append('g').append("foreignObject")
        .attr("width",lineSize.width)
        .attr("x",(el.attr('text-anchor')==='end')?parseInt(el.attr("x"))-lineSize.width:el.attr('x'))
        .attr("y",parseInt(el.attr("y"))+15*lineCtr++)
        .attr("height",15)
        .append("xhtml:span")
        .html(e);
      });
  }
  return;
};

var measureText = function(pText, pFontSize) {
  // shamelessly stolen code to get the size of a text span
  var lDiv = document.createElement('div');
  document.body.appendChild(lDiv);
  lDiv.style.fontSize = "" + pFontSize + "px";
  lDiv.style.position = "absolute";
  lDiv.style.left = -1000;
  lDiv.style.top = -1000;
  lDiv.innerHTML = pText;
  var lResult = {
    width: lDiv.clientWidth,
    height: lDiv.clientHeight
  };
  document.body.removeChild(lDiv);
  lDiv = null;
  return lResult;
}


var getCoords = function (elem) { // crossbrowser version
    var box = elem.getBoundingClientRect();

    var body = document.body;
    var docEl = document.documentElement;

    var scrollTop = window.pageYOffset || docEl.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docEl.scrollLeft || body.scrollLeft;

    var clientTop = docEl.clientTop || body.clientTop || 0;
    var clientLeft = docEl.clientLeft || body.clientLeft || 0;

    var top  = Math.round(box.top +  scrollTop - clientTop);
    var left = Math.round(box.left + scrollLeft - clientLeft);

    return { top: top,
             left: left,
             midX: Math.round(box.width / 2) + left,
             midY: Math.round(box.height / 2) + top };
};



// Pathway diagram main function //

var loadPathwayDiagram = function(container,json) {

  // the 'save svg' handler.
  d3.select("#save").on("click",function() {
    var svg = document.getElementById("pathway-svg").cloneNode(true);
    var toRemove = d3.select(svg)
      .selectAll(".no-display")
      .remove();
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    var blob = new Blob(["<?xml version=\"1.0\" standalone=\"no\"?>" +
                         "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" "+
                         "\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">" +
                         svg.outerHTML],
                         { type: "image/svg+xml"});
    var url = URL.createObjectURL(blob);
    var link = document.createElement("a");
    link.href = url;
    link.download = "pathway.svg";
    document.body.appendChild(link);
    link.click();
    setTimeout(function() {
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    }, 0);
  });

  // the drag handler
  var eventStart;
  var moveablePath;
  var drag = d3.drag()
             .on("start",function() {
               eventStart = d3.mouse(this);
               moveablePath = d3.selectAll("line.moveable");
               moveablePath.each(function (dd) {
                 dd.origStart = {x: dd.x1, y: dd.y1};
                 dd.origEnd = {x: dd.x2, y: dd.y2};
               });
             })
             .on("drag",function(d) {
               d.x = d3.event.x;
               d.y = d3.event.y;
               var mouse = d3.mouse(this);
               d3.select(this).attr("transform","translate("+(d.x-d.baseX)+","+(d.y-d.baseY)+")");
               refreshPositions();
               setSvgElementDimensions(svgContainer);
             });


  // make the SVG Container, we'll add height and width when everything's constructed
  var svgContainer = d3.select(container).append("svg")
                                         .attr("id","pathway-svg")
                                         .style("font-family", fontAttrs.family)
                                         .style("color", fontAttrs.color);

  
  // defs: arrowheads, dropshadows
  var refX = 7;
  var refY = 2.5;
  svgContainer.append("defs").append("marker")
    .attr("id", "arrowend")
    .attr("refX", refX)
    .attr("refY", refY)
    .attr("markerWidth", refX)
    .attr("markerHeight", 2 * refY)
    .attr("orient", "auto")
    .style("stroke", markerPathAttrs.stroke)
    .style("stroke-width", markerPathAttrs.strokeWidth)
    .style("fill", markerPathAttrs.fill)
    .append("path").attr("d", "M0," + 2 * refY + "L" + refX + "," + refY + "L0,0");

  svgContainer.select("defs").append("marker")
    .attr("id", "n_degree_arrowend")
    .attr("refX", refX)
    .attr("refY", refY)
    .attr("markerWidth", refX)
    .attr("markerHeight", 2 * refY)
    .attr("orient", "5.5")
    .style("stroke", markerPathAttrs.stroke)
    .style("stroke-width", markerPathAttrs.strokeWidth)
    .style("fill", markerPathAttrs.fill)
    .append("path").attr("d", "M0," + 2 * refY + "L" + refX + "," + refY + "L0,0");

  svgContainer.select("defs").append("filter")
      .attr("id", "dropshadow")
      .attr("height", "120%")
    .append("feGaussianBlur")
      .attr("in", "SourceAlpha")
      .attr("stdDeviation", "3"); // how much to blur

  svgContainer.select("filter")
    .append("feComponentTransfer")
      .attr("xmlns", "http://www.w3.org/2000/svg")
    .append("feFuncA")
      .attr("type", "linear")
      .attr("slope", "0.15"); // tweak the opacity

  svgContainer.select("filter")
    .append("feOffset")
      .attr("dx", "-2")
      .attr("dy", "2")
      .attr("result", "offsetblur"); // how much to offset

  svgContainer.select("filter")
    .append("feMerge")
    .append("feMergeNode"); // contains the offset blurred image

  svgContainer.select("feMerge")
    .append("feMergeNode")
      .attr("in", "SourceGraphic") // contains the element that the filter is applied to


  // Make an all-encompassing group in the svg
  var masterGroup = svgContainer.selectAll("g")
                                .data([{"id":"master"}])
                                .enter()
                                .append("g")
                                .attr("id",function(d) { return d.id});

  // for testing. Draw a ruler
  //masterGroup.append('line')
              //.attr('id','ruler')
              //.attr('x1','10')
              //.attr('y1','10')
              //.attr('x2','60')
              //.attr('y2','10')
              //.attr('stroke','red')
              //.attr('stroke-width','2');

  // and a div for the tooltip
  var tooltipDiv = d3.select("body")
                     .append("div")
                     .attr("id","tooltip-container")
                     .attr("align", "start")
                     .style("opacity",0)
                     .style("display", "none");


  // Make a <g> for the node groups and associate each node with its node group
  var containerData = [];
  var parents = {};
  json.groups.forEach( function(d) {
    // each group has up to 3 members. This will generate a name
    // that is unique enough
    var thisId = d[0] + ":" + d[1] + ":" + d[2];
    containerData.push({"id":thisId});
    d.forEach( function(e) { parents[e] = thisId; });
  });

  // and insert them. This is initially unpositioed.
  // we'll position it as we populate it with the elements
  var nodeGroups = masterGroup.selectAll("g")
              .data(containerData)
              .enter()
              .append("g")
              .attr("id",function(d){return d.id; })
              .attr("class","moveable")

  var processedGroups = [];
  json.nodes.forEach( function(d) {

    // every node is already part of a group, even if the
    // group only has 1 node in it. The json was generated this way.
    var parentGroupId = parents[d.id];

    // the group that owns this node has already been registered
    var parentGroup = masterGroup.selectAll("g")
                                 .filter(function(dd){return dd.id === parentGroupId;});

    // if the type of the node is "reaction" or "link", the location
    // of this node is the anchor point for the group. This is where
    // the line goes to.
    if ( d.type === 'link' || d.type == 'reaction') {
      // we'll keep track of both the original (x,y) and current (x,y)
      parentGroup.each(function(e) { e.x = 5*d.x; e.y = 15*d.y ; e.baseX = e.x; e.baseY = e.y} );
    }

    // each node has a rectangle and text. These will be their own group one level lower.
    var nodeGroup = parentGroup.selectAll("g")
                               .filter(function(dd) { return dd.id === d.id; })
                               .data([{id:d.id,x:5.*d.x,y:15.*d.y}])
                               .enter()
                               .append("g")
                               .attr("id", d.id);

    // put a rectangle in the group, and with tooltip if it's a reaction
    nodeGroup.selectAll("rect")
               .data([{"id":d.id,"groupId":parentGroupId,"tooltip":d.tooltip}])
               .enter()
               .append("rect")
               .attr("id", "rect-" + d.id)
               .attr("class","rect no-display")
               .attr("x", 5*d.x)
               .attr("y", 15*d.y);

    // and the text holder, with a tooltip that will show up if it's in the data
    nodeGroup.selectAll("text")
               .data([{"id":d.id,"label":d.label,
                        "type":d.type, "xCoor":d.x,
                        "yCoor":d.y, "labelHeight":d.height,
                        "tooltip":d.tooltip, "genes":d.genes, "ecs":d.ecs}])
               .enter().append("text")
               .attr("x", function(d){
                 if (d.type === "reaction" || d.type === "link") {
                   return 5 * d.xCoor;
                 }
                 return 5*d.xCoor + 10;
                 })
               .attr("y", function(d){
                 if (d.labelHeight === 1) {
                   return 15 * d.yCoor + 8;
                 } else if (d.type === "reaction" && ( d.genes.length + d.ecs.length > 3)) {
                   return 15 * (d.yCoor - 1.5);
                 }
                 return 15 * d.yCoor;
                 })
               .attr("text-anchor", function(d){
                 if (d.type === "link") {
                   return "beginning";
                 }
                 return "end";
                 })
               .attr("text-anchor",(d.type==="input"||d.type==="output")?"beginning":"end")
               .attr("alignment-baseline","middle")
               .attr("class", function(dd){
                 if (dd.type === "reaction") {
                   return "reaction";
                 }
                 return "label";
                 })
              .on("mouseover",function() {
                if ( d.tooltip ) {
                  tooltipDiv.html("<span id='tooltip-text'>" + d.tooltip + "</span>" +
                  "<span id='dismiss-text'>X</span>");
                  var coords = getCoords(this);
                  var yloc = coords.top - 60;
                  var xloc = coords.midX - 50;

                  tooltipDiv.style("left", xloc + "px")
                     .style("top", yloc + "px");

                  tooltipDiv.transition()
                     .duration(200)
                     .style("opacity", 1)
                     .style("display", "block");

                  tooltipDiv.select("#dismiss-text").on("click", function() {
                    tooltipDiv.transition()
                    .duration(500)
                    .style("opacity",0)
                    .style("display", "none")});;
                }
           });

  });

  // dismiss the tooltip if the pathway diagram is scrolled

  d3.select("#pathway-diagram").on("scroll", function() {
                tooltipDiv.transition()
                   .duration(500)
                   .style("opacity", 0)
                   .style("display", "none");
           });

  // the zoom handler

  var zoomDiagram = d3.zoom()
                      .scaleExtent([.512, 1.953125])
                      .on("zoom",function() {
                           var transform = d3.zoomTransform(this);
                                    masterGroup.attr("transform", "translate(30, 10) scale(" + transform.k + ")");
                               setSvgElementDimensions(svgContainer);
                           setFlex();
                           });


  d3.selectAll("text.label").each(renderLabels);
  d3.selectAll("text.reaction").each(labelReactions);
  d3.selectAll("text.reaction").each(extractMenuItems);

  // was thinking of pushing the addTooltip event handler here...
  //d3.selectAll("text").each(function(e) { addTooltips(tooltipDiv,e)});


  // use the origStart and origEnd objects to limit the firing of these.

  function lineFunction(f) {
    // rules for drawing lines between reactions and linking reaction products
    if ((f.x1 === f.x2) || (f.y1 === f.y2)) {
      // where either start and end values on either axis is identical
      return "M" + f.x1 + "," + f.y1 +
             "L" + f.x2 + "," + f.y2;
      // return a straight line between start and end points
    } else if ((f.x2 > f.x1) && (f.y2 > f.y1)) {
      // where the end values on both axes are greater than the start values
      return "M" + f.x1 + "," + f.y1 +
             "L" + f.x2 + "," + f.y1 +
             "L" + f.x2 + "," + f.y2;
      // return a line that runs right on the x-axis to the x2 point, turns +90deg and runs to the y2 point
    } else {
      return "M" + f.x1 + "," + f.y1 +
             "L" + f.x1 + "," + f.y2 +
             "L" + f.x2 + "," + f.y2;
      // return a line that runs on the y-axis to the y2 point, turns +90 deg and runs to the x2 point
    }
  }

  function inputArcFunction(f) {
    // how to draw an arc for an input compound
    var str = "M"+f.x1+","+f.y1+
              " A"+Math.abs(f.x1-f.x2)+"," +
              Math.abs(f.y1-f.y2)+" 0 0 0 "+
              f.x2+","+f.y2;
    return str;
  }

  function outputArcFunction(f) {
    // how to draw an arc for an output compound
    var str = "M"+f.x1+","+f.y1+
              " A"+Math.abs(f.x1-f.x2)+","+
              Math.abs(f.y1-f.y2)+" 0 0 0 "+
              f.x2+","+f.y2;
    return str;
  }

  // create links.
  // This data structure is what we're going to pass to the force layout
  var forceLinkData = [];
  json.links.forEach( function(d) {
    // source and target
    // at this point we don"t know what we have.
    var source = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.source;});
    var target = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.target;});
    var type = d.type;

    if (type === 'input') {
      // draw an arc with arrow for inputs.
      var thisGroup = masterGroup.selectAll("g")
                                 .filter(function(ddd) { return ddd.id===parents[d.source]; });
      thisGroup.selectAll("path").data([{"id":d.source+":"+d.target,
                                         "source":d.source,
                                         "target":d.target,
                                         "x1":parseInt(source.attr("x"))+5,
                                         "y1":parseInt(source.attr("y"))+5,
                                         "x2":parseInt(target.attr("x"))+5,
                                         "y2":parseInt(target.attr("y"))+5 }],
                                         function(dd){return dd.id===d.source+":"+d.target;})
                                         .enter()
                                         .append("path")
                                         .attr("d",inputArcFunction)
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill);

    } else if (type === 'output') {
      // draw an arc with arrow for inputs.
      var thisGroup = masterGroup.selectAll("g")
                                 .filter(function(ddd) { return ddd.id===parents[d.source]; });
      thisGroup.selectAll("path").data([{"id":d.source+":"+d.target,
                                         "source":d.source,
                                         "target":d.output,
                                         "x1":parseInt(source.attr("x"))+5,
                                         "y1":parseInt(source.attr("y"))+5,
                                         "x2":parseInt(target.attr("x"))+5,
                                         "y2":parseInt(target.attr("y"))+5 }],
                                         function(dd){return dd.id===d.source+":"+d.target;})
                                         .enter()
                                         .append("path")
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill)
                                         .attr("marker-end","url(#n_degree_arrowend)")
                                         .attr("d",outputArcFunction);
    } else if (type === 'link') {
      // a movable line
      forceLinkData.push( {"id":parents[d.source]+'->'+parents[d.target], "source":parents[d.source],"target":parents[d.target] } );
    }

  });

  var link = masterGroup.append("g").selectAll("line")
                                    .data(forceLinkData)
                                    .enter()
                                    .append("line")
                                    .attr("marker-end","url(#arrowend)")
                                    .attr('stroke-width',pathAttrs.strokeWidth)
                                    .attr('stroke',pathAttrs.stroke)
                                    .attr('class','moveable');

  // discover how large the svg is and explicitly set it on the root <svg> node
  setSvgElementDimensions(svgContainer);

  // initiate the layout.
  simulation = d3.forceSimulation()
                     .force("link", d3.forceGridLink())
                     .force("collision", d3.forceGridCollision())
                     .force("gravity",d3.forceGravity())
                     ;

  simulation.force("link").distance(100).strength(1).id(function (d) { return d.id;});
  simulation.force("collision").strength(1);
  simulation.force("collision").exclude(forceLinkData);
  simulation.force("collision").distanceX(200).distanceY(75);
  simulation.force("gravity").strengthY(function(d) { return 10*d.y/svgContainer.attr('height');} );

  simulation.nodes(containerData).on("tick", refreshPositions);
  simulation.force("link").links(forceLinkData);

  simulation.on('end',function() {
    // install the zoom handler on the zoom buttons
    d3.select("#zoom-in").on("click", function() {
         zoomDiagram.scaleBy(svgContainer, 1.25);
        });
    d3.select("#zoom-out").on("click", function() {
         zoomDiagram.scaleBy(svgContainer, 0.8);
        });
    // install a drag handler for every group inside the master group that has an 'id'
    masterGroup.selectAll("g.moveable").filter(function(dd) { return (dd.id && !(dd.id in parents)); }).call(drag);

    // development. remove for production
    svgContainer.on('mousemove', function() { console.log("Mouse is at "+d3.mouse(this)); });


   });

  function refreshPositions() {
    simulation.force("gravity").strengthY(function(d) { return 10*d.y/svgContainer.attr('height');} );
    link
        .attr("x1", function(d) { return d.source.x+5; })
        .attr("y1", function(d) { return d.source.y; })
        .attr("x2", function(d) { return d.target.x+5; })
        .attr("y2", function(d) { return d.target.y; });

    nodeGroups
        .attr('transform',function (d) { return 'translate ('+(d.x-d.baseX)+','+(d.y-d.baseY)+')';});

    setSvgElementDimensions(svgContainer);

   }
};


// EXPRESSION TABLE //

//* Helper functions for displaying the expression table *//

var toLowerCaseAndSpacesToDashes = function (string) {
  return string.toLowerCase().replace(/ /g, "-");
};

var showTable = function(tableId) {
  d3.selectAll("table.collection-table")
    .style("display", "none");

  var tableToDisplay = "#" + tableId;

  d3.select(tableToDisplay)
    .style("display", "block");

  // show the coefficient of variation from the expression table in the diagram too
  showCoefficientsOfVariation(tableToDisplay);

};

var showCoefficientsOfVariation = function(table) {
    var allCoeffVar = [];
    var genes = d3.selectAll(table + " td.gene")

    genes.each(function(d, i) {
        allCoeffVar.push(d.coeffVar);
    });
    var cvExtent = d3.extent(allCoeffVar);
    var colorScale = setColorScale(cvExtent, [coefficientOfVariationAttrs.minCvColor, coefficientOfVariationAttrs.maxCvColor]);

    var typeColorScale = d3.scaleThreshold()
                           .domain(cvExtent)
                           .range(coefficientOfVariationAttrs.thresholdColors);

    genes.each(function(d, i) {
        d3.selectAll("tspan.highlighter")
          .filter( function(dd) {
            return dd.content && dd.content.match(d.content); })
          .each( function (e) {
            var target = d3.select(this).style("fill", function () { return colorScale(d.coeffVar)});
          });
        d3.selectAll("td.highlighter")
          .filter( function(dd) {
            return dd.content && dd.content.match(d.content); })
          .each( function (e) {
            var target = d3.select(this).style("color", function () { return colorScale(d.coeffVar)});
          });
        d3.selectAll("td.coeff-var")
          .filter( function(dd) {
            return dd.gene && dd.gene.match(d.content); })
          .each( function (e) {
            var target = d3.select(this)
                     .style("color", function () { return colorScale(d.coeffVar)})
          });

     });
};


var createExperimentGroupSelect = function (container, json) {

  var select = container.append("label")
                        .text("Experiment Group: ")
                        .append("select")
                        .attr("id", "experiments-select");

  select.selectAll("option")
        .data(json.data)
        .enter()
        .append("option")
        .attr("value", function(d) {
          return "table-" + toLowerCaseAndSpacesToDashes(d.group);
        })
        .text(function(d) {
          return d.group;
        });

  select.on("change", function(){
    var newTableId = d3.select(this).node().value;
    showTable(newTableId);
  });

};

var flattenJson = function(data) {
  var result = {};
  function recurse (cur, prop) {
    if (Object(cur) !== cur) {
      result[prop] = cur;
    } else if (Array.isArray(cur)) {
      for (var i=0, l=cur.length; i<l; i++) {
        recurse(cur[i], prop ? prop + "." + i : "" + i);
      }
      if (l === 0) result[prop] = [];
    } else {
      var isEmpty = true;
      for (var p in cur) {
        isEmpty = false;
        recurse(cur[p], prop ? prop + "." + p : p);
      }
      if (isEmpty) result[prop] = {};
    }
  }
  recurse(data, "");
  return result;
};

var getMinMaxFpkm = function(data) {
  var flattenedData = flattenJson(data);
  var fpkmArr = [];
  for (var key in flattenedData) {
    if (flattenedData.hasOwnProperty(key) && /fpkm/.test(key)) {
      fpkmArr.push(parseFloat(flattenedData[key]));
    }
  }
  return d3.extent(fpkmArr);
};

var setColorScale = function(minMaxVal, minMaxColors) {
    if ( minMaxVal[0] === 0 ||
         minMaxVal[1] === 0 ||
         ( minMaxVal[0] < 0 && minMaxVal[1] > 0) ||
         ( minMaxVal[0] > 0 && minMaxVal[1] < 0)) {
        return d3.scaleLinear()
                 .domain(minMaxVal)
                 .range(minMaxColors);
    }
    return d3.scaleLog()
             .domain(minMaxVal)
             .range(minMaxColors);
};

var doLinkout = function(n) {
  window.open(n,"_blank");
};

var makeBarplot = function (d, geneData, colorScale) {

    var plotType = d.plotType;
    var data = [];
    // this should be part of prepping data where called
    var longestName = 0;
    if (plotType === "Condition") {
        for ( var gene in geneData ) {
          if (geneData.hasOwnProperty(gene)) {
            var result = geneData[gene].filter( function (obj) { return obj.sample === d.content })[0];
            if (gene.length > longestName) longestName = gene.length;
            data.push({name: gene, fpkm: result.fpkm});
          }
        }
    } else {
        geneData.samples.forEach( function (condition) {
            if (condition.sample.length > longestName) longestName = condition.sample.length;
            data.push({name: condition.sample, fpkm: condition.fpkm});
        });
    }

    var minMaxFkpm = getMinMaxFpkm(data);
    var container = document.getElementById("pathway-ancillary-info");
    var containerWidth = container.offsetWidth;
    var containerHeight = container.offsetHeight;
    var margin = {top: 100, right: 50, bottom: 100, left: (longestName * 5) + 10}; // revisit these magic numbers with a better calculation based on font size
    var width =  containerWidth - margin.left - margin.right;
    // var height = (containerHeight) - margin.top - margin.bottom;

    // d3's bandwidth method for range [0, height] produces height / 1.333333
    // so, to get a bandwidth of n pixels:  (height / 1.333333) / data.length = n, aka
    var heightWMaxBand = 25 * data.length * 1.333333;
    var heightWMinBand = 15 * data.length * 1.333333;

    var plotHeight;
    var svgHeight;
    var yOffset = margin.top;

    if ( heightWMinBand + 100 > containerHeight ) {
      // lots of data relative to pane size: let it scroll
      plotHeight = heightWMaxBand;
      svgHeight = heightWMaxBand + 200;
    } else if ( heightWMaxBand + 100 < containerHeight ) {
      // data fits comfortably in pane
      plotHeight = heightWMaxBand;
      svgHeight = containerHeight - 5;
      // center it vertically
      yOffset = ( containerHeight - plotHeight ) / 2;
    } else {
      // something in between what demands min or max bandwidth
      plotHeight = containerHeight - margin.top - margin.bottom;
      svgHeight = containerHeight - 5;
    }

    // var x = d3.scaleBand().rangeRound([0, width]).padding(0.05);
    // var y = d3.scaleLinear().rangeRound([height, 0]);
    var x = d3.scaleLinear().rangeRound([0, width - 10]);
    var y = d3.scaleBand().rangeRound([0, plotHeight]).padding(0.25);



    // x.domain(data.map(function(d) { return d.name; }));
    // y.domain([0, minMaxFkpm[1]]);
    x.domain([0, minMaxFkpm[1]]);
    y.domain(data.map(function(d) { return d.name; }));

    clearBarGraphPane();

    d3.select("#get-info")
      .classed("no-display", false);

    var xOffset = ( margin.left + containerWidth - width ) / 2 ;

    var chart = d3.select("#pathway-ancillary-info")
                  .append("svg")
                  .attr("id", "bar-graph")
                  .attr("height", svgHeight)
                  .attr("width", containerWidth)
                  .append("g")
                  .attr("transform", "translate(" + xOffset + "," + yOffset + ")")


    chart.append("g")
         .attr("class", "axis axis--x")
         .attr("transform", "translate(0," + plotHeight + ")")
         .call(d3.axisBottom(x))
         .append("text")
         .attr("x", width / 2)
         .attr("dy", "2em")
         .attr("text-anchor", "middle")
         .text("FPKM");

    chart.append("g")
         .attr("class", "axis axis--y")
         .call(d3.axisLeft(y))
         .append("text")
         .attr("transform", "rotate(-90)")
         .attr("y", 6)
         .attr("dy", "0.71em")
         .attr("text-anchor", "end")
        //  .text(plotType);

    chart.selectAll(".bar")
        .data(data)
        .enter().append("rect")
        .attr("class", "bar")
        .style("fill", function(d) { return colorScale(d.fpkm); })
        // .attr("x", function(d) { return x(d.name); })
        // .attr("y", function(d) { return y(d.value); })
        // .attr("height", function(d) { return height - y(d.value); })
        // .attr("width", x.bandwidth());
        .attr("x", 1)
        .attr("y", function(d) { return y(d.name); })
        .attr("width", function(d) { return x(d.fpkm); })
        .attr("height", y.bandwidth());

    chart.append("text")
         .attr("id", "bar-graph-title")
         .attr("x", (width / 2) - (xOffset / 3) )
         .attr("y", 0 - ( margin.top / 2 ))
         .attr("text-anchor", "middle")
         .style("font-size", barGraphAttrs.titleFontSize)
         .append("tspan")
         .text(plotType + " ")
         .append("tspan")
         .attr("font-style", "italic")
         .text(d.content)
         .append("tspan")
         .attr("font-style", "normal")
         .text(" in the ")
         .append("tspan")
         .attr("font-style", "italic")
         .text(d.experimentDisplayName)
         .append("tspan")
         .attr("font-style", "normal")
         .text(" experiment group");

    chart.append("text")
         .attr("transform", "translate(" + (width / 2) + "," + (plotHeight + 40) + ")")
         .attr("text-anchor", "middle")
         .attr("font-size", barGraphAttrs.labelFontSize)
         .text("FPKM");

    var barGraphg = d3.select("#bar-graph g");
    var targetWidth = getSvgElementDimensions(barGraphg).width;
    var title = d3.select("#bar-graph-title");
    var titleWidth = getSvgElementDimensions(title).width + (xOffset / 2) + margin.right;
    if (titleWidth  > targetWidth) targetWidth = titleWidth;

    var pathwayWidth = (getHtmlElementDimensions("pathway")).width;
    var paneWidthTarget = Math.round( targetWidth / pathwayWidth * 100 );
    var paneSize = Math.round( splits.topHorizontal.getSizes()[1]);
    if ( paneWidthTarget > paneSize ) {
      splits.topHorizontal.setSizes([100 - (paneWidthTarget + 5), paneWidthTarget + 5]);
      makeBarplot(d, geneData, colorScale);
    }

};

var getHtmlElementDimensions = function (element) {
  var el = document.getElementById(element);
  return {height: el.offsetHeight, width: el.offsetWidth};
}

var mean = function (data){
    var sum = data.reduce(function(sum, value){
         return sum + value;
     }, 0);

    return sum / data.length;
};

var standardDeviation = function (values){
    var avg = mean(values);

    var squareDiffs = values.map(function(value){
         var diff = value - avg;
         var sqrDiff = diff * diff;
         return sqrDiff;
     });

    var avgSquareDiff = mean(squareDiffs);

    var stdDev = Math.sqrt(avgSquareDiff);
    return stdDev;
};

var coefficientOfVariation = function (values) {
    var stdDev = standardDeviation(values) / mean(values);
    return parseFloat(stdDev.toFixed(3));
};


var clearBarGraphPane = function () {
  var barGraph = document.getElementById("bar-graph");
  if (barGraph) barGraph.parentNode.removeChild(barGraph);
};

var setInitialGraphInfo = function (){

       clearBarGraphPane();

       var info = d3.select("#pathway-ancillary-info")
                    .append("div")
                      .attr("id", "bar-graph")
                      .classed("initial-info", true)
                      .html('<div class="flexed" id="how-to-info">' +
                              "<h3>How-to</h3>" +
                                       "<h4>Gene and EC labels</h4>" +
                                       "<ul>" +
                                         "<li>Hover over a gene or ec label in the diagram to see it highlighted in the table and vice versa.</li>" +
                                         "<li>Left click to have that highlighting persist.</li>" +
                                       "</ul>" +
                                       "<h4>Expression experiment data</h4>" +
                                       "<ul>" +
                                          "<li>Tabular data expresses FPKM values in a heat map across conditions.</li>" +
                                          "<li>Right click on a gene or condition label to see a context menu.</li>" +
                                          "<li>For genes, context menus have links to reports in Phytozome, Phytomine and JBrowse. Both gene and condition context menus have options to get a bar plot of expression levels.</li>" +
                                       "</ul>" +
                            "</div>"
                                     );

      d3.select("#get-info")
        .classed("no-display", true);

};

var sortOnEc = function (jsonIn) {

  // make a quick and dirty clone, leaving jsonIn unmutated
  var json = JSON.parse(JSON.stringify(jsonIn));

  json.data.forEach( function(experiment) {
    var outGenes = []
    experiment.genes.forEach( function(g) {
      g.enzyme.forEach( function (enz) {
        var geneOut = {
            enzyme: enz,
            gene: g.gene,
            samples: g.samples
          }
        outGenes.push(geneOut);
      })
    })
    experiment.genes = outGenes;
    experiment.genes.sort(sortBy("enzyme", "gene"))
  })

  return json;

}


var loadExpressionTable = function(initialContainer, rawJson) {

  var json = sortOnEc(rawJson);
  // var json = rawJson;

  setInitialGraphInfo();

  var container = d3.select(initialContainer)
                    .append("div")
                      .attr("id", "pathway-expression-table-inner");

  container.append("h3")
           .text("Gene Expression");

  if (json.data.length > 1) createExperimentGroupSelect(container, json);

  // the highest level is an experiment group. We'll process each
  // of these and generate a separate table for each.

  json.data.forEach( function(d) {

    // create the label for this table if only one table
  if (json.data.length === 1) {
     container.selectAll("label")
              .data([{ caption: "Experiment Group: " + d.group,
                    id: "caption-" + d.idName }])
              .enter()
              .append("label")
              .text(function(d) { return d.caption; });
    }

    // prescan to find all sample names/EC. There may be holes in the table
    // so we do not want to just push onto a list

    var geneData = {};
    var sampleNames = {};
    var ec = {};
    d.idName = d.group.toLowerCase().replace(/ /g, "-");


    d.genes.forEach( function(e) {
      e.samples.forEach( function(f) { sampleNames[f.sample] = 1; });
      geneData[e.gene] = e.samples;
      ec[e.gene] = e.enzyme;
    });

    // find minimum and maximum fpkm values for the experiment for setting a color scale.
    var minMaxFkpm = getMinMaxFpkm(d);
    var colorScale = setColorScale(minMaxFkpm, [expTableAttrs.minFkpmColor, expTableAttrs.maxFkpmColor]);

    // put the experiment's data into a table.
    var table = container.append("table")
                         .attr("class","collection-table")
                         .attr("id","table-"+d.idName);

    // make the data structure for the table headings
    var cols =[ { content: "EC",
                  class: "ec" },
                { content: "Gene",
                  class: "gene" },
                { content: "Coefficient of variation",
                  class: "coeff-var",
                  ttText: "Coefficient of variation calculated across conditions per gene in this experiment. Coloring for gene and CV value labels ranges from lightest blue for least variation to darkest for greatest." }];
    for( var s in sampleNames ) {
      cols.push( { content: s,
                   class: "condition",
                   experiment: d.idName,
                   experimentDisplayName: d.group,
                   plotType: "Condition" }
               );
    }

    var thead = table.append("thead")
                     .append("tr")
                     .selectAll("th")
                     .data(cols)
                     .enter()
                     .append("th")
                     .attr("class", function(d) { return d.class; })
                     .text(function(d) { return d.content; })
                     .attr("title", function(d) { return d.ttText;});


    table.selectAll(".condition")
         .each( function (d) {
           var menu = [{ title: "Plot " + d.content,
                         action: function (elem, d, i) { makeBarplot( d, geneData, colorScale ) }}];
           var thisCondition = d3.select(this);
           thisCondition.on("contextmenu", d3.contextMenu(menu));
         })

    var body = table.append("tbody");
    // and now the rows

    d.genes.forEach( function (gene) {

      var geneName = gene.gene

      var lookup = {};
      var fpkmArr = []
      // hash the results, also make an array of the fpkms
      gene.samples.forEach( function(e) {
           lookup[e.sample] = e.fpkm;
           fpkmArr.push(parseFloat(e.fpkm));
      });

      var coeffVar = coefficientOfVariation(fpkmArr);

      var cols = [{ content: gene.enzyme,
                              class: "ec highlighter table",
                              baseColor: ecLabelAttrs.tableColor,
                              highlightedColor: ecLabelAttrs.highlightedColor },
                  { class: "highlighter table gene",
                    content: geneName,
                          coeffVar: coeffVar,
                    experiment: d.idName,
                    experimentDisplayName: d.group,
                    baseColor: geneLabelAttrs.tableColor,
                    highlightedColor: geneLabelAttrs.highlightedColor,
                    plotType: "Gene" },
                  { content: coeffVar,
                    class: "coeff-var",
                    gene: geneName }];
      for( s in sampleNames ) {
        if (s in lookup) {
          cols.push({"content":lookup[s], "class": "fpkm result"});
        } else {
          cols.push({"content":"N/A", "class": "fpkm not-available"});
        }
      }

      var menu = [];
      geneLinks[geneName].forEach( function(f) {
        if (/PhytoWeb/.test(f.label)) f.label = "Phytozome Gene Report";
        menu.push({title: f.label,
                   action: function() { doLinkout(f.url)}});
      });
      menu.push({ title: "Plot " + geneName,
                  action: function(elem, d, i) { makeBarplot(d, gene, colorScale) }});


      var tr = body.append("tr");

      tr.selectAll("td")
        .data(cols)
        .enter()
        .append("td")
        .html( function(d) { return d.content; })
        .attr("class", function(d) { return d.class;})
        .style("background-color", function(d){
            if (/result/.test(d.class)) {
                return colorScale(d.content);
            }
        });

      tr.select("td.gene")
          .on("contextmenu", d3.contextMenu(menu));

    });

  });

  // show the initial table and hide the rest
  var initialTable;
  if (json.data.length > 1) {
    initialTable = d3.select("#experiments-select").node().value;
  }  else {
    initialTable = d3.select(".collection-table").attr("id");
  }
  showTable(initialTable);

};

// Global event handlers

// svg diagram specific handlers

var setDiagramEventHandlers = function () {

  var putAsTopLayer = function (mastergroup) {
      document.getElementById("master").appendChild(mastergroup);
  };

  d3.selectAll(".reaction-control")
    .on("click", function(d){
         var parent = d3.select(this.parentNode);
         var previousUncle = d3.select(this.parentNode.previousSibling); // the rect to be used as a background, text's previous sibling
         var masterGroup= this.parentNode.parentNode.parentNode; // (this = tspan) > text > g > g-mastergroup

         var toggled = parent.selectAll(".hidable")
               .classed("no-display", function (dd, i) {
                 return !d3.select(this).classed("no-display");
             })

         toggled.attr("dy", function (dd){
                  var el = d3.select(this);
                  if (el.classed("no-display reaction-control")) {
                     return "0em";
                  }
                  return "1em";
           });

         previousUncle.classed("no-display", function (dd, i) {
                 return !d3.select(this).classed("no-display");
          });

         var parentDimensions = getSvgElementDimensions(parent); // values after the click

         if (previousUncle.classed("no-display")) {
          previousUncle.attr("height", parentDimensions.height)
              .attr("width", parentDimensions.width)
              .attr("x", parentDimensions.x)
              .attr("y", parentDimensions.y)
              .attr("fill", "#fff")
              .style("filter", null);
         } else {
          previousUncle.attr("x", parentDimensions.x - 8)
              .attr("y", parentDimensions.y - 5)
              .attr("height", parentDimensions.height + 10)
              .attr("width", parentDimensions.width + 10)
              .attr("fill", "#fff")
              .style("filter", "url(#dropshadow)");

              // put the masterGroup containing the reaction as the last element in the diagram, i.e. as the top layer
              putAsTopLayer(masterGroup);
         }

         // reset the pathway diagram's size;
         setSvgElementDimensions(d3.select("#pathway-svg"));
    });

    // d3.selectAll(".reaction.label")
    //   .on("mouseover", function(d) {

    //          var parent = d3.select(this.parentNode);
    //          var previousUncle = d3.select(this.parentNode.previousSibling); // the rect to be used as a background, text's previous sibling
    //          var parentDimensions = getElementDimensions(parent);
    //          previousUncle.attr("height", parentDimensions.height)
    //           .attr("width", parentDimensions.width)
    //           .attr("x", parentDimensions.x)
    //           .attr("y", parentDimensions.y)
    //           .attr("fill", "#fff");

    //          var masterGroup= this.parentNode.parentNode; // (this = text > g > g-mastergroup
    //          putAsTopLayer(masterGroup);
    // });

};


// handlers for expression tables / graph / interaction between these and diagram

var setExpressionEventHandlers = function () {

    var setColor = function (elem, elemContent, clicked) {
    elem.classed("clicked", clicked);
    d3.selectAll(".highlighter")
      .filter( function (dd) {
        return dd.content && dd.content.match(elemContent);
      })
      .each( function (e) {
        d3.select(this).classed("highlighted", function (dd, i) {
                                return !d3.select(this).classed("highlighted");})
             .classed("clicked", clicked);
      })
  };


  d3.selectAll(".highlighter")
    .on("mouseover", function(d) {
       var el = d3.select(this);
       if ( ! el.classed("clicked")) {
             setColor(el, d.content, false);
       }})
    .on("mouseout", function(d) {
       if ( ! d3.select(this).classed("clicked")) {
             setColor(d3.select(this), d.content, false);
       }})
    .on("click", function(d) {
       d3.selectAll(".highlighter")
         .filter( function (dd) {
           return dd.content && !dd.content.match(d.content);})
         .each(function(e) {
           d3.select(this).classed("clicked", false);
         })
       var el = d3.select(this);
       setColor(el, d.content, (!el.classed("clicked"))) ;
    });

  d3.select("#get-info")
    .on("click", function (d){
        setInitialGraphInfo();
    });

};

//module.exports.loadPathway = loadPathway;
//module.exports.loadExpressionTable = loadExpressionTable;
//# sourceURL=pathway.js
