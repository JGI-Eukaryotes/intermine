/* This lets the linter know d3 is defined globally and so not throw an error */
/*global d3:true*/

/* style attributes */

var pathAttrs = {
  stroke: "#666",
  strokeWidth: 2,
  fill: "transparent"
};

var markerPathAttrs = {
  stroke: "#666",
  strokeWidth: 0.95,
  fill: "transparent"
};

var fontAttrs = {
  family: "courier new, courier, monospace",
  color: "#333"
}

var loadPathway = function(container,json) {

  // make the SVG Container, we'll add height and width when everything's constructed
  var svgContainer = d3.select(container).append("svg")
                                         .attr("id","pathway_svg")
                                         .style("font-family", fontAttrs.family)
                                         .style("color", fontAttrs.color);

  console.log(JSON.stringify(json));

  d3.select("#save").on("click",function() {
    var svg = document.getElementById("pathway_svg");
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    var blob = new Blob(['<?xml version="1.0" standalone="no"?>' +
                         '<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">' +
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
  var drag = d3.drag()
             .on("start",function() { eventStart = d3.mouse(this);})
             .on("drag",function(d) {
               d.x = d3.event.x;
               d.y = d3.event.y;
               var mouse = d3.mouse(this);
               d3.select(this).attr("transform","translate("+d.x+","+d.y+")");
               d3.selectAll("path.moveable").each(function(dd) {
                 if (!d.id || (""+d.id).split(":").indexOf(""+dd.sourceId) === -1) return;
                 dd.startX += mouse[0]-eventStart[0]; dd.startY += mouse[1]-eventStart[1];
                 d3.select(this).attr("d",lineFunction(dd));
               });
               d3.selectAll("path.moveable").each(function(dd) {
                 if (!d.id || (""+d.id).split(":").indexOf(""+dd.targetId) === -1) return;
                 dd.endX += mouse[0]-eventStart[0]; dd.endY += mouse[1]-eventStart[1];
                 d3.select(this).attr("d",lineFunction(dd));
                 setSvgDimensions();
               });
             })

  // my arrowheads
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

  // and a all-encompassing group
  var masterGroup = svgContainer.selectAll("g").data([{"id":"master"}]).enter().append("g").attr("id","master");

  var div = d3.select(container)
              .append("div")
              .attr("id","tooltip-container")
              .style("opacity",0);

  // associate each node with its parents. And make a <g> for the parent.
  var containerData = [];
  var parents = {};
  //var linesToGroups = {};
  json.groups.forEach( function(d) {
    var thisId = d[0]+":"+d[1]+":"+d[2];
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

    // do we put this as a new group or inside another?
    // if it"s new, well use the node id for the  group id.
    // Otherwise the parent group id.
    var parentGroupId = (d.id in parents)?parents[d.id]:d.id;

    // has the group that owns this node been registered? not if it"s
    // an orphan. but we"ll check anyway.
    var base = masterGroup.selectAll("g")
              .filter(function(dd){return dd.id===parentGroupId;});
    // the parent is the masterGroup if it hasn"t been registered.
    var parentGroup = (base.size()===1)?base:masterGroup;
    //var parentGroupId;
    // should only have 1 item.
    parentGroup.data().forEach(function(dd) { parentGroupId = dd.id;});

    // register this group as a child of it"s parent.
    var thisGroup = parentGroup
                 .selectAll("g")
                 .data([{"id":d.id,"x":0,"y":0}],function (dd) { return dd.id===d.id; })
                 .enter()
                 .append("g")
                 .attr("id",function(dd){return dd.id;});

    // put a rectangle in the group, and with tooltip if it's a reaction

    thisGroup.selectAll("rect").data([{"id":d.id,"groupId":parentGroupId,"tooltip":d.tooltip}]).enter().append("rect")
             .attr("id", d.id)
             .attr("class","rects")
             .attr("x", 5*d.x)
             .attr("y", 15*d.y);


    // and the text holder, with a tooltip that will show up if it's in the data
    thisGroup.selectAll("text")
              .data([{"id":d.id,"label":d.label, "type":d.type, "xCoor":d.x, "yCoor":d.y, "labelHeight":d.height}])
              .enter().append("text")
              .attr("x", function(d){
                if (d.type === "reaction" || d.type === "link") {
                  return 5 * d.xCoor;
                }
                return 5*d.xCoor + 10;
              })
              .attr("y", function(d){
                if (d.labelHeight === 1) {
                  return 15*d.yCoor + 8;
                }
                return 15*d.yCoor;
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
                  div.html("<p id='tooltip-text'>" + d.tooltip + "</p>");
                  var w = document.getElementById("tooltip-text").offsetWidth;
                  var h = document.getElementById("tooltip-text").offsetHeight;
                  var pagex = d3.event.pageX;
                  var pagey = d3.event.pageY;
                  console.log("width of tooltip:", w, "its height:", h, "mouse pagex:", pagex, "pagey:", pagey);
                  div.style("left", pagex - (w / 2) + "px")
                     .style("top",d3.event.pageY - h - 80 + "px");
                  div.transition()
                     .duration(200)
                     .style("opacity", 1);
                }
              })
              .on("mouseout", function() {
                div.transition()
                   .duration(500)
                   .style("opacity", 0);
              });


  });



  // create a drag handler for everything inside the master group
  masterGroup.selectAll("g").filter(function(dd) { return !(dd.id in parents);}).call(drag);

  d3.selectAll("text.label").each(insertLineBreaks);

  function lineFunction(f) {
    if ((f.startX === f.endX) || (f.startY === f.endY)) {
      return "M"+f.startX+","+f.startY+"L"+f.endX+","+f.endY;
    } else if ((f.endX > f.startX) && (f.endY > f.startY)) {
      return "M"+f.startX+","+f.startY+
             "L"+f.endX+","+f.startY+
             "L"+f.endX+","+f.endY;
    } else {
      return "M"+f.startX+","+f.startY+
             "L"+f.startX+","+f.endY+
             "L"+f.endX+","+f.endY;
    }
  }

// function lineFunction(f) { return "M"+f.startX+","+f.startY+"L"+f.endX+","+f.endY;}

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
    if (source.size()===0 && input.size() === 1) {
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

    } else if (target.size()===0 && output.size() === 1) {
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
    } else {
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
  setSvgDimensions();

};

(function(){
  this.loadPathway = loadPathway;
})();

var setSvgDimensions = function () {
    //var bbox = element.node().getBBox();
    d3.select('g').each( function (d) {
               var bbox = d3.select(this).node().getBBox();
               var h = Math.ceil(bbox.height==0?1000:bbox.height) + 50;
               var w = Math.ceil(bbox.width==0?1000:bbox.width) + 50;
               var x = Math.floor(bbox.x) - 50;
               var y = Math.floor(bbox.y) - 50;
               d3.select('svg').attr("width", w)
                              .attr("height", h)
                              .attr("viewBox",x+ " "+ y + " " + w + " " + h); })
}


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
    handleShifts(el,(15*i)+"px",lines[i]);
  }
};

function handleShifts(ele,dy,line) {
  // this does rudimentary parsing of html tags <sub>, <sup> and <i>
  // take the line and break it into raised or lowered tokens.
  // The first function returns a list of the form
  // [ [<text> ,-1||0||1, 0||1 ] , [ <text>, -1||0||1 , 0||1 ] , ...]

  // For the second field, "0" means baseline. "1" means
  // superscript, "-1" means subscript if a subscript is
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
  return w.replace(/&harr;/i,"\u21D4")
          .replace(/&larr;/i,"\u21D0")
          .replace(/&rarr;/i,"\u21D2");
}

var loadExpressionTable = function(container,json) {

  console.log(JSON.stringify(json));
  // the highest level is an experiment group. We'll process each
  // of these and generate a separate table for each.
  var groupNames = [];
  json.data.forEach( function(d) {
    // prescan to find all sample names/EC. There may be holes in the table
    // so we do not want to just push onto a list
    var geneData = {};
    var sampleNames = {};
    var ec = {};
    groupNames.push(d.group);
    d.genes.forEach( function(e) {
      e.samples.forEach( function(f) { sampleNames[f.sample] = 1; })
      geneData[e.gene] = e.samples;
      ec[e.gene] = e.enzyme;
    });

    // now process the group.
    var table = d3.select(container)
                  .append('table')
                  .attr('class','collection-table')
                  .attr('id','table-'+d.group);;
    var cols =[{"label":"Gene"},{"label":"EC(s)"}];
    for( var s in sampleNames ) { cols.push( { "label":s}) };

    // create the caption and header for this table
    table.selectAll('caption')
         .data([{'caption':'Experiment Group: '+d.group}])
         .enter()
         .append('caption')
         .text(function(d) { return d.caption;})
         .on("mouseover",function(d) { showGroupToolTip(d); })
         .on("click",function(d) { hideGroupToolTip(d);
                                       toggleGroupMenu(d); })
         .on("mouseout",function(d) { hideGroupToolTip(d); });
    table.append('thead').append('tr')
         .selectAll('th')
         .data(cols)
         .enter()
         .append('th')
         .text(function(d) { return d.label;});

    var body = table.append('tbody');
    // and now the rows
    for( gene in geneData ) {
      var cols = [{"content":gene,"class":"highlighter"},{"content":ec[gene],"class":"highlighter"}];
      var lookup = {};
      // hash the results
      geneData[gene].forEach( function(e) { lookup[e.sample] = e.fpkm; });
      for( s in sampleNames ) {
        if (s in lookup) {
           cols.push({"content":lookup[s]});
        } else {
           cols.push({"content":"N/A"});
        }
      }
      body.append('tr').selectAll('td')
                       .data(cols)
                       .enter()
                       .append('td')
                       .text( function(d) { return d.content;})
                       .attr("class",function(d) { return d.class;})
                       .on("mouseover",function(d) { if( d.class === 'highlighter') {
                                                     d3.select(this).style('color','red');
                                                     d3.selectAll('text')
                                                     .each( function(dd) {
                                                         if(dd.label.match(d.content)) {
                                                           d3.select(this).style('stroke','red');
                                                         }
                                                      });}})
                       .on("mouseout",function(d) {  if( d.class === 'highlighter') {
                                                     d3.select(this).style('color','black');
                                                     d3.selectAll('text')
                                                       .each( function(dd) {
                                                        if(dd.label.match(d.content)) {
                                                           d3.select(this).style('stroke', null);
                                                        }
                                                     });}});
    }
  });

  // build the select-experiment menu
  if (groupNames.length > 1) {
    var tip = d3.select(container)
              .append('div')
              .html('Click to select an experiment group')
              .attr('id','group-tip')
              .style('background-color','green')
              .style('color','pink')
              .style('opacity',0)
              .style('z-index','1000')
              .style('position','absolute');
    var div = d3.select(container)
              .append("div")
              .style('background-color','red')
              .style('color','white')
              .attr("id","group-menu")
              .style('opacity','0')
              .style('z-index','1000')
              .style('position','absolute')
              .append('table');
    var firstElement = 1;
    groupNames.forEach( function(d) { div.append('tr')
                                         .selectAll('td')
                                         .data([{select:firstElement,name:d}])
                                         .enter()
                                         .append('td')
                                         .html( function(d) { return (d.select?'&#x2714;':'&nbsp;')+d.name;})
                                         .on('click',function(d) { processGroupMenuClick(d)});
              firstElement = 0;
              });
  }
  function showGroupToolTip(d) { d3.select("#group-tip")
                                   .transition()
                                   .delay(100)
                                   .style('opacity','1')
                                   .style("left",d3.event.pageX+'px')
                                   .style("top",d3.event.pageY+'px');}
  function hideGroupToolTip(d) { d3.select("#group-tip")
                                   .transition()
                                   .delay(10)
                                   .style('opacity','0') }
  function toggleGroupMenu(d)  { var opacity = d3.select("#group-menu").style('opacity');
                                 console.log("opacity "+opacity);
                                 opacity > .5?hideGroupMenu(d):showGroupMenu(d); }
  function showGroupMenu(d)    { d3.select("#group-menu")
                                   .style('opacity',1)
                                   .style("left",d3.event.pageX+'px')
                                   .style("top",d3.event.pageY+'px'); }
  function hideGroupMenu(d)    { d3.select("#group-menu")
                                   .style('opacity',0); }
  function processGroupMenuClick(d) { console.log('processing click on '+d.name);
                                      d3.select('#group-menu')
                                        .selectAll('td')
                                        .each( function(e) { e.select=0 })
                                        .each( function(e) { if (d.name === e.name) e.select=1; })
                                        .html( function(e) { return (e.select?'&#x2714;':'&nbsp;')+e.name; });
                                       d3.select(container)
                                         .selectAll('table')
                                         .each( function(e) { var thing = d3.select(this);
                                                              if (thing.attr('id') === 'table-'+d.name) {
                                                                thing.attr('opacity','1');
                                                              } else {
                                                                thing.attr('opacity','0');
                                                              }
                                                            });
                                       hideGroupMenu(d);}
}

