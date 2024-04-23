package com.example.photosapp

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photosapp.data.PhotoRepository
import com.example.photosapp.data.model.PhotoDetails
import com.example.photosapp.data.model.Tags

class PhotoViewModel(private val photoRepository: PhotoRepository) : ViewModel() {

    //Some mutable live data
    val tagsLiveData: LiveData<Tags?> = photoRepository.tagsLiveData
    val photoLiveData: LiveData<Map<String, String>> = photoRepository.photoLiveData

    private val _photoDetails = MutableLiveData<PhotoDetails>()
    val currentPhotoDetails: LiveData<PhotoDetails> = _photoDetails

    private val _currentPhoto = MutableLiveData<Bitmap>()
    val currentPhoto: LiveData<Bitmap> = _currentPhoto

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

    fun setCurrentPhotoDetails(photoDetail : PhotoDetails) {
        _photoDetails.value = photoDetail
    }

    fun setCurrentPhoto(bitmapImage: Bitmap) {
        _currentPhoto.value = bitmapImage
    }

}