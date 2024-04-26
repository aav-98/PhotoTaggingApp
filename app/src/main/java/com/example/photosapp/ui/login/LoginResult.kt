package com.example.photosapp.ui.login

import com.example.photosapp.data.model.LoggedInUser
import java.io.IOException

/**
 * Result : success (user details) or error message or user logged out
 */
data class LoginResult(
    val success: LoggedInUser? = null,
    val error: IOException? = null,
    val loggedOut: Int? = null
)