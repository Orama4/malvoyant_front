package com.example.malvoayant.data.viewmodels


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.data.models.CanvasSize
import com.example.malvoayant.data.models.DoorWindow
import com.example.malvoayant.data.models.FloorPlanState
import com.example.malvoayant.data.models.Offset
import com.example.malvoayant.data.models.POI
import com.example.malvoayant.data.models.Point
import com.example.malvoayant.data.models.Room
import com.example.malvoayant.data.models.RoomPolygon
import com.example.malvoayant.data.models.RoomVertex
import com.example.malvoayant.data.models.Wall
import com.example.malvoayant.data.models.WallReference
import com.example.malvoayant.data.models.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt


class FloorPlanViewModel : ViewModel() {

    var floorPlanState by mutableStateOf(FloorPlanState())
        private set

    fun loadGeoJSONFromAssets(context: Context) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream = context.assets.open("file.geojson")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                importFromGeoJSON(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateMinPoint(walls: List<Wall>, pois: List<POI>, doors: List<DoorWindow>, windows: List<DoorWindow>): Point {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE

        fun updateMinPoint(point: Point) {
            if (point.x < minX) minX = point.x
            if (point.y < minY) minY = point.y
        }

        walls.forEach { wall ->
            updateMinPoint(wall.start)
            updateMinPoint(wall.end)
        }

        pois.forEach { poi ->
            updateMinPoint(Point(poi.x, poi.y))
        }

        doors.forEach { door ->
            updateMinPoint(Point(door.x, door.y))
        }

        windows.forEach { window ->
            updateMinPoint(Point(window.x, window.y))
        }

        return Point(minX, minY)
    }

    fun setWalls(walls: List<Wall>) {
        floorPlanState = floorPlanState.copy(walls = walls)
    }

    fun setPois(pois: List<POI>) {
        floorPlanState = floorPlanState.copy(pois = pois)
    }

    fun setDoors(doors: List<DoorWindow>) {
        floorPlanState = floorPlanState.copy(doors = doors)
    }

    fun setWindows(windows: List<DoorWindow>) {
        floorPlanState = floorPlanState.copy(windows = windows)
    }

    fun setZones(zones: List<Zone>) {
        Log.d("am in setting zones ","am in setting zones ")
        zones.forEach { zone ->
           Log.d("a zone","${zone.name}")
        }
        floorPlanState = floorPlanState.copy(zones = zones)
    }

    fun setRooms(rooms: Room) {
        floorPlanState = floorPlanState.copy(rooms = rooms)
    }

    fun setPlacedObjects(objects: List<DoorWindow>) {
        floorPlanState = floorPlanState.copy(placedObjects = objects)
    }

    fun setScale(scale: Float) {
        floorPlanState = floorPlanState.copy(scale = scale)
    }

    fun setOffset(offset: Offset) {
        floorPlanState = floorPlanState.copy(offset = offset)
    }

    fun setCanvasSize(canvasSize: CanvasSize) {
        floorPlanState = floorPlanState.copy(canvasSize = canvasSize)
    }

    private fun calculateLength(start: Point, end: Point): Float {
        return sqrt((end.x - start.x).pow(2) + (end.y - start.y).pow(2))
    }

    fun importFromGeoJSON(jsonString: String) {
        viewModelScope.launch {
            try {
                val geoJSONData = withContext(Dispatchers.Default) {
                    JSONObject(jsonString)
                }

                if (!geoJSONData.has("features") || !geoJSONData.has("type") || geoJSONData.getString("type") != "FeatureCollection") {
                    return@launch
                }

                val features = geoJSONData.getJSONArray("features")

                val newWalls = mutableListOf<Wall>()
                val newPOIs = mutableListOf<POI>()
                val newDoors = mutableListOf<DoorWindow>()
                val newWindows = mutableListOf<DoorWindow>()
                val newZones = mutableListOf<Zone>()
                val newRoomPolygons = mutableListOf<RoomPolygon>()
                val newRoomVertices = mutableListOf<RoomVertex>()
                val newPlacedObjects = mutableListOf<DoorWindow>()

                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)

                    if (!feature.has("properties") || !feature.has("geometry")) {
                        continue
                    }

                    val properties = feature.getJSONObject("properties")
                    val geometry = feature.getJSONObject("geometry")

                    if (!properties.has("class_name")) {
                        continue
                    }

                    val className = properties.getString("class_name")

                    when {
                        className == "wall" && geometry.getString("type") == "LineString" -> {
                            val coords = geometry.getJSONArray("coordinates")
                            if (coords.length() < 2) continue

                            val start = coords.getJSONArray(0)
                            val end = coords.getJSONArray(1)

                            val wall = Wall(
                                start = Point(start.getDouble(0).toFloat(), start.getDouble(1).toFloat()),
                                end = Point(end.getDouble(0).toFloat(), end.getDouble(1).toFloat()),
                                thickness = properties.optDouble("thickness", 12.0).toFloat(),
                                wallId = properties.optInt("wallId", newWalls.size),
                                type = properties.optString("type", "wall")
                            )

                            newWalls.add(wall)
                        }

                        className == "poi" && geometry.getString("type") == "Point" -> {
                            val coords = geometry.getJSONArray("coordinates")
                            if (coords.length() < 2) continue

                            val x = coords.getDouble(0).toFloat()
                            val y = coords.getDouble(1).toFloat()

                            val poi = POI(
                                id = properties.optString("original_id", UUID.randomUUID().toString()),
                                x = x,
                                y = y,
                                name = properties.optString("name", "POI ${newPOIs.size + 1}"),
                                category = properties.optString("type", "default"),
                                description = properties.optString("description", ""),
                                icon = if (properties.has("icon") && !properties.isNull("icon")) properties.getString("icon") else null,
                                width = properties.optDouble("width", 50.0).toFloat(),
                                height = properties.optDouble("height", 50.0).toFloat()
                            )

                            newPOIs.add(poi)
                        }

                        className == "zone" && geometry.getString("type") == "Polygon" -> {
                            val coordsArray = geometry.getJSONArray("coordinates").getJSONArray(0)
                            val points = mutableListOf<Point>()

                            for (j in 0 until coordsArray.length()) {
                                val coord = coordsArray.getJSONArray(j)
                                points.add(Point(coord.getDouble(0).toFloat(), coord.getDouble(1).toFloat()))
                            }

                            val center = if (properties.has("centerX") && properties.has("centerY")) {
                                Point(properties.getDouble("centerX").toFloat(), properties.getDouble("centerY").toFloat())
                            } else {
                                Point(0f, 0f)
                            }

                            val zone = Zone(
                                coords = points,
                                name = properties.optString("name", "Zone ${newZones.size + 1}"),
                                zone_type = properties.optString("zone_type", "default"),
                                type = properties.optString("type", "rectangle"),
                                fill = properties.optString("fill", "rgba(255, 111, 0, 0.4)"),
                                stroke = properties.optString("stroke", "#4B9CD3"),
                                strokeWidth = properties.optDouble("strokeWidth", 2.0).toFloat(),
                                center = center
                            )

                            newZones.add(zone)
                        }
                        (className == "door" || className == "window") && geometry.getString("type") == "Point" -> {
                            val coords = geometry.getJSONArray("coordinates")
                            if (coords.length() < 2) continue

                            val x = coords.getDouble(0).toFloat()
                            val y = coords.getDouble(1).toFloat()

                            val isDoor = className == "door"

                            var wallReference: WallReference? = null
                            if (properties.has("wall_info")) {
                                val wallInfo = JSONObject(properties.getString("wall_info"))
                                wallReference = WallReference(
                                    wallId = wallInfo.optInt("wallId"),
                                    thickness = wallInfo.optDouble("thickness", 10.0).toFloat(),
                                    type = wallInfo.optString("type", "wall")
                                )
                            }

                            val doorWindow = DoorWindow(
                                x = x,
                                y = y,
                                type = properties.optString("type", if (isDoor) "doorSingle" else "windowSingle"),
                                family = properties.optString("family", "inWall"),
                                angle = properties.optDouble("angle", 0.0).toFloat(),
                                angleSign = properties.optInt("angleSign", if (isDoor) 1 else 0),
                                hinge = properties.optString("hinge", "normal"),
                                size = properties.optDouble("size", 60.0).toFloat(),
                                thick = properties.optDouble("thick", 10.0).toFloat(),
                                width = properties.optString("width", "1.00"),
                                height = properties.optString("height", "0.17"),
                                wallId = if (properties.has("wallId") && !properties.isNull("wallId")) properties.getInt("wallId") else null,
                                wall = wallReference
                            )

                            if (isDoor) {
                                newDoors.add(doorWindow)
                            } else {
                                newWindows.add(doorWindow)
                            }

                            newPlacedObjects.add(doorWindow)
                        }

                        className == "room_polygon" && geometry.getString("type") == "Polygon" -> {
                            val coordsArray = geometry.getJSONArray("coordinates").getJSONArray(0)
                            val points = mutableListOf<Point>()

                            for (j in 0 until coordsArray.length()) {
                                val coord = coordsArray.getJSONArray(j)
                                points.add(Point(coord.getDouble(0).toFloat(), coord.getDouble(1).toFloat()))
                            }

                            val center = if (properties.has("center")) {
                                val centerObj = JSONObject(properties.getString("center"))
                                Point(centerObj.optDouble("x", 0.0).toFloat(), centerObj.optDouble("y", 0.0).toFloat())
                            } else {
                                Point(0f, 0f)
                            }

                            val roomPolygon = RoomPolygon(
                                coords = points,
                                name = properties.optString("name", "Room ${newRoomPolygons.size + 1}"),
                                area = properties.optDouble("area", 0.0).toFloat(),
                                pixelArea = properties.optDouble("pixelArea", 0.0).toFloat(),
                                type = properties.optString("type", "room"),
                                color = properties.optString("color", "#f0daaf"),
                                partitionCount = properties.optInt("partitionCount", 0),
                                regularCount = properties.optInt("regularCount", 0),
                                center = center
                            )

                            newRoomPolygons.add(roomPolygon)
                        }

                        className == "room_vertex" && geometry.getString("type") == "Point" -> {
                            val coords = geometry.getJSONArray("coordinates")
                            if (coords.length() < 2) continue

                            val x = coords.getDouble(0).toFloat()
                            val y = coords.getDouble(1).toFloat()

                            val roomVertex = RoomVertex(
                                x = x,
                                y = y,
                                bypass = properties.optInt("bypass", 0)
                            )

                            newRoomVertices.add(roomVertex)
                        }
                    }
                }

                if (geoJSONData.has("metadata")) {
                    val metadata = geoJSONData.getJSONObject("metadata")

                    if (metadata.has("scale")) {
                        setScale(metadata.getDouble("scale").toFloat())
                    }

                    if (metadata.has("offset")) {
                        val offsetObj = metadata.getJSONObject("offset")
                        setOffset(
                            Offset(
                            x = offsetObj.optDouble("x", 0.0).toFloat(),
                            y = offsetObj.optDouble("y", 0.0).toFloat()
                        )
                        )
                    }

                    if (metadata.has("canvasSize")) {
                        val sizeObj = metadata.getJSONObject("canvasSize")
                        setCanvasSize(
                            CanvasSize(
                            width = sizeObj.optDouble("width", 800.0).toFloat(),
                            height = sizeObj.optDouble("height", 600.0).toFloat()
                        )
                        )
                    }
                }

                setWalls(newWalls)
                setPois(newPOIs)
                setDoors(newDoors)
                setWindows(newWindows)
                Log.d("after setting zones ","after setting zones ")
                setZones(newZones)
                setRooms(Room(polygons = newRoomPolygons, vertex = newRoomVertices))
                setPlacedObjects(newPlacedObjects)

                val minPoint = calculateMinPoint(newWalls, newPOIs, newDoors, newWindows)
                setMinPoint(minPoint)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setMinPoint(point: Point) {
        floorPlanState = floorPlanState.copy(minPoint = point)
    }

    fun importFromGeoJSONUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    } ?: ""
                }

                if (jsonString.isNotEmpty()) {
                    importFromGeoJSON(jsonString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}