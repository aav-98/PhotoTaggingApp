package com.example.photosapp.data.model

/**
 * Data class that captures the tags associated with a photo to properly store the photo on server-side
 */
data class Tags(
    val id: String,
    var numberOfTags: String,
    val tagId: MutableList<String>,
    val tagDes: MutableList<String>,
    val tagPhoto: MutableList<String>,
    val tagLocation: MutableList<String>,
    val tagPeopleName: MutableList<String>
)