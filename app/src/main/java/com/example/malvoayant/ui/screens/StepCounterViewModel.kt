package com.example.malvoayant.ui.screens
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs

class StepCounterViewModel(application: Application,   var floorPlanState: FloorPlanState,) : AndroidViewModel(application) {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager


    // Use accelerometer for more reliable step detection
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _steps = MutableLiveData(0)
    val steps: LiveData<Int> = _steps

    private val _currentHeadingLive = MutableLiveData(0f)
    val currentHeadingLive: LiveData<Float> = _currentHeadingLive

    private var currentHeading = 0f
    private var currentPosition = Pair(0f, 0f)
    private val stepLength = 1f

    private val _currentPositionLive = MutableLiveData(Pair(0f, 0f))
    val currentPositionLive: LiveData<Pair<Float, Float>> = _currentPositionLive

    private val _pathPoints = MutableLiveData(listOf(Pair(0f, 0f)))
    val pathPoints: LiveData<List<Pair<Float, Float>>> = _pathPoints

    private var environmentNorth = 0f
    private var isCalibrated = false

    // Step detection parameters
    private var lastAcceleration = 0f
    private var previousAcceleration = 0f
    private var lastStepTime = 0L
    private var stepThreshold = 1.8f // Optimized for normal walking (1.5-3.0 range)
    private val minStepInterval = 250L // Minimum 250ms between steps (240 steps/min max)
    private val maxStepInterval = 2000L // Maximum 2 seconds between steps (30 steps/min min)
    private var consecutiveNoSteps = 0
    private val maxConsecutiveNoSteps = 10

    // Low-pass filter for smoothing accelerometer data
    private val alpha = 0.8f
    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)

    // Step detection using both sensors for redundancy
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_STEP_DETECTOR -> {
                    // Hardware step detector as backup
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastStepTime > minStepInterval) {
                        handleStepDetected("hardware")
                        lastStepTime = currentTime
                    }
                }

                Sensor.TYPE_ACCELEROMETER -> {
                    // Custom step detection using accelerometer
                    processAccelerometerData(event.values)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d("StepCounter", "Sensor accuracy changed: ${sensor.name} = $accuracy")
        }
    }

    private fun processAccelerometerData(values: FloatArray) {
        // Apply low-pass filter to isolate gravity
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2]

        // Remove gravity to get linear acceleration
        linearAcceleration[0] = values[0] - gravity[0]
        linearAcceleration[1] = values[1] - gravity[1]
        linearAcceleration[2] = values[2] - gravity[2]

        // Calculate magnitude of linear acceleration
        val magnitude = sqrt(
            linearAcceleration[0] * linearAcceleration[0] +
                    linearAcceleration[1] * linearAcceleration[1] +
                    linearAcceleration[2] * linearAcceleration[2]
        )

        // Step detection algorithm
        detectStep(magnitude)
    }

    private fun detectStep(acceleration: Float) {
        val currentTime = System.currentTimeMillis()

        // Check for step pattern: significant change in acceleration
        val accelerationChange = abs(acceleration - lastAcceleration)

        if (accelerationChange > stepThreshold &&
            currentTime - lastStepTime > minStepInterval) {

            // Additional validation: check if we have a proper step pattern
            if (isValidStepPattern(acceleration, lastAcceleration, previousAcceleration)) {
                handleStepDetected("accelerometer")
                lastStepTime = currentTime
                consecutiveNoSteps = 0
            }
        }

        // Update acceleration history
        previousAcceleration = lastAcceleration
        lastAcceleration = acceleration

        // Check for walking timeout
        if (currentTime - lastStepTime > maxStepInterval) {
            consecutiveNoSteps++
            if (consecutiveNoSteps > maxConsecutiveNoSteps) {
            }
        }
    }

    private fun isValidStepPattern(current: Float, last: Float, previous: Float): Boolean {
        // Check for a peak or valley pattern typical of walking
        val isIncreasing = current > last && last > previous
        val isDecreasing = current < last && last < previous
        val hasPeak = last > current && last > previous
        val hasValley = last < current && last < previous

        return hasPeak || hasValley || (isIncreasing && current > stepThreshold) ||
                (isDecreasing && abs(current - previous) > stepThreshold * 0.5f)
    }

    private fun handleStepDetected(source: String) {
        _steps.postValue((_steps.value ?: 0) + 1)
        updatePosition()
        Log.d("StepCounter", "Step detected from $source sensor. Total steps: ${_steps.value}")
    }

    private val rotationListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                // Convert to degrees and normalize to 0-360
                currentHeading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 127
                if (currentHeading < 0) {
                    currentHeading += 360f
                }

                // Calculate heading relative to environment
                val envHeading = calculateEnvironmentHeading(currentHeading)
                _currentHeadingLive.postValue(envHeading)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                Log.w("StepCounter", "Rotation sensor accuracy is unreliable")
            }
        }
    }

    private fun calculateEnvironmentHeading(deviceHeading: Float): Float {
        return if (isCalibrated) {
            ((deviceHeading - environmentNorth + 360) % 360)
        } else {
            deviceHeading
        }
    }
    fun getOuterWallPolygon(floorPlanState: FloorPlanState): List<Pair<Float, Float>> {

        val polygon = mutableListOf<Pair<Float, Float>>()

        for (wall in floorPlanState.walls) {
            val start = Pair(wall.start.x, wall.start.y)
            val end = Pair(wall.end.x, wall.end.y)
            if (polygon.contains(start)) {
                // Si start existe déjà, on ajoute end
                polygon.add(end)
            } else {
                // Sinon, on ajoute start
                polygon.add(start)
            }
        }

        return polygon
    }

    fun isPointInPolygon(point: Pair<Float, Float>, polygon: List<Pair<Float, Float>>): Boolean {
        var (x, y) = point
        x=x*50+floorPlanState.minPoint.x
        y=y*50+floorPlanState.minPoint.y

        var inside = false
        val n = polygon.size
        Log.d("PolygonCheck", "Checking if point ($x, $y) is inside polygon with $n vertices.")

        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            Log.d("PolygonCheck", "Edge from ($xi, $yi) to ($xj, $yj)")

            if (((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / (yj - yi + 0.0000001f) + xi)) {
                inside = !inside
                Log.d("PolygonCheck", "Point crosses edge from ($xi, $yi) to ($xj, $yj). Inside status: $inside")
            }

            j = i
        }

        Log.d("PolygonCheck", "Point ($x, $y) is inside polygon: $inside")
        return inside
    }


    private fun updatePosition() {
        val envHeading = calculateEnvironmentHeading(currentHeading)
        val rad = Math.toRadians(envHeading.toDouble())

        var deltaX = (stepLength * cos(rad)).toFloat()
        var deltaY = (stepLength * sin(rad)).toFloat()

        // Initial step length
        var currentStepLength = stepLength

        // Minimum step length threshold
        val minStepLength = 0.2f

        var nextPosition = Pair(
            currentPosition.first + deltaX,
            currentPosition.second + deltaY
        )

        val outerPolygon: List<Pair<Float, Float>> = getOuterWallPolygon(floorPlanState)

        // Reduce step length until the next position is inside the polygon or step length is too small
        while (!isPointInPolygon(nextPosition, outerPolygon) && currentStepLength > minStepLength) {
            currentStepLength -= 0.1f
            deltaX = (currentStepLength * cos(rad)).toFloat()
            deltaY = (currentStepLength * sin(rad)).toFloat()
            nextPosition = Pair(
                currentPosition.first + deltaX,
                currentPosition.second + deltaY
            )
        }

        // Check if the final position is inside the polygon
        if (isPointInPolygon(nextPosition, outerPolygon)) {
            currentPosition = nextPosition
            _currentPositionLive.postValue(currentPosition)
            val updatedPath = _pathPoints.value.orEmpty() + currentPosition
            _pathPoints.postValue(updatedPath)
            Log.d("Navigation", "Step taken: heading=$envHeading°, position=$currentPosition")
        } else {
            Log.d("Navigation", "Step ignored: next position is outside the walls!, Current position: $currentPosition")
        }
    }


    fun startListening() {
        // Register both accelerometer and step detector for redundancy
        accelerometer?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        stepDetector?.let {
            sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        rotationVector?.let {
            sensorManager.registerListener(rotationListener, it, SensorManager.SENSOR_DELAY_GAME)
        }

        Log.d("StepCounter", "Started listening - Accelerometer: ${accelerometer != null}, StepDetector: ${stepDetector != null}")
    }

    fun stopListening() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(rotationListener)
        Log.d("StepCounter", "Stopped listening for sensors")
    }


    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

}