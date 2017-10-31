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

var baseFontSize = 16;

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

var supAttrs = {
  "vertical-align": "super",
  "font-size": "65%"
}
var subAttrs = {
  "vertical-align": "sub",
  "font-size": "65%"
}
var italicAttrs = {
  "font-style": "italic"
}

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
 
  // what is the quadrant (1-4) given an angle in degrees?
var quadrantOf = function (d) {
    // map into the range of [0,360)
    var ranged = (parseInt(d)%360 + 360)%360;
    return (parseInt((parseInt(ranged)+45)/90))%4+1;
  }

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
  var h = Math.ceil(bbox.height === 0 ? 1000 : bbox.height) + 150;
  var w = Math.ceil(bbox.width === 0 ? 1000 : bbox.width) + 150;
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
  if (!d.genes) return;
  d.genes.forEach( function(e) {
    var gN = e.name;
    geneLinks[gN] = [];
    if (e.links) {
      e.links.forEach( function(f) {
        geneLinks[gN].push({"label":f.label,"url":f.url});
      } )
    }
  });
}

var labelReactions = function (d) {
  var el = d3.select(this);

  // optionally label with reaction names
  // this is really just for development.
  if (options.labelRxnNames) {
    el.text(d.label);
    return;
  }

  // do not touch the label if there are no ECs and no genes.
  if ( (!d.genes || d.genes.length == 0) && (!d.ecs || d.ecs.length == 0) ) return;

  var lineCtr = 0;
  var ecData = [];
  d.ecs.forEach( function(dd) {
    var menu = [{title: 'ExPASy Information',
               action: function() { doLinkout("http://enzyme.expasy.org/EC/"+dd)} }];
    ecData.push({ content: dd,
                  class: "highlighter diagram ec",
                  baseColor: ecLabelAttrs.diagramColor,
                  highlightedColor: ecLabelAttrs.highlightedColor,
                  menu: menu,
                  line: lineCtr }
                );
    lineCtr++;
  });

  var pData = [];
  d.genes.forEach( function(dd) {
    var menu = [];
    if( dd.links) {
       dd.links.forEach( function(ee) {
         if (/PhytoWeb/.test(ee.label)) ee.label = "Phytozome Gene Report";
          menu.push({title: ee.label,
                   action: function() {doLinkout(ee.url)}});
         });
      pData.push({ content: dd.name,
                   class: "highlighter diagram gene",
                   baseColor: geneLabelAttrs.diagramColor,
             highlightedColor: geneLabelAttrs.highlightedColor,
                   menu: menu,
                   line: lineCtr }
                 );
      lineCtr++;
    }
  });

  el.text("")
    .attr("font-size", fontAttrs.size);

  // for more that 4 lines, we're going to truncate a that
  lineCtr = (lineCtr>4)?4:lineCtr;
  // placement.
  placementOptions = {};
  switch( quadrantOf(d.orient) ) {
    case 1: placementOptions = { "x":d.x, "y":d.y-baseFontSize*(lineCtr-1), "align":"left" }; break;
    case 2: placementOptions = { "x":d.x, "y":d.y, "align":"left" }; break;
    case 3: placementOptions = { "x":d.x, "y":d.y-baseFontSize*(lineCtr-1), "align":"left" }; break;
    case 4: placementOptions = { "x":d.x, "y":d.y, "align":"right" }; break;
  }

  var geneTspan = el.selectAll("tspan").data(ecData.concat(pData)).enter().append("tspan")
                       .attr("x",placementOptions.x)
                       .attr("y",function(d) { return placementOptions.y+baseFontSize*d.line})
                       .attr("class", function (dd) { return dd.class })
                       .text(function(dd) { return dd.content })
                       .style("fill", function(dd) { return dd.baseColor });

  geneTspan.each( function (d) {
           var thisElement = d3.select(this);
           thisElement.on("contextmenu", d3.contextMenu(d.menu)) });


  // add elements and class names to hide / show excess ecs & genes in reaction label if necessary.
  if (el.selectAll("tspan").size() > 4) {
          el.classed("has-ellipse", true);

       el.append("tspan")
           .attr("x",d.labelX)
           .attr("dy", "1em")
           .attr("text-decoration", "underline")
           .classed("reaction-control", true)
           .text("less...")
           .style("fill", fontAttrs.color)
           .style("font-style", "italic");

          el.selectAll("tspan:nth-child(n+4)")
           .classed("no-display hidable", true);

       el.insert("tspan", ":nth-child(4)")
           .attr("x",placementOptions.x)
           .attr("dy", "1em")
           .attr("text-decoration", "underline")
           .classed("reaction-control hidable", true)
           .text("more...")
           .style("fill", fontAttrs.color)
           .style("font-style", "italic");
  }
}

var styledWith = function (p1, styleAttrs) {
  var stylesString = ""
  Object.keys(styleAttrs).forEach( function (key) {
    stylesString += key + ":" + styleAttrs[key] + ";"
  })
  return '<' + p1 + ' style="' + stylesString + '">'
}

var renderLabels = function (d) {
  var el = d3.select(this);
  if (d.label) {
    //console.log('for label '+d.label+' x,y,text-anchor are '+el.attr('x')+', '+el.attr('y')+', '+el.attr('text-anchor')+'.');
    var parent = this.parentElement;
    var label = d.label.split("<br>").join("<br />")
    //console.log('label', label)
    // var lines = d.label.split('<br>');
    // var lineCtr = 0;
    // lines.forEach( function(e) {
      var lineSize = measureText(label, baseFontSize);
      // placement.
      placementOptions = {};
      switch( quadrantOf(d.orient) ) {
        case 1: placementOptions = { "x":d.x - lineSize.width/2,
                                     "y":d.y};
                break;
        case 2: placementOptions = { "x":"0", "dy":"0", "align":"left" }; break;
        case 3: placementOptions = { "x":d.x - lineSize.width/2,
                                     "y":d.y};
                break;
        case 4: placementOptions = { "x":"0", "dy":"0", "align":"right" }; break;
      }
      var styledD = label.replace(/<(i)>|<(em)>/ig, function(match, p1) { return styledWith(p1, italicAttrs) })
                         .replace(/<(sub)>/ig, function(match, p1) { return styledWith(p1, subAttrs) })
                         .replace(/<(sup)>/ig, function(match, p1) { return styledWith(p1, supAttrs) })
                         .replace(/'/g,'&#39;');
      //console.log('html for label:', styledD);
      d3.select(parent).append('g')
        .append("foreignObject")
         .attr("width",lineSize.width)
         .attr("x",placementOptions.x)
         .attr("y", function () { if (d.type === "link") {
                                    return placementOptions.y - 8;
                                  } else {
                                    return placementOptions.y;
                                  }})
         //.attr("height",baseFontSize)
         .attr("height",lineSize.height)
         .attr("class","foreign-object")
         .append("xhtml:body")
         .attr("xmlns", "http://www.w3.org/1999/xhtml")
         .style("padding", 0)
         .style("margin", 0)
         .style("background-color","rgba(255,255,255,0)")
         .append("xhtml:div")
         .style("font-size", fontAttrs.size)
         .style("color", fontAttrs.color)
         .style("font-family", fontAttrs.family)
        // these 3 lines weare an experiment in short form of names with a long form on mouseover.
        // replace with .html(styledD) to revert.
        // .attr("onmouseover","innerHTML='"+styledD+"'")
        // .attr("onmouseout","innerHTML='"+styledD.substr(0,30)+(styledD.length>30?"...'":"'"))
        // .html(styledD.substr(0,30)+(styledD.length>30?"...":""));
        // comment out this line if the previous 3 are uncommented
         .html(styledD);
      // lineCtr++;
      // });
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
    // it seems that we need to skosh this up a bit.
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

var options = {};

var loadPathwayDiagram = function(container,json,optArgs) {

  if(optArgs != null) options = optArgs;

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
                         svg.outerHTML.replace(/<br>/ig, '<br />')],
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
    var thisId = 'node-group' + d[0] + "-" + d[1] + "-" + d[2];
    containerData.push({"id":thisId});
    d.forEach( function(e) { parents[e] = thisId; });
  });

  // and insert them. This is initially unpositioned.
  // we'll position it as we populate it with the elements
  var nodeGroups = masterGroup.selectAll("g")
              .data(containerData,function(e) { return e.id })
              .enter()
              .append("g")
              .attr("orient",0)
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

    // if the type of the node is "reaction", "link", "source" or "drain",
    // the location of this node is the anchor point for the group.
    // This is where the line goes to.
    if (d.type === 'link' || d.type == 'reaction' || d.type === 'source' || d.type == 'drain') {
      // we'll keep track of both the original (x,y) and current (x,y)
      parentGroup.each(function(e) { e.x = d.x;
                                     e.y = d.y ;
                                     e.baseX = e.x;
                                     e.baseY = e.y;
                                     e.orient=d.orient} );
    }

    // each node has a rectangle and text. These will be their own group one level lower.
    var nodeGroup = parentGroup.selectAll("g")
                               .filter(function(dd) { return dd.id === d.id; })
                               .data([{"id":d.id,
                                        "x":d.x,
                                        "y":d.y,
                                        "labelX":d.labelX,
                                        "labelY":d.labelY,
                                        "orient":d.orient,
                                        "type":d.type}],function(e) { return e.id})
                               .enter()
                               .append("g")
                               .attr("class","anchored")
                               .attr("id", d.id);

    // put a rectangle in the group, and with tooltip if it's a reaction
    nodeGroup.selectAll("rect")
               .data([{"id":d.id,"groupId":parentGroupId,"tooltip":d.tooltip,"orient":d.orient}],
                      function(e) { return e.id })
               .enter()
               .append("rect")
               .attr("id", "rect-" + d.id)
               .attr("class","rect no-display")
               .attr("height",10)
               .attr("width",10)
               .attr("orient",d.orient)
               .attr("x", d.x-5)
               .attr("y", d.y-5);

    // and the text holder, with a tooltip that will show up if it's in the data
    nodeGroup.selectAll("text")
               .data([{"id":d.id,"label":d.label,
                        "type":d.type, "x":d.labelX,
                        "y":d.labelY, "labelHeight":d.height,"orient":d.orient,
                        "tooltip":d.tooltip, "genes":d.genes, "ecs":d.ecs}])
               .enter().append("text")
               .attr("x", function(d){ return textPlacement(d).x;})
               .attr("y", function(d){ return textPlacement(d).y;})
               .attr("text-anchor", function(d){ return textPlacement(d).anchor;})
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
    var sweep;
    switch(quadrantOf(f.orient)) {
      case 1: sweep = 1; break;
      case 2: sweep = 1; break;
      case 3: sweep = 0; break;
      case 4: sweep = 0; break;
    }
    //console.log('input quad and sweep are '+quadrantOf(f.orient)+" "+sweep);

    var str = "M"+f.x1+","+f.y1+
              " A"+Math.abs(f.x1-f.x2)+"," +
              Math.abs(f.y1-f.y2)+" 0 0 "+sweep+" "+
              f.x2+","+f.y2;
    return str;
  }

  function outputArcFunction(f) {
    // how to draw an arc for an output compound
    var sweep;
    switch(quadrantOf(f.orient)) {
      case 1: sweep = 1; break;
      case 2: sweep = 1; break;
      case 3: sweep = 0; break;
      case 4: sweep = 0; break;
    }
    //console.log('output quad and sweep are '+quadrantOf(f.orient)+" "+sweep);
    var str = "M"+f.x1+","+f.y1+
              " A"+Math.abs(f.x1-f.x2)+","+
              Math.abs(f.y1-f.y2)+" 0 0 "+sweep+" "+
              f.x2+","+f.y2;
    return str;
  }


  // create links.
  var linkData = [];
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
                                         "y2":parseInt(target.attr("y"))+5,
                                         "orient":target.attr("orient")}
                                         ],
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
                                         "y2":parseInt(target.attr("y"))+5,
                                         "orient":source.attr("orient")}
                                        ],
                                         function(dd){return dd.id===d.source+":"+d.target;})
                                         .enter()
                                         .append("path")
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill)
                                         .attr("marker-end","url(#arrowend)")
                                         .attr("d",outputArcFunction);
    } else if (type === 'link') {
      // a movable line
      //
      var sourceGroup = masterGroup.selectAll("g")
                                 .filter(function(ddd) { return ddd.id===parents[d.source]; });
      var targetGroup = masterGroup.selectAll("g")
                                 .filter(function(ddd) { return ddd.id===parents[d.target]; });
      sourceGroup.each( function(a) { targetGroup.each ( function(b) {
                       linkData.push( {"id":parents[d.source]+'->'+parents[d.target],
                                        "source":a,
                                        "target":b,
                                        "orient": d.orient} );
                                        });
                      });
    }

  });

  var link = masterGroup.append("g").selectAll("line")
                                    .data(linkData)
                                    .enter()
                                    .append("line")
                                    .attr("marker-end","url(#arrowend)")
                                    .attr('stroke-width',pathAttrs.strokeWidth)
                                    .attr('stroke',pathAttrs.stroke)
                                    .attr('class','moveable');

  // discover how large the svg is and explicitly set it on the root <svg> node
  setSvgElementDimensions(svgContainer);
  refreshPositions();

  // install the zoom handler on the zoom buttons
  d3.select("#zoom-in").on("click", function() {
       zoomDiagram.scaleBy(svgContainer, 1.25);
      });
  d3.select("#zoom-out").on("click", function() {
       zoomDiagram.scaleBy(svgContainer, 0.8);
      });
  // install a drag handler for every group inside the master group that has an 'id'
  masterGroup.selectAll("g.moveable").filter(function(dd) { return (dd.id && !(dd.id in parents)); }).call(drag);

  function refreshPositions() {
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
      if( geneLinks[geneName]) {
        geneLinks[geneName].forEach( function(f) {
          if (/PhytoWeb/.test(f.label)) f.label = "Phytozome Gene Report";
          menu.push({title: f.label,
                   action: function() { doLinkout(f.url)}});
        });
      }
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

var textPlacement = function(d) {
  // a place holder for the return.
  // maybe not the best values; but something.
  var result = { x: d.x, y: d.y, anchor: 'middle' };
  // the orientatation angle in the range of [0,360)
  var rangedOrient = (d.orient%360 + 360)%360;

  if (rangedOrient <= 45 || rangedOrient > 315) {
    // flow is left to right
    switch(d.type) {
      case "source":
        result.anchor = "end";
        break;
      case "drain":
        result.anchor = "beginning";
        break;
      default:
        result.anchor = "middle";
    }
    return result;
  } else if (rangedOrient <= 135) {
    switch(d.type) {
      case "link":
        result.anchor = "beginning";
        break;
      default:
        result.anchor = "middle";
    }
    return result;
  } else if (rangedOrient <= 225) {
    // flow is right to left
    return result;
  } else if (rangedOrient <= 316) {
    // flow is bottom to top
    return result;
  }
}


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
