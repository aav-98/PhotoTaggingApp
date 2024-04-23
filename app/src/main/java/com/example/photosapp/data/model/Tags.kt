package com.example.photosapp.data.model

/**
 * Data class that captures the tags associated with a photo to properly store the photo on server-side
 */
data class Tags(
    val id: String,
    val numberOfTags: String,
    val tagId: List<String>,
    val tagDes: List<String>,
    val tagPhoto: List<String>,
    val tagLocation: List<String>,
    val tagPeopleName: List<String>
)