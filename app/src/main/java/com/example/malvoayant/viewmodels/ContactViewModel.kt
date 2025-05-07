package com.example.malvoayant.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository
import com.example.malvoayant.ui.screens.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val contactRepository: ContactsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
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
                val token = authRepository.getToken()
                if (token == null) {
                    _error.value = "Authentication required"
                    return@launch
                }

                val userInfo = authRepository.getUserInfo()
                val endUserId = userInfo?.id
                if (endUserId == null) {
                    _error.value = "User ID not available"
                    return@launch
                }

                contactRepository.getEmergencyContacts(token, endUserId.toString())
                    .onSuccess { apiContacts ->
                        _contacts.value = apiContacts.map {
                            Contact(it.name, it.phoneNumber)
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

                val userInfo = authRepository.getUserInfo()
                val endUserId = userInfo?.id
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