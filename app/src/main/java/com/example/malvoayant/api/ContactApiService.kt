package com.example.malvoayant.api
import retrofit2.Response
import retrofit2.http.*

interface ContactApiService {
    // Récupérer les contacts d'urgence pour un endUser
    @GET("/urgence/{endUserId}")
    suspend fun getEmergencyContacts(
        @Path("endUserId") endUserId: String,
        @Header("Authorization") token: String
    ): Response<List<ApiContact>>

    // Ajouter un contact d'urgence
    @POST("/urgence/{endUserId}")
    suspend fun addEmergencyContact(
        @Path("endUserId") endUserId: String,
        @Body request: AddContactRequest,
        @Header("Authorization") token: String
    ): Response<Unit>
}

// Modèles de données
data class ApiContact(
    val id: String,
    val name: String,
    val phoneNumber: String
)

data class AddContactRequest(
    val name: String,
    val phoneNumber: String
)