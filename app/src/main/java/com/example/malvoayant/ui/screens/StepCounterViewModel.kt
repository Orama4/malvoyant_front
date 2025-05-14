
package com.example.malvoayant.ui.screens

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.cos
import kotlin.math.sin
class StepCounterViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _steps = MutableLiveData(0)
    val steps: LiveData<Int> = _steps

    private val _currentHeadingLive = MutableLiveData(0f)
    val currentHeadingLive: LiveData<Float> = _currentHeadingLive

    private var currentHeading = 0f
    private var currentPosition = Pair(0f, 0f)
    private val stepLength = 0.7f  // Average step length in meters

    private val _currentPositionLive = MutableLiveData(Pair(0f, 0f))
    val currentPositionLive: LiveData<Pair<Float, Float>> = _currentPositionLive

    private val _pathPoints = MutableLiveData(listOf(Pair(0f, 0f)))
    val pathPoints: LiveData<List<Pair<Float, Float>>> = _pathPoints

    // Environment reference angles (map coordinate system)
    private var environmentNorth = 0f

    // Flag to know if environment directions are calibrated
    private var isCalibrated = false

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                _steps.postValue((_steps.value ?: 0) + 1)
                updatePosition()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private val rotationListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                // Convert to degrees and normalize to 0-360
                currentHeading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()-50
                if (currentHeading < 0) {
                    currentHeading += 360f
                }

                // Calculate heading relative to environment
                val envHeading = calculateEnvironmentHeading(currentHeading)
                _currentHeadingLive.postValue(envHeading)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    /**
     * Calculates heading relative to the environment coordinate system
     */
    private fun calculateEnvironmentHeading(deviceHeading: Float): Float {
        return if (isCalibrated) {
            // Calculate heading relative to calibrated environment North
            ((deviceHeading - environmentNorth + 360) % 360)
        } else {
            // If not calibrated, just return device heading
            deviceHeading
        }
    }

    private fun updatePosition() {
        // Use the environment-relative heading for movement
        val envHeading = calculateEnvironmentHeading(currentHeading)

        // Convert heading to radians
        val rad = Math.toRadians(envHeading.toDouble())

        // Calculate position changes
        val deltaX = (stepLength * cos(rad)).toFloat()
        val deltaY = (stepLength * sin(rad)).toFloat()

        // Update position using floor plan coordinate system
        currentPosition = Pair(
            currentPosition.first + deltaX,
            currentPosition.second + deltaY
        )

        _currentPositionLive.postValue(currentPosition)

        val updatedPath = _pathPoints.value.orEmpty() + currentPosition
        _pathPoints.postValue(updatedPath)

        Log.d("Navigation", "Step taken: heading=$envHeading°, position=$currentPosition")
    }

    /**
     * Calibrate the environment coordinate system.
     * Call this when user is facing a known direction in your floor plan.
     *
     * @param environmentAngle The angle in your floor plan coordinate system that user is facing
     */
    fun calibrateEnvironment(environmentAngle: Float) {
        // Current device heading represents the provided environment angle
        environmentNorth = (currentHeading - environmentAngle + 360) % 360
        isCalibrated = true
        Log.d("Navigation", "Calibrated: Device $currentHeading° = Environment $environmentAngle°, North at $environmentNorth°")
    }

    /**
     * Reset calibration to use raw device orientation
     */
    fun resetCalibration() {
        isCalibrated = false
        environmentNorth = 0f
        Log.d("Navigation", "Environment calibration reset")
    }

    fun startListening() {
        sensorManager.registerListener(stepListener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(rotationListener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        Log.d("Navigation", "Started listening for steps and orientation")
    }

    fun stopListening() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(rotationListener)
        Log.d("Navigation", "Stopped listening for sensors")
    }

    fun resetAll() {
        _steps.value = 0
        currentPosition = Pair(0f, 0f)
        _currentPositionLive.value = currentPosition
        _pathPoints.value = listOf(currentPosition)
        resetCalibration()
        Log.d("Navigation", "Reset all navigation data")
    }
}

