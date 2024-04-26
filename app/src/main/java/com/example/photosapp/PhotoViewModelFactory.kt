package com.example.photosapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.photosapp.data.PhotoRepository

/**
 * Factory class responsible for creating instances of the PhotoViewModel.
 *
 * It provides a way to pass dependencies, such as the application context and repositories,
 * to the PhotoViewModel.
 *
 * @property appContext The application context used for creating the PhotoRepository.
 */
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