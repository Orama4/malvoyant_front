package com.example.malvoayant.data.api


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/location")
    fun sendLocation(@Body requestData: LocationRequestData): Call<String>
}