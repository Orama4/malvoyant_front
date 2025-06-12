package com.example.malvoayant.data.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.malvoayant.repositories.AuthRepository
import com.example.malvoayant.repositories.ContactsRepository

class ContactsViewModelFactory(
    private val repository: ContactsRepository,
    private val authRepository: AuthRepository,
    private val context :Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactViewModel(
            repository,
            authRepository = authRepository,
            context
        ) as T
    }
}