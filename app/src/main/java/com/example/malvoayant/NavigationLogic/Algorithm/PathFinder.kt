package com.example.malvoayant.NavigationLogic.Algorithm

import android.util.Log
import com.example.malvoayant.NavigationLogic.Models.*
import com.example.malvoayant.NavigationLogic.graph.NavigationGraph
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.NavigationLogic.utils.isPointInPolygon
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.*


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
            Dijkstra(graph.nodes, graph.edges)
                .findShortestPath(startNode.id, it.id)
        }
        Log.d("PathFinder", "Chemin trouvé: $path")

        // Nettoyage des nœuds temporaires si nécessaire
        if (isTempDest) graph.nodes.remove(destNode)
        if (path != null) {

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