package com.example.photosapp

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photosapp.data.PhotoRepository
import com.example.photosapp.data.model.Tags

class PhotoViewModel(private val photoRepository: PhotoRepository) : ViewModel() {

    //Some mutable live data
    val tagsLiveData: LiveData<Tags?> = photoRepository.tagsLiveData
    val photoLiveData: LiveData<Map<String, String>> = photoRepository.photoLiveData

    private val _imageBitmap = MutableLiveData<Bitmap>()
    val currentImageBitmap: LiveData<Bitmap> = _imageBitmap

    fun setCurrentImageBitmap(bitmap: Bitmap) {
        _imageBitmap.value = bitmap
    }

    fun loadTags() {
        photoRepository.getTags()
    }
    fun loadPhotos() {
        photoRepository.getPhotos()
    }

    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.publishPost(imageBase64, newTagDes, newTagLoc, newTagPeopleName)
    }
    fun updatePost(position: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.updatePost(position, newTagDes, newTagPho, newTagLoc, newTagPeopleName)
    }






}