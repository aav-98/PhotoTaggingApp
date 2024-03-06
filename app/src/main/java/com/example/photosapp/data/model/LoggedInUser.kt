package com.example.photosapp.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoggedInUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val passClear: String,
    val passHash: String,
    val email: String,
    val livingCity: String,
    val yearOfBirth: String,
)