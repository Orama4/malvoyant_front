package com.example.malvoayant.NavigationLogic.utils

import com.example.malvoayant.NavigationLogic.Models.DOOR_PROXIMITY_THRESHOLD
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.models.RoomPolygon
import com.example.malvoayant.data.models.Zone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
fun findAdjacentRooms(x: Float, y: Float, rooms: List<RoomPolygon>): List<RoomPolygon> {
    return rooms.filter { room ->
        room.coords.any { point -> distance(point.x, point.y, x, y) < DOOR_PROXIMITY_THRESHOLD }
    }
}

fun calculatePolygonCenter(points: List<Point>): Point {
    var centroidX = 0f
    var centroidY = 0f
    val n = points.size

    points.forEach { point ->
        centroidX += point.x
        centroidY += point.y
    }

    return Point(centroidX / n, centroidY / n)
}

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


fun lineIntersectsLine(
    x1: Float, y1: Float, x2: Float, y2: Float,
    wallX1: Float, wallY1: Float, wallX2: Float, wallY2: Float,
    thickness: Float = 12f
): Boolean {
    // Convert wall to "thick" rectangle coordinates
    val halfThickness = thickness / 2
    val angle = atan2(wallY2 - wallY1, wallX2 - wallX1)
    val dx = halfThickness * sin(angle).toFloat()
    val dy = halfThickness * cos(angle).toFloat()

    // Create rectangle vertices for the thick wall
    val rect = listOf(
        Point(wallX1 - dx, wallY1 + dy),
        Point(wallX1 + dx, wallY1 - dy),
        Point(wallX2 + dx, wallY2 - dy),
        Point(wallX2 - dx, wallY2 + dy)
    )

    // Check if path segment intersects with the wall rectangle
    return lineIntersectsPolygon(x1, y1, x2, y2, rect)
}

private fun lineIntersectsPolygon(
    x1: Float, y1: Float, x2: Float, y2: Float,
    polygon: List<Point>
): Boolean {
    // Check against all polygon edges
    for (i in polygon.indices) {
        val j = (i + 1) % polygon.size
        if (lineSegmentsIntersect(
                x1, y1, x2, y2,
                polygon[i].x, polygon[i].y,
                polygon[j].x, polygon[j].y
            )
        ) {
            return true
        }
    }
    return false
}

private fun lineSegmentsIntersect(
    x1: Float, y1: Float, x2: Float, y2: Float,
    x3: Float, y3: Float, x4: Float, y4: Float
): Boolean {
    // Calculate orientation values
    fun orientation(px: Float, py: Float, qx: Float, qy: Float, rx: Float, ry: Float): Int {
        val value = (qy - py) * (rx - qx) - (qx - px) * (ry - qy)
        return when {
            value > 0f -> 1   // Clockwise
            value < 0f -> 2   // Counter-clockwise
            else -> 0         // Collinear
        }
    }

    val o1 = orientation(x1, y1, x2, y2, x3, y3)
    val o2 = orientation(x1, y1, x2, y2, x4, y4)
    val o3 = orientation(x3, y3, x4, y4, x1, y1)
    val o4 = orientation(x3, y3, x4, y4, x2, y2)

    // General case of non-collinear segments
    if (o1 != o2 && o3 != o4) return true

    // Special cases for collinear segments
    if (o1 == 0 && isOnSegment(x1, y1, x3, y3, x2, y2)) return true
    if (o2 == 0 && isOnSegment(x1, y1, x4, y4, x2, y2)) return true
    if (o3 == 0 && isOnSegment(x3, y3, x1, y1, x4, y4)) return true
    if (o4 == 0 && isOnSegment(x3, y3, x2, y2, x4, y4)) return true

    return false
}

private fun isOnSegment(
    px: Float, py: Float,
    qx: Float, qy: Float,
    rx: Float, ry: Float
): Boolean {
    return (qx <= maxOf(px, rx) && qx >= minOf(px, rx) &&
            qy <= maxOf(py, ry) && qy >= minOf(py, ry))
}