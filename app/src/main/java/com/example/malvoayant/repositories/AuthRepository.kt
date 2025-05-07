package com.example.malvoayant.repositories

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

class AuthRepository(private val authApiService: AuthApiService) {
    // Variable to store the token
    private var authToken: String? = null
    private var userId: Int? = null

    suspend fun register(request: RegisterRequest): Result<Unit> {
        Log.d("repo1", "repo1: $request")

        return try {
            val response = authApiService.register(request)
            Log.d("response", "response: $response")

            if (response.isSuccessful) {
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
                // Store the token from the response
                authToken = response.body()!!.token
                userId = response.body()!!.user.endUserId  // This will give you 1
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("exception", "Exception during login: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Function to get the stored token
    fun getToken(): String? {
        return authToken
    }
    // Function to get the stored token
    fun getUserId(): String? {
        return userId.toString()
    }

    suspend fun getProfile(token: String): Result<UserProfileResponse> {
        return try {
            Log.d("entering to repo to fetch userprofile ","fetchin with token: $token")
            val response = authApiService.getProfile("Bearer $token")
            Log.d("see response ","heyyyyyyyy $response")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(token: String, request: UpdateProfileRequest): Result<Unit> {
        return try {
            val response = authApiService.updateProfile("Bearer $token", request)
            if (response.isSuccessful) {
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
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}