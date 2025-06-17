package com.example.malvoayant.repositories

import android.util.Log
import com.example.malvoayant.data.api.ApiContact
import com.example.malvoayant.data.api.AddContactRequest
import com.example.malvoayant.data.api.AssignHelperRequest
import com.example.malvoayant.data.api.ContactApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

@Singleton
class ContactsRepository (
    private val contactApiService: ContactApiService
) {
    suspend fun getEmergencyContacts(token: String, endUserId: String): Result<List<ApiContact>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ContactRepo", "Fetching contacts for endUser: $endUserId")
                val response = contactApiService.getEmergencyContacts(
                    endUserId = endUserId,
                    token = "Bearer $token"
                )

                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    val errorMsg = "API error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e("ContactRepo", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ContactRepo", "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun assignHelper(
        token: String,
        email: String,
        endUserId: Int
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = contactApiService.assignHelperToEndUser(
                    request = AssignHelperRequest(email, endUserId),
                    token = "Bearer $token"
                )

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = "API error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e("ContactRepo", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ContactRepo", "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun addEmergencyContact(
        token: String,
        endUserId: String,
        name: String,
        phoneNumber: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ContactRepo", "Adding contact for endUser: $endUserId")
                val response = contactApiService.addEmergencyContact(
                    endUserId = endUserId,
                    request = AddContactRequest(name, phoneNumber),
                    token = "Bearer $token"
                )

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorMsg = "API error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e("ContactRepo", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ContactRepo", "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }


    suspend fun hasHelper(token: String, userId: Int): Result<Pair<Boolean, Int?>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = contactApiService.hasHelper(userId, "Bearer $token")

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        Result.success(Pair(result.hasHelper, result.helperId))
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                } else {
                    val errorMsg = "API error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e("ContactRepo", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("ContactRepo", "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }


}