package com.example.malvoayant.data.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.data.api.User
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository
import com.example.malvoayant.ui.screens.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactViewModel (
    private val contactRepository: ContactsRepository,
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {
    private val authViewModel  = AuthViewModel(authRepository,context)
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> get() = _contacts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    fun fetchEmergencyContacts() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val token = authViewModel.getToken()
                if (token == null) {
                    _error.value = "Authentication required"
                    return@launch
                }

                val response = authRepository.getProfile(token)
                val user : User? = authViewModel.getUserInfo()
                Log.d("USER_USER",user.toString())
                val endUserId = user?.endUserId
                if (endUserId == null) {
                    Log.d("ContactVM", "user id: $endUserId")
                    _error.value = "User ID not available"
                    return@launch
                }

                contactRepository.getEmergencyContacts(token, endUserId.toString())
                    .onSuccess { apiContacts ->
                        _contacts.value = apiContacts.map {
                            Log.d("ContactVM", "apiContacts: $apiContacts")
                            Contact(it.nom, it.telephone)
                        }
                    }
                    .onFailure { e ->
                        _error.value = "Failed to load contacts: ${e.message}"
                        Log.e("ContactVM", "Fetch error", e)
                    }
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                Log.e("ContactVM", "Unexpected error", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun addEmergencyContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val token = authRepository.getToken()
                if (token == null) {
                    _error.value = "Authentication required"
                    return@launch
                }


                val endUserId = authRepository.getUserId()
                if (endUserId == null) {
                    _error.value = "User ID not available"
                    return@launch
                }

                contactRepository.addEmergencyContact(token, endUserId.toString(), name, phoneNumber)
                    .onSuccess {
                        // Refresh the contacts list after adding a new contact
                        fetchEmergencyContacts()
                    }
                    .onFailure { e ->
                        _error.value = "Failed to add contact: ${e.message}"
                        Log.e("ContactVM", "Add contact error", e)
                        _loading.value = false
                    }
            } catch (e: Exception) {
                _error.value = "Unexpected error: ${e.message}"
                Log.e("ContactVM", "Unexpected error", e)
                _loading.value = false
            }
        }
    }
}