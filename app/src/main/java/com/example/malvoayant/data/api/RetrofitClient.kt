package com.example.malvoayant.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    private const val AUTH_BASE_URL = "http://172.20.10.3:3001"  // Localhost for emulator
    private const val CONTACTS_BASE_URL = "http://172.20.10.3:3002"

    // Auth service client
    val authApiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }

    // Contacts service client
    val contactService: ContactApiService by lazy {
        Retrofit.Builder()
            .baseUrl(CONTACTS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ContactApiService::class.java)
    }
}