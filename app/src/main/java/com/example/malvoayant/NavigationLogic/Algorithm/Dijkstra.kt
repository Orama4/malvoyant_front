package com.example.malvoayant.NavigationLogic.Algorithm

import com.example.malvoayant.NavigationLogic.Models.Edge
import com.example.malvoayant.NavigationLogic.Models.Node
import java.util.*

class Dijkstra(nodes: List<Node>, edges: List<Edge>) {
    private val graph: Map<String, Map<String, Double>> = buildGraph(nodes, edges)
    private val distances = mutableMapOf<String, Double>()
    private val predecessors = mutableMapOf<String, String?>()

    // Construit le graphe sous forme de liste d'adjacence
    private fun buildGraph(nodes: List<Node>, edges: List<Edge>): Map<String, Map<String, Double>> {
        val adjMap = mutableMapOf<String, MutableMap<String, Double>>()
        nodes.forEach { node ->
            adjMap[node.id] = mutableMapOf()
        }
        edges.forEach { edge ->
            adjMap[edge.from]?.put(edge.to, edge.weight)
        }
        return adjMap
    }

    // Trouve le chemin le plus court entre startId et endId
    fun findShortestPath(startId: String, endId: String): List<String> {
        if (startId !in graph || endId !in graph) return emptyList()

        initializeDistances(startId)

        val priorityQueue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        priorityQueue.add(startId to 0.0)

        while (priorityQueue.isNotEmpty()) {
            val (currentNodeId, currentDist) = priorityQueue.poll()

            if (currentNodeId == endId) break

            graph[currentNodeId]?.forEach { (neighbor, weight) ->
                val newDist = currentDist + weight
                if (newDist < distances[neighbor]!!) {
                    distances[neighbor] = newDist
                    predecessors[neighbor] = currentNodeId
                    priorityQueue.add(neighbor to newDist)
                }
            }
        }

        return reconstructPath(endId)
    }

    private fun initializeDistances(startId: String) {
        graph.keys.forEach { nodeId ->
            distances[nodeId] = if (nodeId == startId) 0.0 else Double.POSITIVE_INFINITY
            predecessors[nodeId] = null
        }
    }

    private fun reconstructPath(endId: String): List<String> {
        val path = mutableListOf<String>()
        var current: String? = endId

        if (predecessors[current] == null && current != null) {
            return if (current in distances && distances[current]!! < Double.POSITIVE_INFINITY)
                listOf(current)
            else
                emptyList()
        }

        while (current != null) {
            path.add(0, current)
            current = predecessors[current]
        }

        return if (path.size > 1) path else emptyList()
    }
}