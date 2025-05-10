package com.example.malvoayant.ui.screens

import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
const val DOOR_PROXIMITY_THRESHOLD=2.5f
// Types de nœuds possibles
const val NODE_TYPE_DOOR = "door"
const val NODE_TYPE_ROOM = "room"
const val NODE_TYPE_POI = "poi"
const val NODE_TYPE_WINDOW = "window"
const val NODE_TYPE_ZONE = "zone"
const val NODE_TYPE_TEMP = "temp"
// 1. Représentation d'un graphe avec des nœuds et arêtes pondérées
data class Node(val id: String, val x: Float, val y: Float, val type: String)
data class Edge(val from: String, val to: String, val weight: Double)

class NavigationGraph {
    val nodes = mutableListOf<Node>()
    val edges = mutableListOf<Edge>()

    // 2. Ajouter les éléments navigables comme nœuds
    fun addNavigableElements(floorPlan: FloorPlanState) {
        // Ajouter les portes comme nœuds
        floorPlan.doors.forEach { door ->
            nodes.add(Node("door_${door.x}_${door.y}", door.x, door.y, "door"))
        }


        // Ajouter les POIs comme nœuds (destinations possibles)
        floorPlan.pois.forEach { poi ->
            nodes.add(Node("poi_${poi.name}", poi.x, poi.y, "poi"))
        }

        // Ajouter les windows comme nœuds (destinations possibles)
        floorPlan.windows.forEach { window ->
            nodes.add(Node("window_${window.x}_${window.y}", window.x, window.y,"window"))
        }

        // Ajouter les centroïdes des pièces comme nœuds
        floorPlan.rooms.polygons.forEach { room ->
            if (room.name != "Room 1") { // Exclude Room 1
                val center = room.center // Utilise le centre prédéfini ou calcule-le
                nodes.add(Node("room_${room.name}", center.x, center.y, "room"))
            }
        }

        // Ajouter les centroïdes des zones comme nœuds
        floorPlan.zones.forEach { zone ->
            val center = zone.center // Utilise le centre prédéfini ou calcule-le
            nodes.add(Node("zone${zone.name}", center.x, center.y, "zone"))
        }
    }
    private fun calculatePolygonCenter(points: List<Point>): Point {
        var centroidX = 0f
        var centroidY = 0f
        val n = points.size

        points.forEach { point ->
            centroidX += point.x
            centroidY += point.y
        }

        return Point(centroidX / n, centroidY / n)
    }

    // 3. Connecter les nœuds avec des arêtes pondérées
    /*fun connectNodes(floorPlan: FloorPlanState) {
        // Règle 1: Connecter les portes aux pièces adjacentes (ex: porte entre salle A et couloir)
        floorPlan.doors.forEach { door ->
            val doorNode = nodes.find { it.id == "door_${door.x}_${door.y}" } ?: return@forEach

            // Trouver les pièces adjacentes à la porte (à implémenter via overlap avec RoomPolygon)
            val adjacentRooms = findAdjacentRooms(door.x, door.y, floorPlan.rooms.polygons)

            adjacentRooms.forEach { room ->
                val roomNode = nodes.find { it.id == "room_${room.name}" } ?: return@forEach
                // Arête porte <-> pièce (poids = distance)
                val weight = distance(doorNode.x, doorNode.y, roomNode.x, roomNode.y)
                edges.add(Edge(doorNode.id, roomNode.id, weight))
                edges.add(Edge(roomNode.id, doorNode.id, weight)) // Graphe non dirigé
            }
        }

        // Règle 2: Connecter les POIs aux portes/pièces les plus proches
        floorPlan.pois.forEach { poi ->
            val poiNode = nodes.find { it.id == "poi_${poi.id}" } ?: return@forEach
            val closestDoor = findClosestNode(poiNode, nodes.filter { it.type == "door" })
            closestDoor?.let {
                val weight = distance(poiNode.x, poiNode.y, it.x, it.y)
                edges.add(Edge(poiNode.id, it.id, weight))
                edges.add(Edge(it.id, poiNode.id, weight))
            }
        }
    }*/
   /* fun connectNodes(floorPlan: FloorPlanState) {
        // 1. Connect doors to adjacent rooms
        floorPlan.doors.forEach { door ->
            val doorNode = nodes.find { it.type == "door" && it.x == door.x && it.y == door.y }
            doorNode?.let { dn ->
                // Find rooms containing this door
                val adjacentRooms = floorPlan.rooms.polygons.filter { room ->
                    isPointInPolygon(door.x, door.y, room.coords)
                }

                adjacentRooms.forEach { room ->
                    val roomNode = nodes.find { it.type == "room" && it.id == "room_${room.name}" }
                    roomNode?.let { rn ->
                        val weight = calculateDistance(dn, rn).toDouble()
                        addBidirectionalEdge(dn.id, rn.id, weight)
                    }
                }
            }
        }

        // 2. Connect POIs to nearest doors (existing code)
        floorPlan.pois.forEach { poi ->
            val poiNode = nodes.find { it.id == "poi_${poi.id}" } ?: return@forEach
            val nearestDoor = findClosestNode(poiNode, nodes.filter { it.type == "door" })
            nearestDoor?.let {
                val weight = calculateDistance(poiNode, it).toDouble()
                addBidirectionalEdge(poiNode.id, it.id, weight)
            }
        }
    }*/
    fun connectNodes(floorPlan: FloorPlanState) {
        // 1. Connecter les portes aux pièces adjacentes
        connectDoorsToRooms(floorPlan)

        // 2. Connecter les éléments internes aux pièces
        connectInternalElements(floorPlan)

        // 3. Connecter les POIs/fenêtres aux portes de leur pièce
        connectPoisToRoomDoors(floorPlan)
    }

    private fun connectDoorsToRooms(floorPlan: FloorPlanState) {
        floorPlan.doors.forEach { door ->
            val doorNode = nodes.find { it.type == NODE_TYPE_DOOR && it.id == "door_${door.x}_${door.y}" } ?: return@forEach

            // Trouver les pièces contenant cette porte
            val adjacentRooms = floorPlan.rooms.polygons.filter { room ->
                isPointInPolygon(door.x, door.y, room.coords)
            }

            adjacentRooms.forEach { room ->
                val roomNode = nodes.find { it.type == NODE_TYPE_ROOM && it.id == "room_${room.name}" } ?: return@forEach
                val weight = calculateDistance(doorNode, roomNode).toDouble()
                addBidirectionalEdge(doorNode.id, roomNode.id, weight)
            }
        }
    }

    private fun connectInternalElements(floorPlan: FloorPlanState) {
        floorPlan.rooms.polygons.forEach { room ->
            val roomNode = nodes.find { it.type == NODE_TYPE_ROOM && it.id == "room_${room.name}" } ?: return@forEach

            // Trouver tous les POIs, fenêtres et zones de cette pièce
            val roomElements = nodes.filter { node ->
                when (node.type) {
                    NODE_TYPE_POI, NODE_TYPE_WINDOW, NODE_TYPE_ZONE ->
                        isPointInPolygon(node.x, node.y, room.coords)
                    else -> false
                }
            }

            // 1. Connecter tous les éléments entre eux
            for (i in 0 until roomElements.size) {
                for (j in i+1 until roomElements.size) {
                    val weight = calculateDistance(roomElements[i], roomElements[j]).toDouble()
                    addBidirectionalEdge(roomElements[i].id, roomElements[j].id, weight)
                }
            }

            // 2. Connecter tous les éléments au centre de la pièce
            roomElements.forEach { element ->
                val weight = calculateDistance(element, roomNode).toDouble()
                addBidirectionalEdge(element.id, roomNode.id, weight)
            }
        }
    }

    private fun connectPoisToRoomDoors(floorPlan: FloorPlanState) {
        // Pour chaque pièce
        floorPlan.rooms.polygons.forEach { room ->
            val roomNode = nodes.find { it.type == NODE_TYPE_ROOM && it.id == "room_${room.name}" } ?: return@forEach

            // Trouver les portes de cette pièce
            val roomDoors = nodes.filter { node ->
                node.type == NODE_TYPE_DOOR && isPointInPolygon(node.x, node.y, room.coords)
            }

            // Trouver tous les POIs et fenêtres de cette pièce
            val roomPois = nodes.filter { node ->
                when (node.type) {
                    NODE_TYPE_POI, NODE_TYPE_WINDOW ->
                        isPointInPolygon(node.x, node.y, room.coords)
                    else -> false
                }
            }

            // Connecter chaque POI/fenêtre à chaque porte de la pièce
            roomPois.forEach { poi ->
                roomDoors.forEach { door ->
                    val weight = calculateDistance(poi, door).toDouble()
                    addBidirectionalEdge(poi.id, door.id, weight)
                }
            }
        }
    }

    fun addBidirectionalEdge(from: String, to: String, weight: Double) {
        edges.add(Edge(from, to, weight))
        edges.add(Edge(to, from, weight))
    }

    // Helper function to check if a point is inside a polygon
    fun isPointInPolygon(pointX: Float, pointY: Float, polygon: List<Point>): Boolean {
        var inside = false
        val n = polygon.size

        // Ray-casting algorithm
        for (i in 0 until n) {
            val j = (i + 1) % n
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y

            // Check if point is on a horizontal edge
            if ((yi == yj) && (yi == pointY) && (pointX > min(xi, xj)) && (pointX < max(xi, xj))) {
                return true
            }

            // Check if point is on a vertical edge
            if ((xi == xj) && (xi == pointX) && (pointY > min(yi, yj)) && (pointY < max(yi, yj))) {
                return true
            }

            // Check if the point is exactly a vertex
            if (pointX == xi && pointY == yi) {
                return true
            }

            // Check intersection
            val intersect = ((yi > pointY) != (yj > pointY)) &&
                    (pointX < (xj - xi) * (pointY - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
        }

        return inside
    }

    // 4. Pénaliser les zones dangereuses (ex: poids * 10 si arête traverse une zone à risque)
    fun applyRiskPenalties(floorPlan: FloorPlanState) {
        floorPlan.zones.filter { it.zone_type == "danger" }.forEach { zone ->
            edges.forEach { edge ->
                if (edgeCrossesZone(edge, zone)) {
                    edges.remove(edge)
                    edges.add(edge.copy(weight = edge.weight * 10.0))
                }
            }
        }
    }
    fun NavigationGraph.connectToNearest(
        tempNode: Node,
        floorPlan: FloorPlanState,
        maxDistance: Float = 3.0f // Distance maximale de connexion (en mètres)
    ) {
        // 1. Trouver les nœuds connectables (exclure les murs/zones dangereuses)
        val connectableNodes = nodes.filter { node ->
            when (node.type) {
                "door", "poi", "room" -> true
                else -> false
            } && calculateDistance(tempNode, node) <= maxDistance
        }

        // 2. Connecter aux 2 nœuds les plus proches (évite les chemins illogiques)
        connectableNodes
            .sortedBy { calculateDistance(tempNode, it) }
            .take(2)
            .forEach { nearestNode ->
                val weight = calculateDistance(tempNode, nearestNode).toDouble()

                // Double connexion (graphe non-orienté)
                edges.add(Edge(tempNode.id, nearestNode.id, weight))
                edges.add(Edge(nearestNode.id, tempNode.id, weight))

                // Logs pour débogage
                println("Connecté ${tempNode.id} à ${nearestNode.id} (distance=$weight)")
            }
    }
//**************************************************
//utils
//**************************************************
// Trouve les pièces adjacentes à une porte/fenêtre
    fun findAdjacentRooms(x: Float, y: Float, rooms: List<RoomPolygon>): List<RoomPolygon> {
        return rooms.filter { room ->
            room.coords.any { point -> distance(point.x, point.y, x, y) < DOOR_PROXIMITY_THRESHOLD }
        }
    }

    // Calcule la distance euclidienne entre deux points
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2).toDouble())
    }

    // Vérifie si une arête traverse une zone dangereuse
    fun edgeCrossesZone(edge: Edge, zone: Zone): Boolean {
        val fromNode = nodes.find { it.id == edge.from }!!
        val toNode = nodes.find { it.id == edge.to }!!
        return isLineIntersectingPolygon(
            fromNode.x, fromNode.y,
            toNode.x, toNode.y,
            zone.coords.map { it.x to it.y }
        )
    }

    fun connectToNearest(
        tempNode: Node,
        floorPlan: FloorPlanState,
        maxDistance: Float = 3.0f // Distance maximale de connexion (en mètres)
    ) {
        // 1. Trouver les nœuds connectables (exclure les murs/zones dangereuses)
        val connectableNodes = nodes.filter { node ->
            when (node.type) {
                "door", "poi", "room" -> true
                else -> false
            } && calculateDistance(tempNode, node) <= maxDistance
        }

        // 2. Connecter aux 2 nœuds les plus proches (évite les chemins illogiques)
        connectableNodes
            .sortedBy { calculateDistance(tempNode, it) }
            .take(2)
            .forEach { nearestNode ->
                val weight = calculateDistance(tempNode, nearestNode).toDouble()

                // Double connexion (graphe non-orienté)
                edges.add(Edge(tempNode.id, nearestNode.id, weight))
                edges.add(Edge(nearestNode.id, tempNode.id, weight))

                // Logs pour débogage
                println("Connecté ${tempNode.id} à ${nearestNode.id} (distance=$weight)")
            }
    }
}

//fonction isLineIntersectingPolygon




// Vérifie si un segment (p1-p2) intersecte un côté du polygone (p3-p4)
private fun isIntersecting(
    p1: Point, p2: Point,
    p3: Point, p4: Point
): Boolean {
    // Calcul des orientations (pour vérifier si les segments se croisent)
    fun orientation(a: Point, b: Point, c: Point): Int {
        val crossProduct = (b.y - a.y) * (c.x - b.x) - (b.x - a.x) * (c.y - b.y)
        return when {
            crossProduct < 0 -> 2 // Anti-horaire
            crossProduct > 0 -> 1 // Horaire
            else -> 0 // Colinéaire
        }
    }

    val o1 = orientation(p1, p2, p3)
    val o2 = orientation(p1, p2, p4)
    val o3 = orientation(p3, p4, p1)
    val o4 = orientation(p3, p4, p2)

    // Cas général : les segments se croisent
    if (o1 != o2 && o3 != o4) return true

    // Cas particuliers : segments colinéaires et superposés
    if (o1 == 0 && isOnSegment(p1, p3, p2)) return true
    if (o2 == 0 && isOnSegment(p1, p4, p2)) return true
    if (o3 == 0 && isOnSegment(p3, p1, p4)) return true
    if (o4 == 0 && isOnSegment(p3, p2, p4)) return true

    return false
}

// Vérifie si le point q est sur le segment p-r
private fun isOnSegment(p: Point, q: Point, r: Point): Boolean {
    return (q.x <= max(p.x, r.x) &&
            (q.x >= min(p.x, r.x)) &&
            (q.y <= max(p.y, r.y)) &&
            (q.y >= min(p.y, r.y)))
}

// Fonction principale : vérifie si la ligne (p1-p2) intersecte le polygone
fun isLineIntersectingPolygon(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    polygon: List<Pair<Float, Float>>
): Boolean {
    val lineStart = Point(x1, y1)
    val lineEnd = Point(x2, y2)

    // Convertir les paires (Float, Float) en Points
    val polygonPoints = polygon.map { Point(it.first, it.second) }

    // Vérifier chaque côté du polygone
    for (i in polygonPoints.indices) {
        val j = (i + 1) % polygonPoints.size
        val polyEdgeStart = polygonPoints[i]
        val polyEdgeEnd = polygonPoints[j]

        if (isIntersecting(lineStart, lineEnd, polyEdgeStart, polyEdgeEnd)) {
            return true
        }
    }

    return false
}

// Trouve le nœud le plus proche d'un point donné
fun findClosestNode(
    targetPoint: Point,
    nodes: List<Node>,
    maxDistance: Float = Float.POSITIVE_INFINITY // Distance maximale autorisée
): Node? {
    if (nodes.isEmpty()) return null

    var closestNode: Node? = null
    var minDistance = Float.POSITIVE_INFINITY

    for (node in nodes) {
        val distance = calculateDistance(targetPoint, Point(node.x, node.y))

        if (distance < minDistance && distance <= maxDistance) {
            minDistance = distance
            closestNode = node
        }
    }

    return closestNode
}

// Version alternative pour trouver le nœud le plus proche parmi une liste filtrée
fun findClosestNode(
    targetNode: Node,
    nodes: List<Node>,
    filterCondition: (Node) -> Boolean = { true }
): Node? {
    return nodes
        .filter(filterCondition)
        .minByOrNull { calculateDistance(targetNode, it) }
}

// Calcule la distance euclidienne entre deux points
fun calculateDistance(a: Point, b: Point): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}

// Calcule la distance entre deux nœuds
fun calculateDistance(a: Node, b: Node): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}

//appeler djikstra
fun findPath(
    start: Any, // Accepte Point ou POI
    destination: Any, // Accepte Point, POI, Door ou Window
    floorPlan: FloorPlanState
): List<Point> {
    Log.d("here","i'm here lol1")
    val graph = NavigationGraph().apply {
        addNavigableElements(floorPlan)
        connectNodes(floorPlan)
        applyRiskPenalties(floorPlan)
    }
    Log.d("here","i'm here lol2")
    // Gestion du point de départ
    val startNode = when (start) {
        is POI -> graph.nodes.find { it.id == "poi_${start.name}" }
            ?: throw IllegalArgumentException("POI de départ introuvable")

        is Point -> createTempNode(start, "start", floorPlan, graph)
        else -> throw IllegalArgumentException("Type de départ non supporté")
    }
    Log.d("here","i'm here lol3")
    // Gestion du point d'arrivée
    val (destNode, isTempDest) = when (destination) {
        is POI -> graph.nodes.find { it.id == "poi_${destination.name}" } to false
        is DoorWindow -> when {
            destination.type.contains(
                "door",
                ignoreCase = true
            ) -> graph.nodes.find { it.id == "door_${destination.x}_${destination.y}" } to false

            destination.type.contains(
                "window",
                ignoreCase = true
            ) -> graph.nodes.find { it.id == "window_${destination.x}_${destination.y}" } to false

            else -> throw IllegalArgumentException("Type DoorWindow non supporté: ${destination.class_name}")
        }

        is Point -> createTempNode(destination, "end", floorPlan, graph) to true
        else -> throw IllegalArgumentException("Type de destination non supporté")
    } ?: throw IllegalStateException("Destination introuvable dans le graphe")


    // Exécution de Dijkstra
    val path = destNode?.let {
        Dijkstra(graph.nodes, graph.edges)
            .findShortestPath(startNode.id, it.id)
    }
    Log.d("here","i'm here lol5")

    // Nettoyage des nœuds temporaires si nécessaire
    if (isTempDest) graph.nodes.remove(destNode)
    Log.d("here","i'm here lol6")
    if (path != null) {
        Log.d("here","${path}")

        return path.mapNotNull { nodeId ->
            graph.nodes.find { it.id == nodeId }?.let { Point(it.x, it.y) }
        }
    }

    return emptyList()
}

private fun createTempNode(
    point: Point,
    prefix: String,
    floorPlan: FloorPlanState,
    graph: NavigationGraph
): Node {
    val tempNode = Node("temp_${prefix}_${point.x}_${point.y}", point.x, point.y, NODE_TYPE_TEMP)
    graph.nodes.add(tempNode)

    floorPlan.rooms.polygons.find { graph.isPointInPolygon(point.x, point.y, it.coords) }?.let { room ->
        connectTempNodeToRoomElements(tempNode, room, floorPlan, graph)
    } ?: graph.connectToNearest(tempNode, floorPlan)

    return tempNode
}
private fun connectTempNodeToRoomElements(
    tempNode: Node,
    room: RoomPolygon,
    floorPlan: FloorPlanState,
    graph: NavigationGraph
) {
    // 1. Trouver le nœud de la pièce
    val roomNode = graph.nodes.find { it.type == NODE_TYPE_ROOM && it.id == "room_${room.name}" }

    // 2. Trouver tous les éléments de la pièce (POIs, fenêtres, portes)
    val roomElements = graph.nodes.filter { node ->
        when (node.type) {
            NODE_TYPE_POI, NODE_TYPE_WINDOW ->
                graph.isPointInPolygon(node.x, node.y, room.coords)
            NODE_TYPE_DOOR -> {
                // Vérifier si la porte est dans cette pièce
                floorPlan.doors.any { door ->
                    door.x == node.x && door.y == node.y &&
                            graph.isPointInPolygon(door.x, door.y, room.coords)
                }
            }
            else -> false
        }
    }

    // 3. Connecter le nœud temporaire à tous ces éléments
    roomElements.forEach { element ->
        val weight = calculateDistance(tempNode, element).toDouble()
        graph.addBidirectionalEdge(tempNode.id, element.id, weight)
    }

    // 4. Connecter au centre de la pièce si disponible
    roomNode?.let {
        val weight = calculateDistance(tempNode, it).toDouble()
        graph.addBidirectionalEdge(tempNode.id, it.id, weight)
    }
}


// fonction pour verfier si un point est dans une zone
fun isPointInZone(point: Point, zone: Zone): Boolean {
    val polygon = zone.coords
    if (polygon.size < 3) return false // Un polygone a besoin d'au moins 3 points

    var inside = false
    var i = 0
    var j = polygon.size - 1

    while (i < polygon.size) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y

        // Vérifie si le point est exactement sur un sommet
        if (point.x == xi && point.y == yi) return true

        // Vérifie si le point est sur une arête horizontale
        if (yi == yj && yi == point.y && point.x > min(xi, xj) && point.x < max(xi, xj)) {
            return true
        }

        // Vérifie si le point est sur une arête verticale
        if (xi == xj && xi == point.x && point.y > min(yi, yj) && point.y < max(yi, yj)) {
            return true
        }

        // Vérifie l'intersection avec l'arête du polygone
        val intersect = ((yi > point.y) != (yj > point.y)) &&
                (point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi)
        if (intersect) inside = !inside

        j = i++
    }

    return inside
}
