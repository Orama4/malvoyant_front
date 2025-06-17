package com.example.malvoayant.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("/api/auth/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserProfileResponse>

    @POST("/api/auth/update-profile")
    suspend fun updateProfile(@Header("Authorization") token: String, @Body request: UpdateProfileRequest): Response<Unit>

    @POST("/api/auth/change-password")
    suspend fun changePassword(@Header("Authorization") token: String, @Body request: ChangePasswordRequest): Response<Unit>

    @POST("/api/auth/send-forgot-password-otp")
    suspend fun sendForgotPasswordOTP(@Body request: SendOTPRequest): Response<Unit>

    @POST("/api/auth/verify-forgot-password-otp")
    suspend fun verifyForgotPasswordOTP(@Body request: VerifyOTPRequest): Response<Unit>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "/api/auth/delete-account", hasBody = true)
    suspend fun deleteAccount(@Header("Authorization") token: String, @Body request: DeleteAccountRequest): Response<Unit>
}

data class DeleteAccountRequest(
    val userId: Int
)
data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)
data class ChangePasswordRequest(
    val userId: Int,
    val currentPassword: String,
    val newPassword: String
)
data class UpdateProfileRequest(
    val userId: Int,
    val firstname: String?,
    val lastname: String?,
    val phonenumber: String?,
    val address: String?
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstname: String?,
    val lastname: String?,
    val phonenumber: String?,
    val address: String,
    val role: String? = "endUser"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: User,
    val endUserId:Int
)

data class User(
    val id: Int,
    val email: String,
    val role: String?,
    val endUserId: Int?
)

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val adress:String ,
)
data class UserProfileResponse(
    val id: Int,
    val email: String,
    val role: String?,
    val Profile: Profile?
)

data class Profile(
    val userId: Int,
    val firstname: String?,
    val lastname: String?,
    val phonenumber: String?,
    val address: String?
)
data class SendOTPRequest(
    val email: String
)

data class VerifyOTPRequest(
    val email: String,
    val otp: String
)
