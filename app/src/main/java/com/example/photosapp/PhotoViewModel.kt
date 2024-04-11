package com.example.photosapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.photosapp.data.PhotoRepository
import com.example.photosapp.data.model.Tags

class PhotoViewModel(private val photoRepository: PhotoRepository) : ViewModel() {

    //Some mutable live data
    val tagsLiveData: LiveData<Tags?> = photoRepository.tagsLiveData
    val photoLiveData: LiveData<Map<String, String>> = photoRepository.photoLiveData

    fun loadTags() {
        photoRepository.getTags()
    }
    fun loadPhotos() {
        photoRepository.getPhotos()
    }

    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.publishPost(imageBase64, newTagDes, newTagLoc, newTagPeopleName)
    }

    fun updateTags(position: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.updateTags(position, newTagDes, newTagPho, newTagLoc, newTagPeopleName)
    }






}