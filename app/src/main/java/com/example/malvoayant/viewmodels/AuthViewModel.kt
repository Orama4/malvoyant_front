package com.example.malvoayant.viewmodels


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.api.ChangePasswordRequest
import com.example.malvoayant.api.DeleteAccountRequest
import com.example.malvoayant.api.LoginRequest
import com.example.malvoayant.api.LoginResponse
import com.example.malvoayant.api.RegisterRequest
import com.example.malvoayant.api.ResetPasswordRequest
import com.example.malvoayant.api.SendOTPRequest
import com.example.malvoayant.api.UpdateProfileRequest
import com.example.malvoayant.api.User
import com.example.malvoayant.api.UserProfile
import com.example.malvoayant.api.VerifyOTPRequest
import com.example.malvoayant.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository, private val context: Context) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> get() = _registerSuccess

    private val _loginSuccess = MutableStateFlow<LoginResponse?>(null)
    val loginSuccess: StateFlow<LoginResponse?> get() = _loginSuccess
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> get() = _userProfile
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> get() = _otpSent

    private val _otpVerified = MutableStateFlow(false)
    val otpVerified: StateFlow<Boolean> get() = _otpVerified


    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.register(request)
                    .onSuccess {
                        _registerSuccess.value = true
                        _error.value = null
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to register: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to register: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }


    fun login(request: LoginRequest) {
        Log.d("enter to login", "enter to login: $request")

        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.login(request)
                    .onSuccess { result ->
                        Log.d("succes login", "succes login: $result")

                        _loginSuccess.value = result
                        _error.value = null
                        saveToken(result.token)
                        saveUserInfo(result.user)
                        fetchUserProfile(result.token)
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to login: ${exception.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to login: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    private fun fetchUserProfile(token: String) {
        Log.d("enter to fetch user ", "fetching user with token: $token")
        viewModelScope.launch {
            authRepository.getProfile(token)
                .onSuccess { profileResponse ->
                    Log.d("in view ","profileResponse:$profileResponse")
                    val profile = profileResponse.Profile
                    Log.d("in view ","profile:$profile")

                    if (profile != null) {
                        val userProfile = UserProfile(
                            name = "${profile.firstname ?: ""} ${profile.lastname ?: ""}".trim(),
                            email = profileResponse.email,
                            phone = profile.phonenumber ?: "",
                            adress = profile.address ?: ""
                        )
                        Log.d("profile infos are ", "profile infoes are: $userProfile")
                        _userProfile.value = userProfile
                        saveUserProfile(userProfile)
                    } else {
                        _error.value = "Profile data is missing"
                    }
                }
                .onFailure { exception ->
                    _error.value = "Failed to fetch profile: ${exception.message}"
                }
        }
    }
    fun saveUserProfile(profile: UserProfile) {
        Log.d("saving profile", "saving profile: $profile")
        sharedPreferences.edit()
            .putString("name", profile.name)
            .putString("email", profile.email)
            .putString("phone", profile.phone)
            .putString("address", profile.adress)
            .apply()
    }
    fun getUserProfile(): UserProfile? {
        val name = sharedPreferences.getString("name", null)
        val email = sharedPreferences.getString("email", null)
        val phone = sharedPreferences.getString("phone", null)
        val address = sharedPreferences.getString("address", null)
        Log.d("getting profile", "getting profile: $name")

        return if (name != null && email != null && phone != null && address != null) {
            UserProfile(name, email, phone, address)
        } else {
            null
        }
    }
    private fun saveToken(token: String) {
        Log.d("saving token", "saving token: $token")

        sharedPreferences.edit().putString("token", token).apply()
    }

    private fun saveUserInfo(user: User) {
        Log.d("saving user", "saving user: $user")

        sharedPreferences.edit()
            .putInt("userId", user.id)
            .putString("email", user.email)
            .putString("role", user.role)
            .putInt("endUserId", user.endUserId ?: -1)
            .apply()
    }
    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }

    fun getUserInfo(): User? {
        val userId = sharedPreferences.getInt("userId", -1)
        val email = sharedPreferences.getString("email", null)
        val role = sharedPreferences.getString("role", null)
        val endUserId = sharedPreferences.getInt("endUserId", -1)
        Log.d("getting user", "getting user: $userId")

        return if (userId != -1 && email != null && role != null) {
            User(userId, email, role,endUserId)

        } else {
            null
        }
    }
    fun logout() {
        viewModelScope.launch {
            _loading.value = true
            try {
                clearSession()
                // Handle success, e.g., navigate to login screen
            } catch (e: Exception) {
                _error.value = "Failed to logout: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val token = getToken()
                if (token != null) {
                    val userId = getUserInfo()?.id ?: -1
                    val request = DeleteAccountRequest(userId = userId)
                    authRepository.deleteAccount(token, request)
                        .onSuccess {
                            _loginSuccess.value=null
                            Log.d("_loginSuccess.value","_loginSuccess.value:$_loginSuccess.value")
                            _error.value = null
                            clearSession()
                            // Handle success, e.g., navigate to login screen
                        }
                        .onFailure { exception ->
                            _error.value = "Failed to delete account: ${exception.localizedMessage ?: "Unknown error"}"
                        }
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete account: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
        _loginSuccess.value=null
    }

    fun updateProfile(token: String, request: UpdateProfileRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.updateProfile(token, request)
                    .onSuccess {
                        _error.value = null
                        // Handle success, e.g., update local profile
                        fetchUserProfile(token)
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to update profile: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun changePassword(token: String, request: ChangePasswordRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.changePassword(token, request)
                    .onSuccess {
                        _error.value = null
                        // Handle success, e.g., show a success message
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to change password: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to change password: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }




    fun sendForgotPasswordOTP(request: SendOTPRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.sendForgotPasswordOTP(request)
                    .onSuccess {
                        _error.value = null
                        // Handle success, e.g., show a success message
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to send OTP: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to send OTP: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyForgotPasswordOTP(request: VerifyOTPRequest) {
        Log.d("am in verifying view","am in verifying view:$request")
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.verifyForgotPasswordOTP(request)
                    .onSuccess {
                        Log.d("succes vrifying ","succes vrifying")
                        _otpVerified.value = true
                        _error.value = null

                    }
                    .onFailure { exception ->
                        _error.value = "Failed to verify OTP: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to verify OTP: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetPassword(request: ResetPasswordRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                authRepository.resetPassword(request)
                    .onSuccess {
                        _error.value = null
                        _loginSuccess.value=null
                        // Handle success, e.g., show a success message
                    }
                    .onFailure { exception ->
                        _error.value = "Failed to reset password: ${exception.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to reset password: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}