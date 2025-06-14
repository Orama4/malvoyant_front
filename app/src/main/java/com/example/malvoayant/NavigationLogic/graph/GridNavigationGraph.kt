package com.example.malvoayant.NavigationLogic.graph

import android.util.Log
import com.example.malvoayant.NavigationLogic.Models.Edge
import com.example.malvoayant.NavigationLogic.Models.Node
import com.example.malvoayant.NavigationLogic.utils.calculateDistance
import com.example.malvoayant.NavigationLogic.utils.isPointInPolygon
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.utils.NavigationUtils
import java.util.PriorityQueue
import kotlin.math.sqrt

class GridNavigationGraph(
    private val floorPlan: FloorPlanState,
    private val bounds: List<Float>,
    private val cellSize: Float = 50f // en mètres
) {
    data class Cell(val i: Int, val j: Int, val center: Point, val walkable: Boolean)

    private val cells = mutableListOf<Cell>()
    private lateinit var cellMap: Map<Pair<Int, Int>, Cell>

    val nodes = mutableListOf<Node>()
    val edges = mutableListOf<Edge>()

    fun buildGrid() {
        val minX = bounds[0]
        val maxX = bounds[1]
        val minY = bounds[2]
        val maxY = bounds[3]

        val cols = ((maxX - minX) / cellSize).toInt() + 1
        val rows = ((maxY - minY) / cellSize).toInt() + 1
        val obstacles = NavigationUtils.getDetectedObstacles()
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val cx = minX + i * cellSize
                val cy = minY + j * cellSize
                val center = Point(cx, cy)

                // 1) Hors POI
                // 1) Hors POI avec buffer sur la taille de chaque POI
                val inPoi = floorPlan.pois.any { poi ->
                    // demi-dimensions réelles du POI
                    val halfW = poi.width  / 2f
                    val halfH = poi.height / 2f
                    // tampon supplémentaire (en unités de ta grille)
                    val buffer = 1f ?: 0f

                    // test par boîte englobante
                    (cx >= poi.x - halfW - buffer && cx <= poi.x + halfW + buffer) &&
                            (cy >= poi.y - halfH - buffer && cy <= poi.y + halfH + buffer)
                }


                // 2) Hors murs (on considère chaque mur comme un segment épaissi)
                val inWall = floorPlan.walls.any { wall ->
                    // distance du point au segment [start–end]
                    val dist = distancePointToSegment(center, wall.start, wall.end)
                    dist <= wall.thickness / 2f
                }

                // 3) Hors hors-salle (si tu veux cloisonner)
                val inAnyRoom = floorPlan.rooms.polygons.any { roomPoly ->
                    isPointInPolygon(cx, cy, roomPoly.coords)
                }
                // si tu veux inter-connecter plusieurs pièces via portes, ignore inAnyRoom ici
                // et gère la connexion porte→cellule dans connectNodes()
                val inObstacle = obstacles.any { obstacle ->
                    calculateDistance(center, obstacle) <= cellSize / 2f
                }
                val walkable = !inPoi && !inWall && inAnyRoom && !inObstacle

                val cell = Cell(i, j, center, walkable)
                cells += cell
                if (walkable) nodes += Node("cell_${i}_$j", cx, cy, "grid")
            }
        }

        cellMap = cells.associateBy { Pair(it.i, it.j) }
    }

    /** Utilitaire pour mesurer la distance d’un point à un segment AB */
    fun distancePointToSegment(p: Point, a: Point, b: Point): Float {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val wx = p.x - a.x
        val wy = p.y - a.y
        val c1 = vx*wx + vy*wy
        if (c1 <= 0) return sqrt(wx*wx + wy*wy)
        val c2 = vx*vx + vy*vy
        if (c2 <= c1) {
            val dx = p.x - b.x
            val dy = p.y - b.y
            return sqrt(dx*dx + dy*dy)
        }
        val t = c1 / c2
        val px = a.x + t * vx
        val py = a.y + t * vy
        val dx = p.x - px
        val dy = p.y - py
        return sqrt(dx*dx + dy*dy)
    }

    /**
     * Récupère les voisins selon une connectivité à 8 directions
     */
    private fun getNeighbors(cell: Cell): List<Cell> {
        // 8-connexité : N, S, E, W et diagonales NE, NW, SE, SW
        val deltas = listOf(
            Pair(1, 0), Pair(-1, 0),
            Pair(0, 1), Pair(0, -1),
            Pair(1, 1), Pair(1, -1),
            Pair(-1, 1), Pair(-1, -1)
        )
        return deltas.mapNotNull { (di, dj) ->
            cellMap[Pair(cell.i + di, cell.j + dj)]
        }.filter { it.walkable }
    }

    private fun heuristic(a: Cell, b: Cell): Double =
        calculateDistance(a.center, b.center).toDouble()

    private fun reconstructPath(
        cameFrom: Map<Cell, Cell>,
        current: Cell
    ): List<Cell> {
        val path = mutableListOf(current)
        var curr = current
        while (cameFrom.containsKey(curr)) {
            curr = cameFrom[curr]!!
            path.add(curr)
        }
        return path.reversed()
    }

    private fun aStar(start: Cell, goal: Cell): List<Cell> {
        val openSet = PriorityQueue<Pair<Cell, Double>>(compareBy { it.second })
        openSet.add(Pair(start, heuristic(start, goal)))

        val cameFrom = mutableMapOf<Cell, Cell>()
        val gScore = mutableMapOf<Cell, Double>().withDefault { Double.MAX_VALUE }
        gScore[start] = 0.0

        val fScore = mutableMapOf<Cell, Double>().withDefault { Double.MAX_VALUE }
        fScore[start] = heuristic(start, goal)

        while (openSet.isNotEmpty()) {
            val (current, _) = openSet.poll()
            if (current == goal) return reconstructPath(cameFrom, current)

            for (neighbor in getNeighbors(current)) {
                val tentativeG = gScore.getValue(current) +
                        calculateDistance(current.center, neighbor.center)

                if (tentativeG < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    val f = tentativeG + heuristic(neighbor, goal)
                    fScore[neighbor] = f
                    openSet.add(Pair(neighbor, f))
                }
            }
        }
        return emptyList()
    }

    fun findPath(start: Any, goal: Any): List<Point> {
        val startPoint = when (start) {
            is Point -> start
            is POI -> Point(start.x as Float, start.y as Float)
            else -> throw IllegalArgumentException("Invalid start type ${'$'}{start.javaClass.simpleName}")
        }
        val goalPoint = when (goal) {
            is Point -> goal
            is POI -> Point(goal.x as Float, goal.y as Float)
            else -> throw IllegalArgumentException("Invalid goal type ${'$'}{goal.javaClass.simpleName}")
        }
        val startCell = cells.filter { it.walkable }
            .minByOrNull { calculateDistance(startPoint, it.center) }!!
        val goalCell = cells.filter { it.walkable }
            .minByOrNull { calculateDistance(goalPoint, it.center) }!!

        val cellPath = aStar(startCell, goalCell).toMutableList()
        //just for the last element i want it to be the goalPoint
        if (cellPath.isEmpty()) return emptyList()
        //remove the last and put goalPoint instead of it
        if (cellPath.last() == goalCell) {
            cellPath [0]= Cell(startCell.i, startCell.j, startPoint, true)

        }

        return cellPath.map { it.center }
    }

    fun simplifyPath(path: List<Point>): List<Point> {
        if (path.size <= 2) return path
        val simplified = mutableListOf(path.first())
        for (i in 1 until path.size - 1) {
            val prev = simplified.last()
            val curr = path[i]
            val next = path[i + 1]
            if ((curr.x - prev.x) * (next.y - curr.y) ==
                (next.x - curr.x) * (curr.y - prev.y)
            ) continue
            simplified += curr
        }
        simplified += path.last()
        return simplified
    }
}