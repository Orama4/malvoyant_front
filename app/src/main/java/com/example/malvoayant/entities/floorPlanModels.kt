package com.example.malvoayant.entities


import java.util.UUID

// Modèles de données pour le FloorPlan
data class Point(
    val x: Float,
    val y: Float
)


data class Wall(
    val start: Point,
    val end: Point,
    val thickness: Float = 12f,
    val wallId: Int? = null,
    val type: String = "wall" // "wall" ou "partition"
)

data class POI(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val name: String = "",
    val category: String = "default",
    val description: String = "",
    val icon: String? = null,
    val width: Float = 50f,
    val height: Float = 50f
)

data class DoorWindow(
    val x: Float,
    val y: Float,
    val class_name: String = "doorWindow",
    val family: String = "inWall",
    val type: String,
    val angle: Float = 0f,
    val angleSign: Int,
    val hinge: String = "normal",
    val size: Float = 60f,
    val thick: Float = 10f,
    val width: String = "1.00",
    val height: String = "0.17",
    val wallId: Int? = null,
    val paths: List<Any>? = null,
    val wall: WallReference? = null
)

data class WallReference(
    val wallId: Int,
    val thickness: Float = 10f,
    val type: String = "wall"
)

data class RoomVertex(
    val x: Float,
    val y: Float,
    val bypass: Int = 0,
    val segment: List<Any> = listOf(),
    val links: List<Any> = listOf(),
    val child: List<Any> = listOf()
)

data class RoomPolygon(
    val coords: List<Point>,
    val name: String = "",
    val area: Float = 0f,
    val pixelArea: Float = 0f,
    val type: String = "room",
    val color: String = "#f0daaf",
    val partitionCount: Int = 0,
    val regularCount: Int = 0,
    val edgeKeys: List<Any> = listOf(),
    val path: List<Any> = listOf(),
    val center: Point = Point(0f, 0f)
)

data class Room(
    val polygons: List<RoomPolygon> = listOf(),
    val vertex: List<RoomVertex> = listOf()
)

data class CanvasSize(
    val width: Float = 800f,
    val height: Float = 600f
)

data class Offset(
    val x: Float = 0f,
    val y: Float = 0f
)

data class FloorPlanState(
    val walls: List<Wall> = listOf(),
    val pois: List<POI> = listOf(),
    val doors: List<DoorWindow> = listOf(),
    val windows: List<DoorWindow> = listOf(),
    val zones: List<Any> = listOf(),
    val rooms: Room = Room(),
    val placedObjects: List<DoorWindow> = listOf(),
    val scale: Float = 1f,
    val offset: Offset = Offset(),
    val canvasSize: CanvasSize = CanvasSize()
)





