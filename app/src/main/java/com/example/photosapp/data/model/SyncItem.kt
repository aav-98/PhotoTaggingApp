package com.example.photosapp.data.model

/**
 * Data class that holds the id (of a tag) and what method (publishing new post, updating post)
 * was attempted but failed for this id
 */
data class SyncItem(
    val id: Int,
    val operation: SyncOperation
)

enum class SyncOperation {
    NEW, UPDATE, UPDATE_TAGS_OR_DELETE
}
