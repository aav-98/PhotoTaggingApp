package com.example.photosapp.ui.login

import com.example.photosapp.data.model.LoggedInUser
import java.io.IOException

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
    val success: LoggedInUser? = null,
    val error: IOException? = null,
    val loggedOut: Int? = null
)