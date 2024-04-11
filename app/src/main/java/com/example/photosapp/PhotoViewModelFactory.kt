package com.example.photosapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.photoapp.data.LoginDataSource
import com.example.photosapp.data.PhotoRepository

class PhotoViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
            return PhotoViewModel(
                photoRepository = PhotoRepository(appContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}