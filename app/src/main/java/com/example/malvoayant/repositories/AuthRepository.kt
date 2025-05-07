package com.example.malvoayant.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.malvoayant.api.AuthApiService
import com.example.malvoayant.api.ChangePasswordRequest
import com.example.malvoayant.api.DeleteAccountRequest
import com.example.malvoayant.api.LoginRequest
import com.example.malvoayant.api.LoginResponse
import com.example.malvoayant.api.RegisterRequest
import com.example.malvoayant.api.ResetPasswordRequest
import com.example.malvoayant.api.SendOTPRequest
import com.example.malvoayant.api.UpdateProfileRequest
import com.example.malvoayant.api.UserProfileResponse
import com.example.malvoayant.api.VerifyOTPRequest
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val context: Context
) {
    companion object {
        private const val PREF_NAME = "auth_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_INFO = "user_info"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val gson = Gson()

    // Get authentication token
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // Save authentication token
    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    // Clear authentication data (for logout)
    fun clearAuthData() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_INFO)
            .apply()
    }

    // Get user information
    fun getUserInfo(): UserProfileResponse? {
        val userJson = prefs.getString(KEY_USER_INFO, null) ?: return null
        return try {
            gson.fromJson(userJson, UserProfileResponse::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error parsing user info", e)
            null
        }
    }

    // Save user information
    private fun saveUserInfo(userInfo: UserProfileResponse) {
        val userJson = gson.toJson(userInfo)
        prefs.edit().putString(KEY_USER_INFO, userJson).apply()
    }

    suspend fun register(request: RegisterRequest): Result<Unit> {
        Log.d("repo1", "repo1: $request")

        return try {
            val response = authApiService.register(request)
            Log.d("response", "response: $response")

            if (response.isSuccessful) {
                // Return success without trying to access the body
                Log.d("register", "Registration successful with code: ${response.code()}")
                Result.success(Unit)
            } else {
                Log.d("failure", "failure response: ${response.message()}")
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("exception", "Exception during register: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            Log.d("in repo", "in repo : $request")

            val response = authApiService.login(request)
            Log.d("in repo result ", "in repo resulet  : $response")

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                // Save the token and potentially some user information from login response
                loginResponse.token?.let { saveToken(it) }

                Result.success(loginResponse)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("exception", "Exception during login: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getProfile(token: String): Result<UserProfileResponse> {
        return try {
            Log.d("entering to repo to fetch userprofile ","fetching with token: $token")
            val response = authApiService.getProfile("Bearer $token")
            Log.d("see response ","heyyyyyyyy $response")

            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                // Save user profile information locally
                saveUserInfo(userProfile)
                Result.success(userProfile)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // This function refreshes user profile from the API
    suspend fun refreshUserProfile(): Result<UserProfileResponse> {
        val token = getToken() ?: return Result.failure(Exception("Not authenticated"))
        return getProfile(token)
    }

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): Result<Unit> {
        return try {
            val response = authApiService.updateProfile("Bearer $token", request)
            if (response.isSuccessful) {
                // After successful profile update, refresh the stored profile
                refreshUserProfile()
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(token: String, request: ChangePasswordRequest): Result<Unit> {
        return try {
            val response = authApiService.changePassword("Bearer $token", request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendForgotPasswordOTP(request: SendOTPRequest): Result<Unit> {
        return try {
            val response = authApiService.sendForgotPasswordOTP(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyForgotPasswordOTP(request: VerifyOTPRequest): Result<Unit> {
        return try {
            val response = authApiService.verifyForgotPasswordOTP(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Result<Unit> {
        return try {
            val response = authApiService.resetPassword(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(token: String, request: DeleteAccountRequest): Result<Unit> {
        return try {
            val response = authApiService.deleteAccount("Bearer $token", request)
            if (response.isSuccessful) {
                // Clear local authentication data after successful account deletion
                clearAuthData()
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Function to logout user
    fun logout() {
        clearAuthData()
    }
}