//Grid
var _nodes = [];

var _destinations = [];

var _destinationNodes = [];

var _items = [];

var _update = true;

var _selectedRack;

const ENTRANCE = 0;
const EXIT = 49;
const DEBUG = false;

function setupStore(nodes, racks, items) {
    console.log(items);
    clearAll();
    _racks = racks;
    _nodes = nodes;
    _items = items;
    getDestinations();
    drawStore();
    runDjikstra();
}

function setupUpdateStore(nodes, racks, items) {
    clearAll();
    _racks = racks;
    _nodes = nodes;
    _items = items;
    getUpdateDestinations();
    drawStore();
}

function rackSelected(selectedRackId) {
    _selectedRack = selectedRackId;
    clearCanvas();
    drawStore();
    drawSelectedRack(selectedRackId);
}

function storeUpdate(itemName) {
    var itemId = getItemIdForName(itemName);

    if (itemId) {
        updateDestinations(itemId);
        updateRackForItem(itemId);
        _update = true;
    }
    clearCanvas();
    drawStore();
}

function updateDestinations(itemId) {
    var oldRack = _items[itemId].RACK_ID;

    removeItemFromArray(_destinations, String(oldRack));
    _destinations.push(String(_selectedRack));
}

function getItemIdForName(itemName) {
    for (var i = 0; i < _items.length; i++) {
        if (_items[i].NAME === itemName) {
            return i;
        }
    }
}

function updateRackForItem(itemId) {
    _items[itemId].RACK_ID = _selectedRack;
}

function clearAll() {
    _update = true;
    _destinations = [];
    _destinationNodes = [];
    clearCanvas();
}

function getDestinations() {
    for (var i = 0; i < _items.length; i++) {
        if (_items[i].DRAW === "1") {
            _destinations.push(_items[i].RACK_ID);
        }
    }
}

function getUpdateDestinations() {
    for (var i = 0; i < _items.length; i++) {
        _destinations.push(_items[i].RACK_ID);
    }
}

function drawStore() {
    drawRacks();
    if (_update) {
        updateDestinationNodes();
    }
    drawDestinations();
    if (DEBUG) {
        drawGrid();
    }
}

function clearPath() {
    clearCanvas();
    drawStore();
}

function updateDestinationNodes() {
    _destinationNodes = [];
    var duplicatePrevention = Array.apply(null, Array(_nodes.length)).map(Number.prototype.valueOf, 0);

    for (var i = 0; i < _items.length; i++) {
        var destination = _racks[_items[i].RACK_ID].node;
        if (duplicatePrevention[destination] === 0) {
            _destinationNodes.push(destination);
            duplicatePrevention[destination]++;
        }
    }
    _update = false;
}

function clearCanvas() {
    var ctx = $('#myCanvas')[0].getContext("2d");
    var canvas = $('#myCanvas')[0];
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function drawDestinations() {
    for (var i = 0; i < _destinations.length; i++) {
        drawRack(_racks[_destinations[i]], true);
    }
}

function drawRacks() {
    for (var i = 0; i < _racks.length; i++) {
        drawRack(_racks[i], false);
    }
}

function drawRack(rack, destination) {
    var ctx = $('#myCanvas')[0].getContext("2d");

    ctx.beginPath();

    ctx.rect(rack.x, rack.y, rack.width, rack.height);
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'black';
    if (destination) {
        ctx.fillStyle = 'red';
        ctx.fill();
    }
    ctx.stroke();
}

function drawSelectedRack(selectedRackId) {
    var ctx = $('#myCanvas')[0].getContext("2d");
    var rack = racks[selectedRackId];

    ctx.beginPath();

    ctx.rect(rack.x, rack.y, rack.width, rack.height);
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'black';
    ctx.fillStyle = 'yellow';
    ctx.fill();
    
    ctx.stroke();
}

function drawGrid() {
    var ctx = $('#myCanvas')[0].getContext("2d");

    for (var i = 0; i < _nodes.length; i++) {

        ctx.beginPath();
        ctx.arc(_nodes[i].x, _nodes[i].y, 3, 0, 6.28, false);
        ctx.fillStyle = 'blue';
        ctx.fill();
        ctx.lineWidth = 1;
        ctx.strokeStyle = 'black';
        ctx.stroke();
    }
}

function drawPaths(paths) {
    for (var i = 0; i < paths.length; i++) {
        for (var j = 0; j < paths[i].length - 1; j++) {
            drawPath(paths[i][j], paths[i][j + 1]);
        }
    }
}

function drawPath(startNode, endNode) {
    var ctx = $('#myCanvas')[0].getContext("2d");

    ctx.beginPath();

    ctx.beginPath()
    ctx.moveTo(_nodes[startNode].x, _nodes[startNode].y);
    ctx.lineTo(_nodes[endNode].x, _nodes[endNode].y);
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'blue';
    ctx.stroke();
}

function Dijkstra(Graph, source) {

    var Q = [];
    var dist = [];
    var prev = [];

    for (var v = 0; v < Graph.length; v++) {
        dist[v] = 10000;                            // Unknown distance from source to v
        prev[v] = -1;                 				// Previous node in optimal path from source
        Q.push(v);
    }

    dist[source] = 0;                               // Distance from source to source

    var counter = 100;

    while (Q.length && counter-- >= 0) {

        u = minVertex(Q, dist);     		// Node with the least distance
                                            // will be selected first
        removeItemFromArray(Q, u);

        for (i = 0; i < _nodes[u].adjacencies.length; i++) {
            v = _nodes[u].adjacencies[i];
            var isIndexOf = Q.indexOf(v);

            if (isIndexOf >= 0) {
                alt = dist[u] + 1;

                if (alt < dist[v]) {
                    dist[v] = alt;
                    prev[v] = u;
                }
            }              // A shorter path to v has been found
        }
    }
    return [dist, prev];
}

function minVertex(Q, dist) {
    if (dist.length === 0) {
        return -1;
    }

    var minDistance = dist[Q[0]];
    var minVrtx = Q[0];

    for (var i = 0; i < Q.length; i++) {
        if (dist[Q[i]] < minDistance) {
            minVrtx = Q[i];
            minDistance = dist[Q[i]];
        }
    }
    return minVrtx;
}

function removeItemFromArray(array, item) {
    var index = array.indexOf(item);
    if (index !== -1) array.splice(index, 1);
}

function findBestOrder(distances) {
    var permitations = permutator(_destinationNodes);

    var minPathLength = 10000;
    var pathLength = 0;
    var minPath = [];
    var node;
    var path = [];
    for (var i = 0; i < permitations.length; i++) {
        path = [];
        node = permitations[i][0];

        path.push(node);
        pathLength = distances[0][node];
        var currentNode = permitations[i][0];
        for (var j = 0; j < permitations[i].length - 1; j++) {
            var nextNode = permitations[i][j + 1];
            var n = _destinationNodes.indexOf(currentNode);
            pathLength += distances[n + 1][nextNode];
            path.push(nextNode);
            currentNode = nextNode;
        }

        var n = _destinationNodes.indexOf(currentNode);

        pathLength += distances[n + 1][EXIT];
        path.push(EXIT);

        if (pathLength < minPathLength) {
            minPathLength = pathLength;
            minPath = path;
        }
        
    }

    return minPath;
}

function getPathForBestOrder(order, paths) {
    var path = [];
    path.push(getPathTo(order[0], paths[0]));
    path[0].push(ENTRANCE);
    for (var i = 1; i < order.length; i++) {
        var startNodeIndex = _destinationNodes.indexOf(order[i - 1]);
        path.push(getPathTo(order[i], paths[startNodeIndex + 1]));
    }
    return path;
}

function getPathTo(destNode, prev) {
    var path = [destNode];
    var prevNode = prev[destNode];
    
    while (prevNode > 0) {
        path.push(prevNode);
        prevNode = prev[prevNode];
    }
    return path;
}

function runDjikstra() {
    try {
        if (!(_destinationNodes.length > 0)) {
            return false;
        }
        var result = Dijkstra(_nodes, ENTRANCE);
        var shortestDistances = [result[0]];
        var shortestPaths = [result[1]];

        for (var i = 0; i < _destinationNodes.length; i++) {
            result = Dijkstra(_nodes, _destinationNodes[i]);
            shortestDistances.push(result[0]);
            shortestPaths.push(result[1]);
        }

        result = Dijkstra(_nodes, EXIT);
        shortestDistances.push(result[0]);
        shortestPaths.push(result[1]);

        var bestOrder = findBestOrder(shortestDistances)
        var paths = getPathForBestOrder(bestOrder, shortestPaths);
        drawPaths(paths);
    }
    catch (err) {
        console.log(err);
    }
    finally {
        return false;
    }
}

function permutator(inputArr) {
    var results = [];

    function permute(arr, memo) {
        var cur, memo = memo || [];

        for (var i = 0; i < arr.length; i++) {
            cur = arr.splice(i, 1);
            if (arr.length === 0) {
                results.push(memo.concat(cur));
            }
            permute(arr.slice(), memo.concat(cur));
            arr.splice(i, 0, cur[0]);
        }

        return results;
    }
    return permute(inputArr);
}

function clearDestinations() {
    _destinations = [];
    _update = true;
    clearCanvas();
    drawStore();
}


