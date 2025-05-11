package com.example.malvoayant.NavigationLogic.Algorithm

import android.util.Log
import com.example.malvoayant.NavigationLogic.Models.*
import com.example.malvoayant.NavigationLogic.graph.NavigationGraph
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.NavigationLogic.utils.isPointInPolygon
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.*
import kotlin.math.abs


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
            else -> throw IllegalArgumentException("Type de départ non supporté")
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

        // Étape 1: Créer un chemin orthogonal strict
        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]

            // Ajouter un point intermédiaire pour forcer l'angle droit
            if (current.x != next.x && current.y != next.y) {
                // Choix basé sur la plus grande différence
                if (abs(current.x - next.x) > abs(current.y - next.y)) {
                    orthogonalPath.add(Point(next.x, current.y))
                } else {
                    orthogonalPath.add(Point(current.x, next.y))
                }
            }
            orthogonalPath.add(next)
        }

        // Étape 2: Simplifier le chemin
        val simplifiedPath = simplifyPath(orthogonalPath)

        // Étape 3: Arrondir tous les angles
        return addRoundedCorners(simplifiedPath)
    }

    private fun simplifyPath(path: List<Point>): List<Point> {
        val simplified = mutableListOf<Point>()
        var lastDirection: Direction? = null

        for (i in 0 until path.size - 1) {
            val current = path[i]
            val next = path[i + 1]

            val newDirection = when {
                current.x == next.x -> Direction.VERTICAL
                current.y == next.y -> Direction.HORIZONTAL
                else -> null
            }

            if (newDirection != lastDirection) {
                simplified.add(current)
                lastDirection = newDirection
            }
        }
        simplified.add(path.last())
        return simplified
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

        floorPlan.rooms.polygons.find { isPointInPolygon(point.x, point.y, it.coords) }?.let { room ->
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



}