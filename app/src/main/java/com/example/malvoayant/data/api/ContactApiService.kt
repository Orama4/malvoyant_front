package com.example.malvoayant.data.api
import retrofit2.Response
import retrofit2.http.*

interface ContactApiService {
    // Récupérer les contacts d'urgence pour un endUser
    @GET("/api/emergency/urgence/{endUserId}")
    suspend fun getEmergencyContacts(
        @Path("endUserId") endUserId: String,
        @Header("Authorization") token: String
    ): Response<List<ApiContact>>

    // Ajouter un contact d'urgence
    @POST("/api/emergency/urgence/{endUserId}")
    suspend fun addEmergencyContact(
        @Path("endUserId") endUserId: String,
        @Body request: AddContactRequest,
        @Header("Authorization") token: String
    ): Response<Unit>
}

// Modèles de données
data class ApiContact(
    val id: String,
    val nom: String,
    val telephone: String
)

data class AddContactRequest(
    val nom: String,
    val telephone: String
)