package com.example.malvoayant.ui.screens


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun setZones(zones: List<Any>) {
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
                val newZones = mutableListOf<Any>()
                val newRoomPolygons = mutableListOf<RoomPolygon>()
                val newRoomVertices = mutableListOf<RoomVertex>()
                val newPlacedObjects = mutableListOf<DoorWindow>()
                var navigationSettings: NavigationSettings? = null
                var hazards: List<HazardMarker> = emptyList()

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
                                type = properties.optString("type", "wall"),
                                navigation = parseNavigationProperties(properties)
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
                                height = properties.optDouble("height", 50.0).toFloat(),
                                navigation = parseNavigationProperties(properties)
                            )

                            newPOIs.add(poi)
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
                                wall = wallReference,
                                navigation = parseNavigationProperties(properties)
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
                                center = center,
                                navigation = parseNavigationProperties(properties)
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
                        setOffset(Offset(
                            x = offsetObj.optDouble("x", 0.0).toFloat(),
                            y = offsetObj.optDouble("y", 0.0).toFloat()
                        ))
                    }

                    if (metadata.has("canvasSize")) {
                        val sizeObj = metadata.getJSONObject("canvasSize")
                        setCanvasSize(CanvasSize(
                            width = sizeObj.optDouble("width", 800.0).toFloat(),
                            height = sizeObj.optDouble("height", 600.0).toFloat()
                        ))
                    }
                    if (metadata.has("navigationSettings")) {
                        val navSettings = metadata.getJSONObject("navigationSettings")

                        val northRef = navSettings.getJSONObject("northReference")
                        val audioSettings = navSettings.getJSONObject("audioSettings")

                        navigationSettings = NavigationSettings(
                            defaultStepLength = navSettings.getDouble("defaultStepLength").toFloat(),
                            unit = navSettings.getString("unit"),
                            magneticDeclination = navSettings.getDouble("magneticDeclination").toFloat(),
                            northReference = NorthReference(
                                wallId = northRef.getInt("wallId"),
                                direction = northRef.getDouble("direction").toFloat()
                            ),
                            calibrationPoints = parseCalibrationPoints(navSettings),
                            knownDistances = parseKnownDistances(navSettings),
                            hazardMarkers = parseHazardMarkers(navSettings),
                            audioSettings = AudioSettings(
                                landmarkVolume = audioSettings.getDouble("landmarkVolume").toFloat(),
                                directionVolume = audioSettings.getDouble("directionVolume").toFloat(),
                                hazardVolume = audioSettings.getDouble("hazardVolume").toFloat(),
                                repeatInterval = audioSettings.getInt("repeatInterval")
                            )
                        )

                        hazards = parseHazardMarkers(navSettings)
                    }
                }

                setWalls(newWalls)
                setPois(newPOIs)
                setDoors(newDoors)
                setWindows(newWindows)
                setZones(newZones)
                setRooms(Room(polygons = newRoomPolygons, vertex = newRoomVertices))
                setPlacedObjects(newPlacedObjects)

                val minPoint = calculateMinPoint(newWalls, newPOIs, newDoors, newWindows)
                setMinPoint(minPoint)

                floorPlanState = floorPlanState.copy(
                    walls = newWalls,
                    pois = newPOIs,
                    doors = newDoors,
                    windows = newWindows,
                    zones = newZones,
                    rooms = Room(polygons = newRoomPolygons, vertex = newRoomVertices),
                    placedObjects = newPlacedObjects,
                    navigationSettings = navigationSettings,
                    hazards = hazards
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Helper functions for parsing navigation data
    private fun parseNavigationProperties(properties: JSONObject): NavigationProperties? {
        return if (properties.has("navigation")) {
            val nav = properties.getJSONObject("navigation")
            NavigationProperties(
                audioCue = nav.optString("audioCue", null),
                hapticFeedback = nav.optString("hapticFeedback", null),
                approachDistance = nav.optDouble("approachDistance").takeIf { !nav.isNull("approachDistance") }?.toFloat(),
                interaction = nav.optString("interaction", null),
                landmark = nav.optBoolean("landmark").takeIf { nav.has("landmark") },
                calibrationPoint = nav.optBoolean("calibrationPoint").takeIf { nav.has("calibrationPoint") },
                defaultOrientation = nav.optDouble("defaultOrientation").takeIf { !nav.isNull("defaultOrientation") }?.toFloat(),
                hazard = nav.optString("hazard", null),
                surfaceType = nav.optString("surfaceType", null),
                echoProfile = nav.optString("echoProfile", null)
            )
        } else {
            null
        }
    }

    private fun parseCalibrationPoints(navSettings: JSONObject): List<CalibrationPoint> {
        val points = mutableListOf<CalibrationPoint>()
        if (navSettings.has("calibrationPoints")) {
            val pointsArray = navSettings.getJSONArray("calibrationPoints")
            for (i in 0 until pointsArray.length()) {
                val point = pointsArray.getJSONObject(i)
                points.add(
                    CalibrationPoint(
                        featureId = point.getString("featureId"),
                        type = point.getString("type"),
                        defaultOrientation = point.getDouble("defaultOrientation").toFloat(),
                        description = point.getString("description")
                    )
                )
            }
        }
        return points
    }

    private fun parseKnownDistances(navSettings: JSONObject): List<KnownDistance> {
        val distances = mutableListOf<KnownDistance>()
        if (navSettings.has("knownDistances")) {
            val distArray = navSettings.getJSONArray("knownDistances")
            for (i in 0 until distArray.length()) {
                val dist = distArray.getJSONObject(i)
                distances.add(
                    KnownDistance(
                        from = dist.getString("from"),
                        to = dist.getString("to"),
                        distance = dist.getDouble("distance").toFloat(),
                        unit = dist.getString("unit")
                    )
                )
            }
        }
        return distances
    }

    private fun parseHazardMarkers(navSettings: JSONObject): List<HazardMarker> {
        val hazards = mutableListOf<HazardMarker>()
        if (navSettings.has("hazardMarkers")) {
            val hazardsArray = navSettings.getJSONArray("hazardMarkers")
            for (i in 0 until hazardsArray.length()) {
                val hazard = hazardsArray.getJSONObject(i)
                val coords = hazard.getJSONArray("coordinates")
                hazards.add(
                    HazardMarker(
                        coordinates = listOf(coords.getDouble(0).toFloat(), coords.getDouble(1).toFloat()),
                        type = hazard.getString("type"),
                        description = hazard.getString("description"),
                        warningDistance = hazard.getDouble("warningDistance").toFloat(),
                        unit = hazard.getString("unit")
                    )
                )
            }
        }
        return hazards
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