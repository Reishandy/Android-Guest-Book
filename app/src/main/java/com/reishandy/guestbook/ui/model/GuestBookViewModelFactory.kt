package com.reishandy.guestbook.ui.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GuestBookViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GuestBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GuestBookViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}