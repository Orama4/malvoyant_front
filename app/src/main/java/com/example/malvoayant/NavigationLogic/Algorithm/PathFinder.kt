package com.example.malvoayant.NavigationLogic.Algorithm

import android.util.Log
import com.example.malvoayant.NavigationLogic.Models.*
import com.example.malvoayant.NavigationLogic.graph.NavigationGraph
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.NavigationLogic.utils.isPointInPolygon
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.*
import com.example.malvoayant.exceptions.PathfindingException
import kotlin.math.abs
import kotlin.math.hypot


class PathFinder {
    //appeler djikstra
    fun findPath(
        start: Any, // Accepte Point ou POI
        destination: Any, // Accepte Point, POI, Door ou Window
        floorPlan: FloorPlanState
    ): List<Point> {
        Log.d("PathFinder", "Calcul du chemin de $start à $destination ")
        val graph = NavigationGraph().apply {
            addNavigableElements(floorPlan)
            connectNodes(floorPlan)
            applyRiskPenalties(floorPlan)
        }
        Log.d("PathFinder", "Graphe créé avec ${graph.nodes} nœuds et ${graph.edges} arêtes")
        // Gestion du point de départ
        val startNode = when (start) {
            is POI -> graph.nodes.find { it.id == "poi_${start.name}" }
                ?: throw IllegalArgumentException("POI de départ introuvable")

            is Point -> createTempNode(start, "start", floorPlan, graph)
            else -> throw PathfindingException("Type de départ non supporté: ${start.javaClass.simpleName}")
        }
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
            // log des edges
            Log.d("PathFinder", "Edges: ${graph.edges}")
            Dijkstra(graph.nodes, graph.edges)
                .findShortestPath(startNode.id, it.id)
        }
        Log.d("PathFinder", "Chemin trouvé: $path")

        // Nettoyage des nœuds temporaires si nécessaire
        if (isTempDest) graph.nodes.remove(destNode)
        if (path != null) {
            return smoothPathWithCorners(path, graph)
            /*return path.mapNotNull { nodeId ->
                graph.nodes.find { it.id == nodeId }?.let { Point(it.x, it.y) }
            }*/

        }

        return emptyList()
    }


    private fun smoothPathWithCorners(path: List<Point>, graph: NavigationGraph): List<Point> {
        if (path.size < 2) return path
        val orthogonalPath = mutableListOf<Point>()
        orthogonalPath.add(path[0])

        val threshold = 6f // Utiliser un float pour la cohérence avec isPointOnSegment

        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]

            // Vérifier si le segment est orthogonal (même x ou même y)
            if (abs(current.x - next.x) <= threshold || abs(current.y - next.y) <= threshold) {
                val obstacles = findObstaclesOnSegment(current, next, graph, threshold)
                Log.d("PathFinder", "Obstacles: $obstacles")

                if (obstacles.isNotEmpty()) {
                    // Contourner chaque obstacle sur le segment orthogonal
                    val detourPoints = mutableListOf<Point>()
                    var lastPoint = current

                    for (obstacle in obstacles) {
                        val obstaclePoint = Point(obstacle.x, obstacle.y)
                        val contourPoints = createContourPoints(lastPoint, obstaclePoint, next, threshold)
                        detourPoints.addAll(contourPoints)
                        lastPoint = contourPoints.last()
                    }

                    // Ajouter le segment final après le dernier obstacle
                    if (isSegmentClear(lastPoint, next, graph, threshold.toString())) {
                        detourPoints.add(next)
                    }

                    orthogonalPath.addAll(detourPoints)
                } else {
                    // Aucun obstacle - ajouter directement
                    orthogonalPath.add(next)
                }
            } else {
                // Segment diagonal - essayer les points intermédiaires
                val horizontalIntermediate = Point(next.x, current.y)
                val verticalIntermediate = Point(current.x, next.y)
                var addedIntermediate = false

                // Essayer le point horizontal
                if (!isPointOnObstacle(horizontalIntermediate, graph, threshold)) {
                    val obstaclesToHorizontal = findObstaclesOnSegment(current, horizontalIntermediate, graph, threshold)
                    val obstaclesToNext = findObstaclesOnSegment(horizontalIntermediate, next, graph, threshold)

                    if (obstaclesToHorizontal.isEmpty() && obstaclesToNext.isEmpty()) {
                        orthogonalPath.add(horizontalIntermediate)
                        orthogonalPath.add(next)
                        addedIntermediate = true
                    } else {
                        // Contourner les obstacles sur les segments
                        orthogonalPath.addAll(handleObstaclesOnDiagonal(
                            current,
                            horizontalIntermediate,
                            next,
                            obstaclesToHorizontal + obstaclesToNext,
                            graph,
                            threshold
                        ))
                        addedIntermediate = true
                    }
                }

                // Essayer le point vertical si horizontal échoue
                if (!addedIntermediate && !isPointOnObstacle(verticalIntermediate, graph, threshold)) {
                    val obstaclesToVertical = findObstaclesOnSegment(current, verticalIntermediate, graph, threshold)
                    val obstaclesToNext = findObstaclesOnSegment(verticalIntermediate, next, graph, threshold)

                    if (obstaclesToVertical.isEmpty() && obstaclesToNext.isEmpty()) {
                        orthogonalPath.add(verticalIntermediate)
                        orthogonalPath.add(next)
                        addedIntermediate = true
                    } else {
                        orthogonalPath.addAll(handleObstaclesOnDiagonal(
                            current,
                            verticalIntermediate,
                            next,
                            obstaclesToVertical + obstaclesToNext,
                            graph,
                            threshold
                        ))
                        addedIntermediate = true
                    }
                }

                // Si les deux échouent, chercher un point médian
                if (!addedIntermediate) {
                    val midPoint = Point((current.x + next.x) / 2, (current.y + next.y) / 2)
                    orthogonalPath.addAll(handleMidPoint(current, midPoint, next, graph, threshold))
                }
            }
        }

        return simplifyPath(orthogonalPath)
    }

//--- Fonctions auxiliaires ---//

    private fun findObstaclesOnSegment(start: Point, end: Point, graph: NavigationGraph, threshold: Float): List<Node> {
        return graph.nodes.filter { node ->
            node.type in listOf(NODE_TYPE_POI, NODE_TYPE_DOOR, NODE_TYPE_WINDOW) &&
                    !(abs(node.x - start.x) <= threshold && abs(node.y - start.y) <= threshold) &&
                    !(abs(node.x - end.x) <= threshold && abs(node.y - end.y) <= threshold) &&
                    isPointOnSegment(node.x,node.y, start.x,start.y, end.x,end.y, threshold)
        }
    }

    private fun isPointOnObstacle(point: Point, graph: NavigationGraph, threshold: Float): Boolean {
        return graph.nodes.any {
            it.type in listOf(NODE_TYPE_POI, NODE_TYPE_DOOR, NODE_TYPE_WINDOW) &&
                    abs(it.x - point.x) <= threshold &&
                    abs(it.y - point.y) <= threshold
        }
    }

    private fun createContourPoints(
        start: Point,
        obstacle: Point,
        end: Point,
        threshold: Float
    ): List<Point> {
        val contourDistance =50 // Distance de contournement
        val points = mutableListOf<Point>()

        // Déterminer la direction principale du segment
        val isHorizontal = abs(start.y - end.y) < threshold

        // Calculer les points de contournement
        if (isHorizontal) {
            // Contournement vertical
            Log.d("PathFinder", "Contournement vertical")
            val direction = if (obstacle.y > start.y) -1 else 1
            val directionX = if (obstacle.x > start.x) 1 else -1
            points.add(Point(obstacle.x - contourDistance*directionX, start.y))
            points.add(Point(obstacle.x - contourDistance*directionX, obstacle.y + direction * contourDistance))
            points.add(Point(obstacle.x + contourDistance*directionX, obstacle.y + direction * contourDistance))
            points.add(Point(obstacle.x + contourDistance*directionX, obstacle.y))
        } else {
            // Contournement horizontal
            Log.d("PathFinder", "Contournement horizontal")
            val direction = if (obstacle.x > start.x) -1 else 1
            val directionY = if (obstacle.y > start.y) 1 else -1
            points.add(Point(start.x, obstacle.y - contourDistance*directionY))
            points.add(Point(obstacle.x + direction * contourDistance, obstacle.y - contourDistance*directionY))
            points.add(Point(obstacle.x + direction * contourDistance, obstacle.y + contourDistance*directionY))
            points.add(Point(obstacle.x, obstacle.y + contourDistance*directionY))
        }
        Log.d("PathFinder", "Points de contournement créés: $points")

        return points
    }

    private fun handleObstaclesOnDiagonal(
        start: Point,
        intermediate: Point,
        end: Point,
        obstacles: List<Node>,
        graph: NavigationGraph,
        threshold: Float
    ): List<Point> {
        val points = mutableListOf<Point>()
        var lastPoint = start

        // Traiter le segment jusqu'au point intermédiaire
        val obstaclesToIntermediate = obstacles.filter {
            isPointOnSegment(it.x,it.y, start.x,start.y, intermediate.x,intermediate.y, threshold)
        }

        for (obstacle in obstaclesToIntermediate) {
            val contourPoints = createContourPoints(lastPoint, Point(obstacle.x, obstacle.y), intermediate, threshold)
            points.addAll(contourPoints)
            lastPoint = contourPoints.last()
        }

        points.add(intermediate)

        // Traiter le segment du point intermédiaire à la fin
        val obstaclesToEnd = obstacles.filter {
            isPointOnSegment(it.x,it.y, intermediate.x,intermediate.y, end.x,end.y, threshold)
        }

        lastPoint = intermediate
        for (obstacle in obstaclesToEnd) {
            val contourPoints = createContourPoints(lastPoint, Point(obstacle.x, obstacle.y), end, threshold)
            points.addAll(contourPoints)
            lastPoint = contourPoints.last()
        }

        points.add(end)
        return points
    }

    private fun handleMidPoint(
        start: Point,
        midPoint: Point,
        end: Point,
        graph: NavigationGraph,
        threshold: Float
    ): List<Point> {
        return if (!isPointOnObstacle(midPoint, graph, threshold)) {
            listOf(midPoint, end)
        } else {
            // Contourner le point médian si nécessaire
            val contourPoints = createContourPoints(start, midPoint, end, threshold)
            contourPoints + end
        }
    }

    private fun findDetour(start: Point, end: Point, graph: NavigationGraph): List<Point> {
        // Try to find the nearest navigable node that can bypass the obstacle
        val midX = (start.x + end.x) / 2
        val midY = (start.y + end.y) / 2
        val potentialNodes = graph.nodes.filter { node ->
            node.type == NODE_TYPE_ROOM
        }.sortedBy { calculateDistance(Point(it.x,it.y), Point(midX, midY)) }

        potentialNodes.forEach { node ->
            val detourPoint = Point(node.x, node.y)
            if (isSegmentClear(start, detourPoint, graph,"end") &&
                isSegmentClear(detourPoint, end, graph,"start")) {
                return listOf(detourPoint)
            }
        }

        // Fallback: Create a small offset around the obstacle
        return listOf(
            Point(start.x + 50, start.y), // Right
            Point(start.x + 50, end.y),   // Down
            end
        )
    }
    private fun simplifyPath(path: List<Point>): List<Point> {
        val simplified = mutableListOf<Point>()
        var lastDirection: Direction? = null

        path.forEachIndexed { i, current ->
            if (i == 0 || i == path.lastIndex) {
                simplified.add(current)
                return@forEachIndexed
            }

            val prev = path[i-1]
            val next = path[i+1]
            val newDirection = getDirection(prev, current, next)

            if (newDirection != lastDirection) {
                simplified.add(current)
                lastDirection = newDirection
            }
        }

        return simplified
    }

    private fun getDirection(prev: Point, current: Point, next: Point): Direction? {
        return when {
            prev.x == current.x && current.y == next.y -> Direction.VERTICAL
            prev.y == current.y && current.x == next.x -> Direction.HORIZONTAL
            else -> null
        }
    }

    private fun addRoundedCorners(path: List<Point>): List<Point> {
        if (path.size < 3) return path

        val rounded = mutableListOf<Point>()
        rounded.add(path[0])

        for (i in 1 until path.size - 1) {
            val prev = path[i - 1]
            val curr = path[i]
            val next = path[i + 1]

            // Détection de tout changement de direction
            if ((prev.x == curr.x && curr.y == next.y) ||
                (prev.y == curr.y && curr.x == next.x)) {

                // Ajouter 5 points de courbe
                val steps = 5
                for (t in 0..steps) {
                    val ratio = t.toFloat() / steps
                    val x = when {
                        prev.x == curr.x -> curr.x
                        else -> prev.x + (next.x - prev.x) * ratio
                    }
                    val y = when {
                        prev.y == curr.y -> curr.y
                        else -> prev.y + (next.y - prev.y) * ratio
                    }
                    rounded.add(Point(x, y))
                }
            } else {
                rounded.add(curr)
            }
        }

        rounded.add(path.last())
        return rounded.distinct()
    }

    private enum class Direction {
        VERTICAL, HORIZONTAL
    }
    private fun shouldAddCorner(current: Point, next: Point, graph: NavigationGraph): Boolean {
        // Vérifie les obstacles dans les deux directions
        val xRange = minOf(current.x, next.x)..maxOf(current.x, next.x)
        val yRange = minOf(current.y, next.y)..maxOf(current.y, next.y)

        val hasXObstacle = graph.nodes.any { it.y == current.y && it.x in xRange }
        val hasYObstacle = graph.nodes.any { it.x == next.x && it.y in yRange }

        return !hasXObstacle && !hasYObstacle
    }

/*    private fun addRoundedCorners(path: List<Point>): List<Point> {
        if (path.size < 3) return path

        val roundedPath = mutableListOf<Point>()
        roundedPath.add(path[0])

        for (i in 1 until path.size - 1) {
            val prev = path[i-1]
            val curr = path[i]
            val next = path[i+1]

            // Détection des angles droits (votre cas)
            if ((prev.x == curr.x && curr.y == next.y) ||
                (prev.y == curr.y && curr.x == next.x)) {

                // Ajout de points pour créer un arrondi
                val steps = 5
                for (t in 1 until steps) {
                    val ratio = t.toFloat() / steps
                    val x = when {
                        prev.x == curr.x -> curr.x
                        else -> lerp(prev.x, next.x, ratio)
                    }
                    val y = when {
                        prev.y == curr.y -> curr.y
                        else -> lerp(prev.y, next.y, ratio)
                    }
                    roundedPath.add(Point(x, y))
                }
            } else {
                roundedPath.add(curr)
            }
        }

        roundedPath.add(path.last())
        return roundedPath
    }*/

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t






    private fun createTempNode(
        point: Point,
        prefix: String,
        floorPlan: FloorPlanState,
        graph: NavigationGraph
    ): Node {
        val tempNode = Node("temp_${prefix}_${point.x}_${point.y}", point.x, point.y, NODE_TYPE_TEMP)
        graph.nodes.add(tempNode)

        val otherRooms = floorPlan.rooms.polygons.filter { it.name != "Room 1" }

        val targetRoom = if (otherRooms.isNotEmpty()) {
            // Il existe au moins une autre salle que "Room 1"
            otherRooms.find { isPointInPolygon(point.x, point.y, it.coords) }
        } else {
            // Sinon, on utilise "Room 1"
            floorPlan.rooms.polygons.find { it.name == "Room 1" && isPointInPolygon(point.x, point.y, it.coords) }
        }

        targetRoom?.let { room ->
            Log.d("connect", "Connecting to room: ${room.name}")
            connectTempNodeToRoomElements(tempNode, room, floorPlan, graph)
        } ?: run {
            Log.d("connect", "Falling back to nearest connection")
            graph.connectToNearest(tempNode, floorPlan)
        }

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
        Log.d("bro here","room_${room.name}")

        // 2. Trouver tous les éléments de la pièce (POIs, fenêtres, portes)
        val roomElements = graph.nodes.filter { node ->
            when (node.type) {
                NODE_TYPE_POI, NODE_TYPE_WINDOW ->
                    isPointInPolygon(node.x, node.y, room.coords)
                NODE_TYPE_DOOR -> {
                    // Vérifier si la porte est dans cette pièce
                    floorPlan.doors.any { door ->
                        door.x == node.x && door.y == node.y &&
                                isPointInPolygon(door.x, door.y, room.coords)
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
    private fun isSegmentClear(start: Point, end: Point, graph: NavigationGraph,type:String): Boolean {
        val temp:Point;
        if (type=="end"){
            temp=start
        }else{
            temp=end
        }
        val obstacles = graph.nodes.filter {
            it.type in listOf(NODE_TYPE_POI, NODE_TYPE_DOOR, NODE_TYPE_WINDOW) &&
                    !(it.x == end.x && it.y == end.y)
                    && !(it.x == start.x && it.y == start.y)
        }
        Log.d("PathFinder", "Obstacles: $obstacles")
        return obstacles.none { obstacle ->
            isPointOnSegment(obstacle.x, obstacle.y, start.x, start.y, end.x, end.y)
        }
    }


    private fun isPointOnSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        threshold: Float = 1.0f // tu peux ajuster cette valeur
    ): Boolean {
        val withinX = px in (minOf(x1, x2) - threshold)..(maxOf(x1, x2) + threshold)
        val withinY = py in (minOf(y1, y2) - threshold)..(maxOf(y1, y2) + threshold)

        if (!withinX || !withinY) return false

        val crossProduct = (py - y1) * (x2 - x1) - (px - x1) * (y2 - y1)
        val distance = abs(crossProduct) / hypot((x2 - x1).toDouble(), (y2 - y1).toDouble())

        val isOnSegment = distance <= threshold
        if (isOnSegment) {
            Log.d("PathFinder", "Point ($px, $py) is within threshold $threshold of segment from ($x1, $y1) to ($x2, $y2)")
        }
        return isOnSegment
    }


//    private fun smoothPathWithCorners(path: List<Point>, graph: NavigationGraph): List<Point> {
//        if (path.size < 2) return path
//        val orthogonalPath = mutableListOf<Point>()
//        orthogonalPath.add(path[0])
//
//        for (i in 0 until path.size - 1) {
//            val current = path[i]
//            val next = path[i + 1]
//
//            val threshold = 5
//            if (abs(current.x - next.x) > threshold && abs(current.y - next.y) > threshold) {
//                Log.d("PathFinder", "Segmentttt bloqué: $current -> $next")
//                val horizontalIntermediate = Point(next.x, current.y)
//                val verticalIntermediate = Point(current.x, next.y)
//                //verifying if horizontalIntermediate is not a pre existing node
//                var horiz=true;
//                var vert=true;
//                if (!graph.nodes.any {
//                        abs(it.x - horizontalIntermediate.x) <= threshold &&
//                                abs(it.y - horizontalIntermediate.y) <= threshold
//                    } ) {
//
//                    val horizontalClear = isSegmentClear(current, horizontalIntermediate, graph,"end") &&
//                            isSegmentClear(horizontalIntermediate, next, graph,"start")
//                    if (horizontalClear) {
//                        orthogonalPath.add(horizontalIntermediate)
//                    } else{
//                        horiz=false;
//                    }
//                }
//
//                if (!graph.nodes.any {
//                        abs(it.x - verticalIntermediate.x) <= threshold &&
//                                abs(it.y - verticalIntermediate.y) <= threshold
//                    } && !horiz) {
//
//                    val verticalClear =
//                        isSegmentClear(current, verticalIntermediate, graph, "end") &&
//                                isSegmentClear(verticalIntermediate, next, graph, "start")
//                    if (verticalClear) {
//                        Log.d("PathFinder", "Segment bloqué: $current -> $verticalIntermediate")
//                        orthogonalPath.add(verticalIntermediate)
//                    } else {
//                        vert = false;
//                    }
//                }
//                if( !horiz && !vert) {
//                    Log.d("PathFinder", "Segment bloqué: $current -> $next")
//                    // Si les deux segments sont bloqués, on cherche un détour
//                    val detour = findDetour(current, next, graph)
//                    orthogonalPath.addAll(detour)
//                }
//
//            }
//            orthogonalPath.add(next)
//        }
//
//        return simplifyPath(orthogonalPath)
//    }

}