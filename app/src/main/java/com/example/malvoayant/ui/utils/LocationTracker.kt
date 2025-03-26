package com.example.malvoayant.ui.utils


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.example.malvoayant.data.api.ApiClient
import com.example.malvoayant.data.api.LocationRequestData
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LocationTrackerService(private val activity: androidx.appcompat.app.AppCompatActivity) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false

    // LiveData that can be observed by ViewModel
    val locationData = MutableLiveData<Location>()
    val trackingStatus = MutableLiveData<Boolean>()
    val serverResponse = MutableLiveData<String>()

    init {
        setupLocationCallback()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Update LiveData with new location
                    locationData.postValue(location)

                    // Log the location
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d(TAG, "Updated Location - Latitude: $lat, Longitude: $lon")

                    // Send to server using your existing API client
                    sendLocationToServer(lat, lon)
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 3000 // Update interval in milliseconds (3 seconds)
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        isTracking = true
        trackingStatus.postValue(true)
        Log.d(TAG, "Location tracking started")
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTracking = false
        trackingStatus.postValue(false)
        Log.d(TAG, "Location tracking stopped")
    }

    fun toggleLocationUpdates() {
        if (isTracking) {
            stopLocationUpdates()
        } else {
            startLocationUpdates()
        }
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        // Using your existing ApiClient to communicate with the server
        val apiService = ApiClient.instance
        val requestData = LocationRequestData(lat, lon)

        apiService.sendLocation(requestData).enqueue(object : Callback<String> {
            private fun formatJson(response: String?): String {
                return response ?: "Invalid response"
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    val formattedResponse = formatJson(response.body())
                    Log.d(TAG, "Server Response:\n$formattedResponse")
                    serverResponse.postValue(formattedResponse)
                } else {
                    val errorMsg = "Error: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    serverResponse.postValue(errorMsg)
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                val errorMsg = "Failed to reach server: ${t.message}"
                Log.e(TAG, errorMsg)
                serverResponse.postValue(errorMsg)
            }
        })
    }

    companion object {
        private const val TAG = "LocationTrackerService"
    }
