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

    @POST("/api/emergency/assign-helper")
    suspend fun assignHelperToEndUser(
        @Body request: AssignHelperRequest,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/api/emergency/has-helper/{userId}")
    suspend fun hasHelper(
        @Path("userId") userId: Int,
        @Header("Authorization") token: String
    ): Response<HasHelperResponse>



}



data class HasHelperResponse(
    val hasHelper: Boolean,
    val helperId: Int?
)


data class AssignHelperRequest(
    val email: String,
    val endUserId: Int
)

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