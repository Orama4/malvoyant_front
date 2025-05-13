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

    // Initial reference heading when the user starts walking
    private var initialHeading: Float? = null

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
                currentHeading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                if (currentHeading < 0) {
                    currentHeading += 360f
                }

                // ✅ Nouvelle version : initialiser initialHeading dès que possible
                if (initialHeading == null) {
                    initialHeading = currentHeading
                }

                // ❌ Ancienne version (trop tardive)
                // if (initialHeading == null && _steps.value!! > 0) {
                //     initialHeading = currentHeading
                // }

                // ✅ Relative heading bien calculée en [0, 360[
                val relativeHeading = if (initialHeading != null) {
                    ((currentHeading - initialHeading!! + 360) % 360)
                } else {
                    currentHeading
                }

                _currentHeadingLive.postValue(relativeHeading)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun updatePosition() {
        // ❌ Ancienne version : bloque le premier pas
        // if (initialHeading == null) {
        //     initialHeading = currentHeading
        //     return
        // }

        // ✅ Nouvelle version : on suppose que initialHeading est bien initialisé au préalable

        // ✅ Relative heading toujours positif [0, 360)
        val relativeHeading = ((currentHeading - initialHeading!! + 360) % 360)

        // Convert heading to radians
        val rad = Math.toRadians(relativeHeading.toDouble())

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

        Log.d("Navigation", "Step taken: heading=$relativeHeading°, position=$currentPosition")
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
        initialHeading = null
        Log.d("Navigation", "Reset all navigation data")
    }
}
