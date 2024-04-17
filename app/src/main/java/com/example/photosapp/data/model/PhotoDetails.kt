package com.example.photosapp.data.model

import android.graphics.Bitmap

data class PhotoDetails(
    val id: String,
    val photoBitmap: Bitmap,
    val description: String,
    val location: String,
    val people: String
)
