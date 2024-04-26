package com.example.photosapp.data

/**
 * Data validation state of the change password form.
 */
data class newPasswordFormState(
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)
