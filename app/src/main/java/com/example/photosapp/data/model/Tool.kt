package com.example.photosapp.data.model

/**
 * Data class that captures information about the tool used to edit a photo in the editPhotoFragment
 */
data class Tool(
    val name: String,
    val iconResId: Int,
    val toolType: ToolType
)

enum class ToolType {
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    CROP,
    ROTATE
}

