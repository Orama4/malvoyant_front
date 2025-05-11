package com.example.malvoayant.data.viewmodels

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

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                _steps.postValue((_steps.value ?: 0) + 1)
                Log.d("StepCounter", "Step detected $_steps.value")
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

                currentHeading = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()

                if (currentHeading < 0) {
                    currentHeading += 360f
                }

                _currentHeadingLive.postValue(currentHeading)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private fun updatePosition() {
        val adjustedHeading = (currentHeading + 90) % 360
        val rad = Math.toRadians(adjustedHeading.toDouble())

        val deltaX = (stepLength * cos(rad)).toFloat()
        val deltaY = (stepLength * sin(rad)).toFloat()

        currentPosition = Pair(currentPosition.first + deltaX, currentPosition.second + deltaY)
        _currentPositionLive.postValue(currentPosition)
        Log.d("position updated ", "poisiton updated $currentPosition")
        val updatedPath = _pathPoints.value.orEmpty() + currentPosition
        _pathPoints.postValue(updatedPath)
    }

    fun startListening() {
        sensorManager.registerListener(stepListener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d("start listen","start listen ")

        sensorManager.registerListener(rotationListener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stopListening() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(rotationListener)
    }

    fun resetAll() {
        _steps.value = 0
        currentPosition = Pair(0f, 0f)
        _currentPositionLive.value = currentPosition
        _pathPoints.value = listOf(currentPosition)
    }
}