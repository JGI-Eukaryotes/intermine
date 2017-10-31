(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports, require('d3-dispatch'), require('d3-collection'), require('d3-timer')) :
	typeof define === 'function' && define.amd ? define(['exports', 'd3-dispatch', 'd3-collection', 'd3-timer'], factory) :
	(factory((global.d3 = global.d3 || {}),global.d3Dispatch,global.d3Collection,global.d3Timer));
}(this, (function (exports,d3Dispatch,d3Collection,d3Timer) { 'use strict';

var constant = function(x) {
  return function() {
    return x;
  };
};

var prefix = "$";

function Map() {}

Map.prototype = map$1.prototype = {
  constructor: Map,
  has: function(key) {
    return (prefix + key) in this;
  },
  get: function(key) {
    return this[prefix + key];
  },
  set: function(key, value) {
    this[prefix + key] = value;
    return this;
  },
  remove: function(key) {
    var property = prefix + key;
    return property in this && delete this[property];
  },
  clear: function() {
    for (var property in this) if (property[0] === prefix) delete this[property];
  },
  keys: function() {
    var keys = [];
    for (var property in this) if (property[0] === prefix) keys.push(property.slice(1));
    return keys;
  },
  values: function() {
    var values = [];
    for (var property in this) if (property[0] === prefix) values.push(this[property]);
    return values;
  },
  entries: function() {
    var entries = [];
    for (var property in this) if (property[0] === prefix) entries.push({key: property.slice(1), value: this[property]});
    return entries;
  },
  size: function() {
    var size = 0;
    for (var property in this) if (property[0] === prefix) ++size;
    return size;
  },
  empty: function() {
    for (var property in this) if (property[0] === prefix) return false;
    return true;
  },
  each: function(f) {
    for (var property in this) if (property[0] === prefix) f(this[property], property.slice(1), this);
  }
};

function map$1(object, f) {
  var map$$1 = new Map;

  // Copy constructor.
  if (object instanceof Map) object.each(function(value, key) { map$$1.set(key, value); });

  // Index array by numeric index or specified key function.
  else if (Array.isArray(object)) {
    var i = -1,
        n = object.length,
        o;

    if (f == null) while (++i < n) map$$1.set(i, object[i]);
    else while (++i < n) map$$1.set(f(o = object[i], i, object), o);
  }

  // Convert object to map.
  else if (object) for (var key in object) map$$1.set(key, object[key]);

  return map$$1;
}

function index(d) {
  return d.index;
}

function find(nodeById, nodeId) {
  var node = nodeById.get(nodeId);
  if (!node) throw new Error("missing: " + nodeId);
  return node;
}

var link = function(links) {
  var id = index,
      strength = defaultStrength,
      strengths,
      distance = constant(30),
      distances,
      symmetry = 6,
      nodes,
      count,
      bias,
      iterations = 1;

  if (links == null) links = [];

  function defaultStrength(link) {
    return 1 / Math.min(count[link.source.index], count[link.target.index]);
  }

  function force(alpha) {

    for (var k = 0, n = links.length; k < iterations; ++k) {
      for (var i = 0, link, source, target, x, y, l, b; i < n; ++i) {
        link = links[i], source = link.source, target = link.target;
        x = target.x - source.x || 1.e-4;
        y = target.y - source.y || 1.5e-5;
        l = Math.sqrt(x * x + y * y);
        var dl = (l - distances[i]) / distances[i] * alpha * strengths[i];
        var dx = x*dl;
        var dy = y*dl;
        target.vx -= dx * (b = bias[i]);
        target.vy -= dy * b;
        source.vx += dx * (b = 1 - b);
        source.vy += dy * b;

        var angle = Math.atan2(y,x);
        var trigFactor = Math.sin(symmetry*angle)*Math.cos(symmetry*angle);
        source.vx -= trigFactor*y/l * strengths[i] * alpha;
        source.vy += trigFactor*x/l * strengths[i] * alpha;
        target.vx += trigFactor*y/l * strengths[i] * alpha;
        target.vy -= trigFactor*x/l * strengths[i] * alpha;
      }
    }
  }

  function initialize() {
    if (!nodes) return;

    var i,
        n = nodes.length,
        m = links.length,
        nodeById = map$1(nodes, id),
        link;

    for (i = 0, count = new Array(n); i < m; ++i) {
      link = links[i], link.index = i;
      if (typeof link.source !== "object") link.source = find(nodeById, link.source);
      if (typeof link.target !== "object") link.target = find(nodeById, link.target);
      count[link.source.index] = (count[link.source.index] || 0) + 1;
      count[link.target.index] = (count[link.target.index] || 0) + 1;
    }

    for (i = 0, bias = new Array(m); i < m; ++i) {
      link = links[i], bias[i] = count[link.source.index] / (count[link.source.index] + count[link.target.index]);
    }

    strengths = new Array(m), initializeStrength();
    distances = new Array(m), initializeDistance();
  }

  function initializeStrength() {
    if (!nodes) return;

    for (var i = 0, n = links.length; i < n; ++i) {
      strengths[i] = +strength(links[i], i, links);
    }
  }

  function initializeDistance() {
    if (!nodes) return;

    for (var i = 0, n = links.length; i < n; ++i) {
      distances[i] = +distance(links[i], i, links);
    }
  }

  force.initialize = function(_) {
    nodes = _;
    initialize();
  };

  force.links = function(_) {
    return arguments.length ? (links = _, initialize(), force) : links;
  };

  force.id = function(_) {
    return arguments.length ? (id = _, force) : id;
  };

  force.iterations = function(_) {
    return arguments.length ? (iterations = +_, force) : iterations;
  };

  force.strength = function(_) {
    return arguments.length ? (strength = typeof _ === "function" ? _ : constant(+_), initializeStrength(), force) : strength;
  };

  force.symmetry = function(_) {
    return arguments.length ? (symmetry = +_, force) : symmetry;
  };

  force.distance = function(_) {
    return arguments.length ? (distance = typeof _ === "function" ? _ : constant(+_), initializeDistance(), force) : distance;
  };

  return force;
};

var tree_add = function(d) {
  var x = +this._x.call(null, d),
      y = +this._y.call(null, d);
  return add(this.cover(x, y), x, y, d);
};

function add(tree, x, y, d) {
  if (isNaN(x) || isNaN(y)) return tree; // ignore invalid points

  var parent,
      node = tree._root,
      leaf = {data: d},
      x0 = tree._x0,
      y0 = tree._y0,
      x1 = tree._x1,
      y1 = tree._y1,
      xm,
      ym,
      xp,
      yp,
      right,
      bottom,
      i,
      j;

  // If the tree is empty, initialize the root as a leaf.
  if (!node) return tree._root = leaf, tree;

  // Find the existing leaf for the new point, or add it.
  while (node.length) {
    if (right = x >= (xm = (x0 + x1) / 2)) x0 = xm; else x1 = xm;
    if (bottom = y >= (ym = (y0 + y1) / 2)) y0 = ym; else y1 = ym;
    if (parent = node, !(node = node[i = bottom << 1 | right])) return parent[i] = leaf, tree;
  }

  // Is the new point is exactly coincident with the existing point?
  xp = +tree._x.call(null, node.data);
  yp = +tree._y.call(null, node.data);
  if (x === xp && y === yp) return leaf.next = node, parent ? parent[i] = leaf : tree._root = leaf, tree;

  // Otherwise, split the leaf node until the old and new point are separated.
  do {
    parent = parent ? parent[i] = new Array(4) : tree._root = new Array(4);
    if (right = x >= (xm = (x0 + x1) / 2)) x0 = xm; else x1 = xm;
    if (bottom = y >= (ym = (y0 + y1) / 2)) y0 = ym; else y1 = ym;
  } while ((i = bottom << 1 | right) === (j = (yp >= ym) << 1 | (xp >= xm)));
  return parent[j] = node, parent[i] = leaf, tree;
}

function addAll(data) {
  var d, i, n = data.length,
      x,
      y,
      xz = new Array(n),
      yz = new Array(n),
      x0 = Infinity,
      y0 = Infinity,
      x1 = -Infinity,
      y1 = -Infinity;

  // Compute the points and their extent.
  for (i = 0; i < n; ++i) {
    if (isNaN(x = +this._x.call(null, d = data[i])) || isNaN(y = +this._y.call(null, d))) continue;
    xz[i] = x;
    yz[i] = y;
    if (x < x0) x0 = x;
    if (x > x1) x1 = x;
    if (y < y0) y0 = y;
    if (y > y1) y1 = y;
  }

  // If there were no (valid) points, inherit the existing extent.
  if (x1 < x0) x0 = this._x0, x1 = this._x1;
  if (y1 < y0) y0 = this._y0, y1 = this._y1;

  // Expand the tree to cover the new points.
  this.cover(x0, y0).cover(x1, y1);

  // Add the new points.
  for (i = 0; i < n; ++i) {
    add(this, xz[i], yz[i], data[i]);
  }

  return this;
}

var tree_cover = function(x, y) {
  if (isNaN(x = +x) || isNaN(y = +y)) return this; // ignore invalid points

  var x0 = this._x0,
      y0 = this._y0,
      x1 = this._x1,
      y1 = this._y1;

  // If the quadtree has no extent, initialize them.
  // Integer extent are necessary so that if we later double the extent,
  // the existing quadrant boundaries don’t change due to floating point error!
  if (isNaN(x0)) {
    x1 = (x0 = Math.floor(x)) + 1;
    y1 = (y0 = Math.floor(y)) + 1;
  }

  // Otherwise, double repeatedly to cover.
  else if (x0 > x || x > x1 || y0 > y || y > y1) {
    var z = x1 - x0,
        node = this._root,
        parent,
        i;

    switch (i = (y < (y0 + y1) / 2) << 1 | (x < (x0 + x1) / 2)) {
      case 0: {
        do parent = new Array(4), parent[i] = node, node = parent;
        while (z *= 2, x1 = x0 + z, y1 = y0 + z, x > x1 || y > y1);
        break;
      }
      case 1: {
        do parent = new Array(4), parent[i] = node, node = parent;
        while (z *= 2, x0 = x1 - z, y1 = y0 + z, x0 > x || y > y1);
        break;
      }
      case 2: {
        do parent = new Array(4), parent[i] = node, node = parent;
        while (z *= 2, x1 = x0 + z, y0 = y1 - z, x > x1 || y0 > y);
        break;
      }
      case 3: {
        do parent = new Array(4), parent[i] = node, node = parent;
        while (z *= 2, x0 = x1 - z, y0 = y1 - z, x0 > x || y0 > y);
        break;
      }
    }

    if (this._root && this._root.length) this._root = node;
  }

  // If the quadtree covers the point already, just return.
  else return this;

  this._x0 = x0;
  this._y0 = y0;
  this._x1 = x1;
  this._y1 = y1;
  return this;
};

var tree_data = function() {
  var data = [];
  this.visit(function(node) {
    if (!node.length) do data.push(node.data); while (node = node.next)
  });
  return data;
};

var tree_extent = function(_) {
  return arguments.length
      ? this.cover(+_[0][0], +_[0][1]).cover(+_[1][0], +_[1][1])
      : isNaN(this._x0) ? undefined : [[this._x0, this._y0], [this._x1, this._y1]];
};

var Quad = function(node, x0, y0, x1, y1) {
  this.node = node;
  this.x0 = x0;
  this.y0 = y0;
  this.x1 = x1;
  this.y1 = y1;
};

var tree_find = function(x, y, radius) {
  var data,
      x0 = this._x0,
      y0 = this._y0,
      x1,
      y1,
      x2,
      y2,
      x3 = this._x1,
      y3 = this._y1,
      quads = [],
      node = this._root,
      q,
      i;

  if (node) quads.push(new Quad(node, x0, y0, x3, y3));
  if (radius == null) radius = Infinity;
  else {
    x0 = x - radius, y0 = y - radius;
    x3 = x + radius, y3 = y + radius;
    radius *= radius;
  }

  while (q = quads.pop()) {

    // Stop searching if this quadrant can’t contain a closer node.
    if (!(node = q.node)
        || (x1 = q.x0) > x3
        || (y1 = q.y0) > y3
        || (x2 = q.x1) < x0
        || (y2 = q.y1) < y0) continue;

    // Bisect the current quadrant.
    if (node.length) {
      var xm = (x1 + x2) / 2,
          ym = (y1 + y2) / 2;

      quads.push(
        new Quad(node[3], xm, ym, x2, y2),
        new Quad(node[2], x1, ym, xm, y2),
        new Quad(node[1], xm, y1, x2, ym),
        new Quad(node[0], x1, y1, xm, ym)
      );

      // Visit the closest quadrant first.
      if (i = (y >= ym) << 1 | (x >= xm)) {
        q = quads[quads.length - 1];
        quads[quads.length - 1] = quads[quads.length - 1 - i];
        quads[quads.length - 1 - i] = q;
      }
    }

    // Visit this point. (Visiting coincident points isn’t necessary!)
    else {
      var dx = x - +this._x.call(null, node.data),
          dy = y - +this._y.call(null, node.data),
          d2 = dx * dx + dy * dy;
      if (d2 < radius) {
        var d = Math.sqrt(radius = d2);
        x0 = x - d, y0 = y - d;
        x3 = x + d, y3 = y + d;
        data = node.data;
      }
    }
  }

  return data;
};

var tree_remove = function(d) {
  if (isNaN(x = +this._x.call(null, d)) || isNaN(y = +this._y.call(null, d))) return this; // ignore invalid points

  var parent,
      node = this._root,
      retainer,
      previous,
      next,
      x0 = this._x0,
      y0 = this._y0,
      x1 = this._x1,
      y1 = this._y1,
      x,
      y,
      xm,
      ym,
      right,
      bottom,
      i,
      j;

  // If the tree is empty, initialize the root as a leaf.
  if (!node) return this;

  // Find the leaf node for the point.
  // While descending, also retain the deepest parent with a non-removed sibling.
  if (node.length) while (true) {
    if (right = x >= (xm = (x0 + x1) / 2)) x0 = xm; else x1 = xm;
    if (bottom = y >= (ym = (y0 + y1) / 2)) y0 = ym; else y1 = ym;
    if (!(parent = node, node = node[i = bottom << 1 | right])) return this;
    if (!node.length) break;
    if (parent[(i + 1) & 3] || parent[(i + 2) & 3] || parent[(i + 3) & 3]) retainer = parent, j = i;
  }

  // Find the point to remove.
  while (node.data !== d) if (!(previous = node, node = node.next)) return this;
  if (next = node.next) delete node.next;

  // If there are multiple coincident points, remove just the point.
  if (previous) return (next ? previous.next = next : delete previous.next), this;

  // If this is the root point, remove it.
  if (!parent) return this._root = next, this;

  // Remove this leaf.
  next ? parent[i] = next : delete parent[i];

  // If the parent now contains exactly one leaf, collapse superfluous parents.
  if ((node = parent[0] || parent[1] || parent[2] || parent[3])
      && node === (parent[3] || parent[2] || parent[1] || parent[0])
      && !node.length) {
    if (retainer) retainer[j] = node;
    else this._root = node;
  }

  return this;
};

function removeAll(data) {
  for (var i = 0, n = data.length; i < n; ++i) this.remove(data[i]);
  return this;
}

var tree_root = function() {
  return this._root;
};

var tree_size = function() {
  var size = 0;
  this.visit(function(node) {
    if (!node.length) do ++size; while (node = node.next)
  });
  return size;
};

var tree_visit = function(callback) {
  var quads = [], q, node = this._root, child, x0, y0, x1, y1;
  if (node) quads.push(new Quad(node, this._x0, this._y0, this._x1, this._y1));
  while (q = quads.pop()) {
    if (!callback(node = q.node, x0 = q.x0, y0 = q.y0, x1 = q.x1, y1 = q.y1) && node.length) {
      var xm = (x0 + x1) / 2, ym = (y0 + y1) / 2;
      if (child = node[3]) quads.push(new Quad(child, xm, ym, x1, y1));
      if (child = node[2]) quads.push(new Quad(child, x0, ym, xm, y1));
      if (child = node[1]) quads.push(new Quad(child, xm, y0, x1, ym));
      if (child = node[0]) quads.push(new Quad(child, x0, y0, xm, ym));
    }
  }
  return this;
};

var tree_visitAfter = function(callback) {
  var quads = [], next = [], q;
  if (this._root) quads.push(new Quad(this._root, this._x0, this._y0, this._x1, this._y1));
  while (q = quads.pop()) {
    var node = q.node;
    if (node.length) {
      var child, x0 = q.x0, y0 = q.y0, x1 = q.x1, y1 = q.y1, xm = (x0 + x1) / 2, ym = (y0 + y1) / 2;
      if (child = node[0]) quads.push(new Quad(child, x0, y0, xm, ym));
      if (child = node[1]) quads.push(new Quad(child, xm, y0, x1, ym));
      if (child = node[2]) quads.push(new Quad(child, x0, ym, xm, y1));
      if (child = node[3]) quads.push(new Quad(child, xm, ym, x1, y1));
    }
    next.push(q);
  }
  while (q = next.pop()) {
    callback(q.node, q.x0, q.y0, q.x1, q.y1);
  }
  return this;
};

function defaultX(d) {
  return d[0];
}

var tree_x = function(_) {
  return arguments.length ? (this._x = _, this) : this._x;
};

function defaultY(d) {
  return d[1];
}

var tree_y = function(_) {
  return arguments.length ? (this._y = _, this) : this._y;
};

function quadtree(nodes, x, y) {
  var tree = new Quadtree(x == null ? defaultX : x, y == null ? defaultY : y, NaN, NaN, NaN, NaN);
  return nodes == null ? tree : tree.addAll(nodes);
}

function Quadtree(x, y, x0, y0, x1, y1) {
  this._x = x;
  this._y = y;
  this._x0 = x0;
  this._y0 = y0;
  this._x1 = x1;
  this._y1 = y1;
  this._root = undefined;
}

function leaf_copy(leaf) {
  var copy = {data: leaf.data}, next = copy;
  while (leaf = leaf.next) next = next.next = {data: leaf.data};
  return copy;
}

var treeProto = quadtree.prototype = Quadtree.prototype;

treeProto.copy = function() {
  var copy = new Quadtree(this._x, this._y, this._x0, this._y0, this._x1, this._y1),
      node = this._root,
      nodes,
      child;

  if (!node) return copy;

  if (!node.length) return copy._root = leaf_copy(node), copy;

  nodes = [{source: node, target: copy._root = new Array(4)}];
  while (node = nodes.pop()) {
    for (var i = 0; i < 4; ++i) {
      if (child = node.source[i]) {
        if (child.length) nodes.push({source: child, target: node.target[i] = new Array(4)});
        else node.target[i] = leaf_copy(child);
      }
    }
  }

  return copy;
};

treeProto.add = tree_add;
treeProto.addAll = addAll;
treeProto.cover = tree_cover;
treeProto.data = tree_data;
treeProto.extent = tree_extent;
treeProto.find = tree_find;
treeProto.remove = tree_remove;
treeProto.removeAll = removeAll;
treeProto.root = tree_root;
treeProto.size = tree_size;
treeProto.visit = tree_visit;
treeProto.visitAfter = tree_visitAfter;
treeProto.x = tree_x;
treeProto.y = tree_y;

function x(d) {
  return d.x;
}

function y(d) {
  return d.y;
}

var manyBody = function() {
  var nodes,
      node,
      alpha,
      strength = constant(-30),
      strengths,
      // this effectively turns off the force.
      distanceXMax2 = 0,
      distanceYMax2 = 0,
      theta2 = 0.81,
      exclude;

  function force(_) {
    var i, n = nodes.length, tree = quadtree(nodes, x, y).visitAfter(accumulate);
    for (alpha = _, i = 0; i < n; ++i) node = nodes[i], tree.visit(apply);
  }

  function initialize() {
    if (!nodes) return;
    var i, n = nodes.length, node;
    strengths = new Array(n);
    for (i = 0; i < n; ++i) node = nodes[i], strengths[node.index] = +strength(node, i, nodes);
  }

  function accumulate(quad) {
    var strength = 0, q, c, x$$1, y$$1, i;

    // For internal nodes, accumulate forces from child quadrants.
    if (quad.length) {
      for (x$$1 = y$$1 = i = 0; i < 4; ++i) {
        if ((q = quad[i]) && (c = q.value)) {
          strength += c, x$$1 += c * q.x, y$$1 += c * q.y;
        }
      }
      quad.x = x$$1 / strength;
      quad.y = y$$1 / strength;
    }

    // For leaf nodes, accumulate forces from coincident quadrants.
    else {
      q = quad;
      q.x = q.data.x;
      q.y = q.data.y;
      do strength += strengths[q.data.index];
      while (q = q.next);
    }

    quad.value = strength;
  }

  function apply(quad, x1, _, x2) {
    if (!quad.value) return true;

    var x$$1 = quad.x - node.x,
        y$$1 = quad.y - node.y,
        w = x2 - x1,
        lx = x$$1 * x$$1,
        ly = y$$1 * y$$1;

    // Apply the Barnes-Hut approximation if possible.
    // Limit forces for very close nodes; randomize direction if coincident.
    if (w * w / theta2 < lx+ly) {
      /*if (l < distanceMax2) {
        if (x === 0) x = -1.5e-4, l += x * x;
        if (y === 0) y = 1.1e-4, l += y * y;
        node.vx += x * quad.value * alpha / l;
        node.vy += y * quad.value * alpha / l;
      }*/
      return true;
    }

    // Otherwise, process points directly.
    else if (quad.length || (lx >= distanceXMax2 && ly >= distanceYMax2)  ) return;

    // Limit forces for very close nodes; randomize direction if coincident.
    if (quad.data !== node || quad.next) {
      if (lx <= 0.1) lx = 1;
      if (ly <= 0.1) ly = 1;
    }

    do if (quad.data !== node) {
      var linked = 0;
      if ( exclude ) {
        exclude.forEach(
        function(e) {
          if ( (e.source.id == node.id && e.target.id == quad.data.id) ||
               (e.target.id == node.id && e.source.id == quad.data.id) ) {
            linked = 1;
          } } );
      }
      if( !linked ) {
        if ( lx < distanceXMax2 ) {
          node.yx += strengths[quad.data.index] * alpha / ly;
        }
        if ( ly < distanceYMax2 ) {
          node.vx += strengths[quad.data.index] * alpha / lx;
        }
      }
    } while (quad = quad.next);
  }

  force.initialize = function(_) {
    nodes = _;
    initialize();
  };

  force.strength = function(_) {
    return arguments.length ? (strength = typeof _ === "function" ? _ : constant(+_), initialize(), force) : strength;
  };

  force.distanceXMax = function(_) {
    return arguments.length ? (distanceXMax2 = _ * _, force) : Math.sqrt(distanceXMax2);
  };

  force.distanceYMax = function(_) {
    return arguments.length ? (distanceYMax2 = _ * _, force) : Math.sqrt(distanceYMax2);
  };

  force.theta = function(_) {
    return arguments.length ? (theta2 = _ * _, force) : Math.sqrt(theta2);
  };

  force.exclude = function(_) {
    return arguments.length ? (exclude = _) : exclude;
  };

  return force;
};

var collision = function() {
  var nodes,
      node,
      alpha,
      strength = constant(-30),
      strengths,
      // this effectively turns off the collision detection.
      distanceX2 = 0,
      distanceY2 = 0,
      theta2 = 0.81,
      exclude;

  function force(alpha) {
    // brute force implementation
    for( var i=0,nodei; i < nodes.length; i++) {
      nodei = nodes[i];
      for( var j=i+1,nodej; j < nodes.length; j++) {
        nodej = nodes[j];
        var x$$1 = (nodei.x-nodej.x);
        var y$$1 = (nodei.y-nodej.y);
        if (x$$1*x$$1 <= distanceX2  && y$$1*y$$1 <= distanceY2) {
          var linked = 0;
          if ( exclude ) {
            exclude.forEach( function(e) {
                         if ( (e.source.id == nodei.id && e.target.id == nodej.id) ||
                              (e.target.id == nodej.id && e.source.id == nodei.id) ) {
                           linked = 1;
                         } });
          }
          if (!linked ) {
            nodei.vx += strengths[i] * (x$$1<0?-1:+1) * alpha;
            nodej.vx += strengths[j] * (x$$1<0?+1:-1) * alpha;
            nodei.vy += strengths[i] * (y$$1<0?-1:+1) * alpha;
            nodej.vy += strengths[j] * (y$$1<0?+1:-1) * alpha;
          }
        }
      }
    }

    /*
    var i, n = nodes.length, tree = quadtree(nodes, x, y).visitAfter(accumulate);
    for (alpha = _, i = 0; i < n; ++i) node = nodes[i], tree.visit(apply); */
  }

  function initialize() {
    if (!nodes) return;
    var i, n = nodes.length, node;
    strengths = new Array(n);
    for (i = 0; i < n; ++i) node = nodes[i], strengths[node.index] = +strength(node, i, nodes);
  }

  /*
  function accumulate(quad) {
    var strength = 0, q, c, x, y, i;

    // For internal nodes, accumulate forces from child quadrants.
    if (quad.length) {
      for (x = y = i = 0; i < 4; ++i) {
        if ((q = quad[i]) && (c = q.value)) {
          strength += c, x += c * q.x, y += c * q.y;
        }
      }
      quad.x = x / strength;
      quad.y = y / strength;
    }

    // For leaf nodes, accumulate forces from coincident quadrants.
    else {
      q = quad;
      q.x = q.data.x;
      q.y = q.data.y;
      do strength += strengths[q.data.index];
      while (q = q.next);
    }

    quad.value = strength;
  }

  function apply(quad, x1, _, x2) {
    if (!quad.value) return true;

    var x = quad.x - node.x,
        y = quad.y - node.y,
        w = x2 - x1,
        lx = x * x,
        ly = y * y;

    // Apply the Barnes-Hut approximation if possible.
    // Limit forces for very close nodes; randomize direction if coincident.
    if (w * w / theta2 < lx+ly) {
      if (l < distanceMax2) {
        if (x === 0) x = -1.5e-4, l += x * x;
        if (y === 0) y = 1.1e-4, l += y * y;
        node.vx += x * quad.value * alpha / l;
        node.vy += y * quad.value * alpha / l;
      }
      return true;
    }

    // Otherwise, process points directly.
    else if (quad.length || (lx >= distanceX2 && ly >= distanceY2)  ) return;

    do if (quad.data !== node) {
      var linked = 0;
      if ( exclude ) {
        exclude.forEach(
        function(e) {
          if ( (e.source.id == node.id && e.target.id == quad.data.id) ||
               (e.target.id == node.id && e.source.id == quad.data.id) ) {
            linked = 1;
          } } );
      }
      if( !linked ) {
        if ( lx < distanceX2 ) {
          node.vx += strengths[quad.data.index] * alpha;
        }
        if ( ly < distanceY2 ) {
          node.vy += strengths[quad.data.index] * alpha;
        }
      }
    } while (quad = quad.next);
  }
  */

  force.initialize = function(_) {
    nodes = _;
    initialize();
  };

  force.strength = function(_) {
    return arguments.length ? (strength = typeof _ === "function" ? _ : constant(+_), initialize(), force) : strength;
  };

  force.distanceX = function(_) {
    return arguments.length ? (distanceX2 = _ * _, force) : Math.sqrt(distanceX2);
  };

  force.distanceY = function(_) {
    return arguments.length ? (distanceY2 = _ * _, force) : Math.sqrt(distanceY2);
  };

  force.theta = function(_) {
    return arguments.length ? (theta2 = _ * _, force) : Math.sqrt(theta2);
  };

  force.exclude = function(_) {
    return arguments.length ? (exclude = _) : exclude;
  };

  return force;
};

function index$1(d) {
  return d.index;
}

var grid = function(nodes) {
  var id = index$1,
      strength = defaultStrength,
      strengths,
      xDistance = constant(50),
      yDistance = constant(30),
      xDistances,
      yDistances,
      iterations = 1;

  if (nodes == null) nodes = [];

  function defaultStrength(node) {
    return 1;
  }

  function force(alpha) {
    for (var k = 0, n = nodes.length; k < iterations; ++k) {
      for (var i = 0, node, x, y, l; i < n; ++i) {
        node = nodes[i];
        // we want to return to a integer multiple of (xDistance[i],yDistance[i]).
        // so the force computation has to rely on a modulo operation.
        // we cannot just say (x%distance)/distance. This returns a
        // number between 0 and 1. We need ((x+distance/2)%distance)/distance-.5
        // to return a number between -1/2 and +1/2
        x = ((node.x+xDistances[i]/2) % xDistances[i])/xDistances[i] - .5;
        y = ((node.y+yDistances[i]/2) % yDistances[i])/yDistances[i] - .5;
        node.vx -= x * alpha * strengths[i];
        node.vy -= y * alpha * strengths[i];
      }
    }
  }

  function initialize() {
    if (!nodes) return;

    strengths = new Array(nodes.length), initializeStrength();
    xDistances = new Array(nodes.length), initializeDistance(xDistances,xDistance);
    yDistances = new Array(nodes.length), initializeDistance(yDistances,yDistance);
  }

  function initializeStrength() {
    if (!nodes) return;

    for (var i = 0, n = nodes.length; i < n; ++i) {
      strengths[i] = +strength(nodes[i], i, nodes);
    }
  }

  function initializeDistance(distances,distance) {
    if (!nodes) return;

    for (var i = 0, n = nodes.length; i < n; ++i) {
      distances[i] = +distance(nodes[i], i, nodes);
    }
  }

  force.initialize = function(_) {
    nodes = _;
    initialize();
  };

  force.nodes = function(_) {
    return arguments.length ? (nodes = _, initialize(), force) : nodes;
  };

  force.id = function(_) {
    return arguments.length ? (id = _, force) : id;
  };

  force.iterations = function(_) {
    return arguments.length ? (iterations = +_, force) : iterations;
  };

  force.strength = function(_) {
    return arguments.length ? (strength = typeof _ === "function" ? _ : constant(+_), initializeStrength(), force) : strength;
  };

  force.xDistance = function(_) {
    return arguments.length ? (xDistance = typeof _ === "function" ? _ : constant(+_), initializeDistance(xDistances,xDistance), force) : xDistance; };

  force.yDistance = function(_) {
    return arguments.length ? (yDistance = typeof _ === "function" ? _ : constant(+_), initializeDistance(yDistances,yDistance), force) : xDistance; };

  return force;
};

function index$2(d) {
  return d.index;
}

var gravity = function(nodes) {
  var id = index$2,
      strengthX = defaultStrength,
      strengthY = defaultStrength,
      strengthXs,
      strengthYs,
      iterations = 1;

  if (nodes == null) nodes = [];

  function defaultStrength(node) {
    return 1;
  }

  function force(alpha) {
    for (var k = 0, n = nodes.length; k < iterations; ++k) {
      for (var i = 0, node, x, y, l; i < n; ++i) {
        node = nodes[i];
        node.vx += alpha * strengthXs[i];
        node.vy += alpha * strengthYs[i];
      }
    }
  }

  function initialize() {
    if (!nodes) return;

    strengthXs = new Array(nodes.length);
    strengthYs = new Array(nodes.length);
    initializeStrength();
  }

  function initializeStrength() {
    if (!nodes) return;

    // if there is a net strength, the thing will fall.
    // unless there is some other constraint. we'll
    // normalize to no net gravity to guard against that.
    for (var i = 0,xSum=0,ySum=0, n = nodes.length; i < n; ++i) {
      strengthXs[i] = +strengthX(nodes[i], i, nodes);
      strengthYs[i] = +strengthY(nodes[i], i, nodes);
      xSum += strengthXs[i];
      ySum += strengthYs[i];
    }
    xSum = xSum / nodes.length;
    ySum = ySum / nodes.length;
    for(var i = 0, n = nodes.length; i < n; ++i) {
      strengthXs[i] = strengthXs[i] - xSum;
      strengthYs[i] = strengthYs[i] - ySum;
    }
  }

  force.initialize = function(_) {
    nodes = _;
    initialize();
  };

  force.nodes = function(_) {
    return arguments.length ? (nodes = _, initialize(), force) : nodes;
  };

  force.id = function(_) {
    return arguments.length ? (id = _, force) : id;
  };

  force.iterations = function(_) {
    return arguments.length ? (iterations = +_, force) : iterations;
  };


  force.strengthX = function(_) {
    return arguments.length ? (strengthX = typeof _ === "function" ? _ : constant(+_), initializeStrength(), force) : strengthX;
  };

  force.strengthY = function(_) {
    return arguments.length ? (strengthY = typeof _ === "function" ? _ : constant(+_), initializeStrength(), force) : strengthY;
  };

  return force;
};

exports.forceGridLink = link;
exports.forceGridManyBody = manyBody;
exports.forceGridCollision = collision;
exports.forceGrid = grid;
exports.forceGravity = gravity;

Object.defineProperty(exports, '__esModule', { value: true });

})));
