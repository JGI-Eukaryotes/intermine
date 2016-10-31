/* This lets the linter know d3 is defined globally and so not throw an error        */
/*global d3:true*/

/* style attributes */

var pathAttrs = {
  stroke: "#666",
  strokeWidth: 1.75,
  fill: "transparent"
};

var markerPathAttrs = {
  stroke: "#666",
  strokeWidth: 1,
  fill: "transparent"
};

var fontAttrs = {
  family: "courier new, courier, monospace",
  color: "#222"
};

var loadPathway = function(container,json) {

  // make the SVG Container, we'll add height and width when everything's constructed
  var svgContainer = d3.select(container).append("svg")
                                         .attr("id","pathway-svg")
                                         .style("font-family", fontAttrs.family)
                                         .style("color", fontAttrs.color);

  console.log(JSON.stringify(json, null, 1));

  // the 'save svg' handler.
  d3.select("#save").on("click",function() {
    var svg = document.getElementById("pathway-svg");
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    var blob = new Blob(["<?xml version=\"1.0\" standalone=\"no\"?>" +
                         "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">" +
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
               moveablePath = d3.selectAll("path.moveable");
               moveablePath.each(function (dd) {
                 dd.origStart = {x: dd.startX, y: dd.startY};
                 dd.origEnd = {x: dd.endX, y: dd.endY};
               });
             })
             .on("drag",function(d) {
               d.x = d3.event.x;
               d.y = d3.event.y;
               var mouse = d3.mouse(this);
               d3.select(this).attr("transform","translate("+d.x+","+d.y+")");
               moveablePath.each(function(dd) {
                 if (!d.id || (""+d.id).split(":").indexOf(""+dd.sourceId) === -1) return;
                 dd.startX += mouse[0]-eventStart[0]; dd.startY += mouse[1]-eventStart[1];
                 d3.select(this).attr("d",lineFunction(dd));
               });
               moveablePath.each(function(dd) {
                 if (!d.id || (""+d.id).split(":").indexOf(""+dd.targetId) === -1) return;
                 dd.endX += mouse[0]-eventStart[0]; dd.endY += mouse[1]-eventStart[1];
                 d3.select(this).attr("d",lineFunction(dd));
                 setElementDimensions(svgContainer);
               });
             });

  // arrowheads
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

  svgContainer.append("defs").append("marker")
    .attr("id", "n_degree_arrowend")
    .attr("refX", refX)
    .attr("refY", refY)
    .attr("markerWidth", refX)
    .attr("markerHeight", 2 * refY)
    .attr("orient", "8")
    .style("stroke", markerPathAttrs.stroke)
    .style("stroke-width", markerPathAttrs.strokeWidth)
    .style("fill", markerPathAttrs.fill)
    .append("path").attr("d", "M0," + 2 * refY + "L" + refX + "," + refY + "L0,0");

  // Make an all-encompassing group
  var masterGroup = svgContainer.selectAll("g").data([{"id":"master"}]).enter().append("g").attr("id","master");


  // and a div for the tooltip
  var tooltipDiv = d3.select("body")
                     .append("div")
                     .attr("id","tooltip-container")
                     .attr("align", "start")
                     .style("opacity",0)
                     .style("display", "none");



  // Make a <g> for the parents and associate each node with its parent.
  var containerData = [];
  var parents = {};
  json.groups.forEach( function(d) {
    var thisId = d[0] + ":" + d[1] + ":" + d[2];
    containerData.push({"id":thisId,"x":0,"y":0});
    d.forEach( function(e) { parents[e] = thisId; });
  });
  masterGroup.selectAll("g")
              .data(containerData)
              .enter()
              .append("g")
              .attr("id",function(d){return d.id; })
              .attr("transform","translate(0,0)");

  json.nodes.forEach( function(d) {

    // every node is already part of a group, even if the
    // group only has 1 node in it. The json was generated this way.
    var parentGroupId = parents[d.id];

    // the group that owns this node has already been registered
    var parentGroup = masterGroup.selectAll("g")
                                 .filter(function(dd){return dd.id === parentGroupId;});

    // each node has a rectangle and text. These will be their own group one level lower.
    var nodeGroup = parentGroup.selectAll("g")
                               .filter(function(dd) { return dd.id === d.id; })
                               .data([{id:d.id}])
                               .enter()
                               .append("g")
                               .attr("id",d.id);

    // put a rectangle in the group, and with tooltip if it's a reaction
    nodeGroup.selectAll("rect")
               .data([{"id":d.id,"groupId":parentGroupId,"tooltip":d.tooltip}])
               .enter()
               .append("rect")
               .attr("id", d.id)
               .attr("class","rects")
               .attr("x", 5*d.x)
               .attr("y", 15*d.y);


    // and the text holder, with a tooltip that will show up if it's in the data
    nodeGroup.selectAll("text")
               .data([{"id":d.id,"label":d.label, "type":d.type, "xCoor":d.x, "yCoor":d.y, "labelHeight":d.height,"tooltip":d.tooltip}])
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
                   return "label reaction";
                 }
                 return "label";
               })
              .on("mouseover",function() {
                if ( d.tooltip ){
                  tooltipDiv.html("<span id='tooltip-text'>" + d.tooltip + "</span><br/>" +
                  "<span id='dismiss-text'>Click to dismiss &#x2715;</span>");
                  var w = document.getElementById("tooltip-text").offsetWidth;
                  var h = document.getElementById("tooltip-text").offsetHeight;
                  var pagex = d3.event.pageX;
                  var pagey = d3.event.pageY;
                  tooltipDiv.style("left", pagex - (w / 2) + "px")
                     .style("top", pagey -h - 50 + "px");
                  tooltipDiv.transition()
                     .duration(200)
                     .style("opacity", 1)
                     .style("display", "block");
                  tooltipDiv.select("#dismiss-text").on("click", function() {
                    tooltipDiv.transition()
                    .duration(500)
                    .style("opacity",0); })
                    .style("display", "none");
                }
              })
              .on("click", function() {
                tooltipDiv.transition()
                   .duration(500)
                   .style("opacity", 0)
                   .style("display", "none");
              });
  });

  // create a drag handler for every group inside the master group that has an 'id'
  masterGroup.selectAll("g").filter(function(dd) { return (dd.id && !(dd.id in parents)); }).call(drag);

  d3.selectAll("text.label").each(insertLineBreaks);
  // was thinking of pushing the addTooltip event handler here...
  //d3.selectAll("text").each(function(e) { addTooltips(tooltipDiv,e)});


  // use the origStart and origEnd objects to limit the firing of these.

  function lineFunction(f) {
    if ((f.startX === f.endX) || (f.startY === f.endY)) {
      // where either start and end values on either axis is identical
      return "M" + f.startX + "," + f.startY +
             "L" + f.endX + "," + f.endY;
      // return a straight line between start and end points
    } else if ((f.endX > f.startX) && (f.endY > f.startY)) {
      // where the end values on both axes are greater than the start values
      return "M" + f.startX + "," + f.startY +
             "L" + f.endX + "," + f.startY +
             "L" + f.endX + "," + f.endY;
      // return a line that runs right on the x-axis to the endX point, turns +90deg and runs to the endY point
    } else {
      return "M" + f.startX + "," + f.startY +
             "L" + f.startX + "," + f.endY +
             "L" + f.endX + "," + f.endY;
      // return a line that runs on the y-axis to the endY point, turns +90 deg and runs to the endX point
    }
  }

  function inputArcFunction(f) {
    var str = "M"+f.startX+","+f.startY+
              " A"+Math.abs(f.startX-f.endX)+"," +
              Math.abs(f.startY-f.endY)+" 0 0 0 "+
              f.endX+","+f.endY;
    return str;
  }

  function outputArcFunction(f) {
    var str = "M"+f.startX+","+f.startY+
              " A"+Math.abs(f.startX-f.endX)+","+
              Math.abs(f.startY-f.endY)+" 0 0 0 "+
              f.endX+","+f.endY;
    return str;
  }

  // create links.
  json.links.forEach( function(d) {
    // source/target/input/output nodes.
    // at this point we don"t know what we have.
    var source = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.source;});
    var target = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.target;});
    var input = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.input;});
    var output = masterGroup.selectAll("rect").filter(function(dd){ return dd.id===d.output;});

    // the path is inside the top level group.
    // but we want to include the id"s of the groups that this line touches.
    var touchedGroups = [];
    source.data().forEach(function(dd) { touchedGroups.push(dd.groupId); });
    target.data().forEach(function(dd) { touchedGroups.push(dd.groupId); });
    input.data().forEach(function(dd) { touchedGroups.push(dd.groupId); });
    output.data().forEach(function(dd) { touchedGroups.push(dd.groupId); });
    if (source.size() === 0 && input.size() === 1) {
      var thisGroup = masterGroup.selectAll("g").filter(function(ddd) { return ddd.id===parents[d.input]; });
      thisGroup.selectAll("path").data([{"id":d.input+":"+d.target,
                                         "sourceId":d.input,
                                         "targetId":d.target,
                                         "startX":parseInt(input.attr("x"))+5,
                                         "startY":parseInt(input.attr("y"))+5,
                                         "endX":parseInt(target.attr("x"))+5,
                                         "endY":parseInt(target.attr("y"))+5,
                                         "touchedGroups":touchedGroups }],
                                         function(dd){return dd.id===d.input+":"+d.target;})
                                         .enter()
                                         .append("path")
                                         .attr("d",inputArcFunction)
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill);

    } else if (target.size() === 0 && output.size() === 1) {
      thisGroup = masterGroup.selectAll("g").filter(function(ddd) { return ddd.id===parents[d.output]; });
      thisGroup.selectAll("path").data([{"id":d.source+":"+d.output,
                                         "sourceId":d.source,
                                         "targetId":d.output,
                                         "startX":parseInt(source.attr("x"))+5,
                                         "startY":parseInt(source.attr("y"))+5,
                                         "endX":parseInt(output.attr("x"))+5,
                                         "endY":parseInt(output.attr("y"))+5,
                                         "touchedGroups":touchedGroups }],
                                         function(dd){return dd.id===d.source+":"+d.output;})
                                         .enter()
                                         .append("path")
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill)
                                         .attr("marker-end","url(#n_degree_arrowend)")
                                         .attr("d",outputArcFunction);
    } else if (source.size() === 1 && target.size() === 1) {
      // default: a straight line
      masterGroup.selectAll("path").data([{"id":d.source+":"+d.target,
                                         "sourceId":d.source,
                                         "targetId":d.target,
                                         "startX":parseInt(source.attr("x"))+5,
                                         "startY":parseInt(source.attr("y"))+5,
                                         "endX":parseInt(target.attr("x"))+5,
                                         "endY":parseInt(target.attr("y"))+5,
                                         "touchedGroups":touchedGroups}],
                                         function(dd){return dd.id===d.source+":"+d.target;}).enter().append("path")
                                         .attr("class","moveable")
                                         .attr("marker-end", function(d) {
                                           var type = json.nodes.filter(function(x){return x.id === d.targetId;})[0].type;
                                           if (type === "link") return "url(#arrowend)";
                                           return "";
                                         })
                                         .style("stroke", pathAttrs.stroke)
                                         .style("stroke-width", pathAttrs.strokeWidth)
                                         .style("fill", pathAttrs.fill)
                                         .attr("d",lineFunction);
    }

  });


  // discover how large the svg is and explicitly set it on the root <svg> node
  setElementDimensions(svgContainer);

};

var setElementDimensions = function (element) {
  var bbox = element.node().getBBox();
  var h = Math.ceil(bbox.height === 0 ? 1000 : bbox.height) + 50;
  var w = Math.ceil(bbox.width === 0 ? 1000 : bbox.width) + 50;
  var x = Math.floor(bbox.x) - 50;
  var y = Math.floor(bbox.y) - 50;
  element.attr("width", w)
         .attr("height", h)
         .attr("viewBox",x+ " "+ y + " " + w + " " + h);
};


var insertLineBreaks = function (d) {
  var el = d3.select(this);
  el.text("");

  // split on <br>"s
  var lines = d.label.split("<br>");
  for (var i = 0; i < lines.length; i++) {
    // first replace html char codes...
    lines[i] = replaceHTMLchar(lines[i]);

    // now deal with <sub>, <sup> and <i> tags.
    // I hope they're not overlapping
    handleShifts(el,(15*i)+"px",lines[i],d);
  }
};

function handleShifts(ele, dy, line) {
  // this does rudimentary parsing of html tags <sub>, <sup> and <i>
  // take the line and break it into raised or lowered tokens.
  // The first function returns a list of the form
  // [ [<text> ,-1||0||1, 0||1 ] , [ <text>, -1||0||1 , 0||1 ] , ...]

  // For the second field, "0" means baseline. "1" means
  // superscript, "-1" means subscript. If a subscript is
  // nested in a superscript or vice versa, you get back to the
  // baseline. This ain't TeX.
  // For the third field field, 0 means normal font, 1 means italic

  var token_list = makeTokens(line);
  // now append tokens
  var last_shift= 0;
  var last_style = 0;
  var parentTspan = ele.append("tspan").attr("x",ele.attr("x")).attr("y",ele.attr("y")).attr("dy",dy);
  for ( var i = 0; i < token_list.length; i++) {
    var tspan = parentTspan.append("tspan").text(token_list[i][0]);
    // semantic tooltip?

    if(i===0) {
      tspan.attr("font-size","12px")
           .attr("font-style","normal");
    } else {
      tspan.attr("dx","0");
      var this_shift = token_list[i][1];
      if (this_shift === 0) {
        tspan.attr("font-size","12px");
      } else {
        tspan.attr("font-size","10px");
      }

    // baseline-shift appears not to work. Here is
    // easier code in case this ever works...
    //switch( token_list[i][1]) {
    //  case 1:
    //    tspan.style("baseline-shift","super");
    //    break;
    //  case -1:
    //    tspan.style("baseline-shift","sub");
    //    break;
    //}

      // and harder-to-follow code until baseline-shift
      if ( (this_shift - last_shift) > 0) {
        // going up...
        tspan.attr("dy","-6px");
      } else if ( (this_shift - last_shift) < 0) {
        // going down...
        tspan.attr("dy","6px");
      } else {
        tspan.attr("dy",0);
      }
      last_shift = this_shift;

      if ( token_list[i][2] !== last_style) {
        last_style = token_list[i][2];
        switch (token_list[i][2]) {
        case 0:
          tspan.attr("font-style","normal");
          break;
        case 1:
          tspan.attr("font-style","italic");
          break;
        }
      }
    }
  }
}

function makeTokens(line) {
  var retList = [];
  var subs = line.split(/<\/?sub>/i);
  for( var j = 0; j<subs.length; j++) {
    var supers =  subs[j].split(/<\/?sup>/i);
    for ( var l = 0; l < supers.length; l++) {
      var this_shift = ((j%2===0) && (l%2===0))?0:(j%2===1)?-1:1;
      // now split on <i> tokens
      var italics = supers[l].split(/<\/?i>/i);
      for (var k = 0; k < italics.length; k++ ) {
        retList.push([italics[k],this_shift,k%2]);
      }
    }
  }
  return retList;
}

// TODO: replace other characters. like the greeks.
function replaceHTMLchar(w) {
  return w.replace(/&harr;/i, "\u21D4")
          .replace(/&larr;/i, "\u21D0")
          .replace(/&rarr;/i, "\u21D2")
          .replace(/&alpha;/i, "\u03B1")
          .replace(/&beta;/i, "\u03B2");
}

var toLowerCaseAndSpacesToDashes = function (string) {
  return string.toLowerCase().replace(/ /g, "-");
};

var showTable = function(tableId) {
  d3.selectAll("table")
    .style("display", "none");

  d3.select("#" + tableId)
    .style("display", "block");
};

var createExperimentGroupSelect = function (container, json) {

  var select = d3.select(container)
                 .append("label")
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
  return d3.scaleLinear()
           .domain(minMaxVal)
           .range(minMaxColors);
};

var loadExpressionTable = function(container,json) {

  if (json.data.length > 1) createExperimentGroupSelect(container, json);

  //console.log(JSON.stringify(json, null, 2));
  // the highest level is an experiment group. We'll process each
  // of these and generate a separate table for each.


  json.data.forEach( function(d) {

    // prescan to find all sample names/EC. There may be holes in the table
    // so we do not want to just push onto a list

    var geneData = {};
    var sampleNames = {};
    var ec = {};
    d.idName = d.group.toLowerCase().replace(/ /g, "-");
    //groupNames.push(d.group);
    d.genes.forEach( function(e) {
      e.samples.forEach( function(f) { sampleNames[f.sample] = 1; });
      geneData[e.gene] = e.samples;
      ec[e.gene] = e.enzyme;
    });

    // find minimum and maximum fpkm values for the experiment for purposes of setting ranges
    var minMaxFkpm = getMinMaxFpkm(d);
    //console.log(minMaxFkpm);
    var colorScale = setColorScale(minMaxFkpm, ["#dfe", "#4b7"]);

    // now process the group.
    var table = d3.select(container)
                  .append("table")
                  .attr("class","collection-table")
                  .attr("id","table-"+d.idName);
    var cols =[{"label":"Gene", "class": "gene"},{"label":"EC(s)", "class": "ec"}];
    for( var s in sampleNames ) { cols.push( { "label":s}); }

    // create the caption and header for this table if only one table
    if (json.data.length === 1) {
      table.selectAll("caption")
           .data([{"caption":"Experiment Group: "+d.group,"id":"caption-"+d.idName}])
           .enter()
           .append("caption")
           .text(function(d) { return d.caption;});
    }

    table.append("thead").append("tr")
         .selectAll("th")
         .data(cols)
         .enter()
         .append("th")
         .attr("class", function(d) { return d.class; })
         .text(function(d) { return d.label; });


    var body = table.append("tbody");
    // and now the rows
    for( var gene in geneData ) {
      cols = [{"content":gene,"class":"highlighter gene"},{"content":ec[gene],"class":"highlighter ec"}];
      var lookup = {};
      // hash the results
      geneData[gene].forEach( function(e) { lookup[e.sample] = e.fpkm; });
      for( s in sampleNames ) {
        if (s in lookup) {
          cols.push({"content":lookup[s], "class": "fpkm result"});
        } else {
          cols.push({"content":"N/A", "class": "fpkm not-available"});
        }
      }
      var menu = [
        {title: "gene link out to Pz gene page"},
        {title: "gene link out to JBrowse"}
      ];
      body.append("tr").selectAll("td")
                       .data(cols)
                       .enter()
                       .append("td")
                       .html( function(d) {
                         if (Array.isArray(d.content)) {
                           return d.content.join("<br />");
                         }
                        return d.content;})
                       .attr("class",function(d) { return d.class;})
                       .style("background-color", function(d){
                         if (/result/.test(d.class)) {
                           return colorScale(d.content);
                         }
                       });

      body.selectAll("td.highlighter")
                      .on("mouseover", function(d) {
                        d3.select(this).style("color", "red");
                        d3.selectAll("text")
                          .each( function(dd){
                            if (dd.label.match(d.content)) {
                              d3.select(this).style("stroke", "red");
                            }
                          });
                      })
                      .on("mouseout", function(d) {
                        d3.select(this).style("color", "black");
                        d3.selectAll("text")
                          .each( function(dd){
                            if (dd.label.match(d.content)) {
                              d3.select(this).style("stroke", null);
                            }
                          });
                      })
                      .on("contextmenu", d3.contextMenu(menu));

    }


  });

  // show the initial table and hide the rest
  if (json.data.length > 1) {
    var initialTable = d3.select("#experiments-select").node().value;
    showTable(initialTable);
  }
};

module.exports.loadPathway = loadPathway;
module.exports.loadExpressionTable = loadExpressionTable;
