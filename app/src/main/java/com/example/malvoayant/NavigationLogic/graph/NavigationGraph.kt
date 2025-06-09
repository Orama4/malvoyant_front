package com.example.malvoayant.NavigationLogic.graph

import com.example.malvoayant.NavigationLogic.Models.Edge
import com.example.malvoayant.NavigationLogic.Models.*
import com.example.malvoayant.NavigationLogic.Models.Node
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.NavigationLogic.utils.isLineIntersectingPolygon
import com.example.malvoayant.NavigationLogic.utils.isPointInPolygon
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.models.Zone


class NavigationGraph {
    val nodes = mutableListOf<Node>()
    val edges = mutableListOf<Edge>()
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


        val rooms = floorPlan.rooms.polygons

// Vérifie s'il y a plus d'une salle
        if (rooms.size > 1) {
            rooms.forEach { room ->
                if (room.name != "Room 1") {
                    val center = room.center
                    nodes.add(Node("room_${room.name}", center.x, center.y, "room"))
                }
            }
        } else {
            // Il n'y a qu'une seule salle, inclure même si c'est "Room 1"
            rooms.forEach { room ->
                val center = room.center
                nodes.add(Node("room_${room.name}", center.x, center.y, "room"))
            }
        }


        // Ajouter les centroïdes des zones comme nœuds
        floorPlan.zones.forEach { zone ->
            val center = zone.center // Utilise le centre prédéfini ou calcule-le
            nodes.add(Node("zone${zone.name}", center.x, center.y, "zone"))
        }
    }

    // Pénaliser les zones dangereuses (ex: poids * 10 si arête traverse une zone à risque)
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

}
