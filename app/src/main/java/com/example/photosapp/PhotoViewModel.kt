package com.example.photosapp

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photosapp.data.PhotoRepository
import com.example.photosapp.data.model.PhotoDetails
import com.example.photosapp.data.model.Tags

/**
 * ViewModel responsible for managing data related to photos in the application.
 *
 * This ViewModel interacts with [PhotoRepository] to fetch and update photo-related data, and
 * provides LiveData objects that the ui elements can observe.
 *
 * @property tagsLiveData LiveData object for observing changes in tags data.
 * @property photoLiveData LiveData object for observing changes in photo data.
 * @property currentPhotoDetails LiveData object for observing changes in current photo details.
 * @property currentPhoto LiveData object for observing changes in the currently displayed photo.
 * @property editedPhoto LiveData object for observing changes in the edited photo.
 */
class PhotoViewModel(private val photoRepository: PhotoRepository) : ViewModel() {

    val tagsLiveData: LiveData<Tags?> = photoRepository.tagsLiveData
    val photoLiveData: LiveData<Map<String, String>> = photoRepository.photoLiveData

    private val _photoDetails = MutableLiveData<PhotoDetails>()
    val currentPhotoDetails: LiveData<PhotoDetails> = _photoDetails

    private val _currentPhoto = MutableLiveData<Bitmap>()
    val currentPhoto: LiveData<Bitmap> = _currentPhoto

    private val _editedPhoto = MutableLiveData<Bitmap?>()
    val editedPhoto: LiveData<Bitmap?> = _editedPhoto

    /**
     * Loads the tagsLiveData by calling the getTags() method in the repository.
     */
    fun loadTags() {
        photoRepository.getTags()
    }
    /**
     * Loads the photoLiveData by calling the getTags() method in the repository.
     */
    fun loadPhotos() {
        photoRepository.getPhotos()
    }
    /**
     * Forwards the publishing of a new photo to the photo repository
     */
    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.publishPost(imageBase64, newTagDes, newTagLoc, newTagPeopleName)
    }
    /**
     * Forwards the updating of a post/photo to the photo repository
     */
    fun updatePost(position: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String) {
        photoRepository.updatePost(position, newTagDes, newTagPho, newTagLoc, newTagPeopleName)
    }

    /**
     * Sets the current photo details.
     * @param photoDetail The details of the current photo.
     */
    fun setCurrentPhotoDetails(photoDetail : PhotoDetails) {
        _photoDetails.value = photoDetail
    }

    /**
     * Sets the current photo.
     * @param bitmapImage The current photos bitmap
     */
    fun setCurrentPhoto(bitmapImage: Bitmap) {
        _currentPhoto.value = bitmapImage
    }

    /**
     * Sets the edited photo
     * @param bitmapImage The edited photos bitmap
     */
    fun setEditedPhoto(bitmapImage: Bitmap) {
        _editedPhoto.value = bitmapImage
    }
}