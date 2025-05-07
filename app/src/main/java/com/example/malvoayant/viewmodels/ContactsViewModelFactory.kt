package com.example.malvoayant.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository

class ContactsViewModelFactory(
    private val repository: ContactsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactViewModel(
            repository,
            authRepository = authRepository
        ) as T
    }
}