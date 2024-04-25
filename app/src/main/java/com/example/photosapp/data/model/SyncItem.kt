package com.example.photosapp.data.model

data class SyncItem(
    val id: Int,
    val operation: SyncOperation
)

enum class SyncOperation {
    NEW, UPDATE, UPDATE_TAGS_OR_DELETE
}
