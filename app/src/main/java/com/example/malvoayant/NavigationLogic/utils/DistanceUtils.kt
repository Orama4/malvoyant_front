package com.example.malvoayant.NavigationLogic.utils

import com.example.malvoayant.NavigationLogic.Models.Node
import com.example.malvoayant.data.models.Point
import kotlin.math.pow
import kotlin.math.sqrt


fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Double {
    return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2).toDouble())
}
// Calcule la distance euclidienne entre deux points
fun calculateDistance(a: Point, b: Point): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}

// Calcule la distance entre deux nœuds
fun calculateDistance(a: Node, b: Node): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}
