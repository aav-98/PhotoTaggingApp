package com.example.photosapp.ui.login

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.photosapp.R
import com.example.photosapp.data.LoginRepository
import com.example.photosapp.data.Result
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.data.newPasswordFormState

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val TAG = javaClass.simpleName

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _newPasswordForm = MutableLiveData<newPasswordFormState>()
    val newPasswordFormState: LiveData<newPasswordFormState> = _newPasswordForm

    val loginResult: LiveData<LoginResult> = loginRepository.loginResult
    val user : LiveData<LoggedInUser> = loginRepository.user

    val changePasswordResult: LiveData<Result<String>> = loginRepository.changePasswordResult

    /**
     * Initiates a login process using provided credentials.
     *
     * @param username The user's username.
     * @param password The user's password.
     */
    fun login(username: String, password: String) {
        loginRepository.login(username, password)
    }

    /**
     * Validates username and password input and updates the login form state accordingly.
     * Sets errors if the username or password are invalid, or marks the form as valid if both are correct.
     *
     * @param username The user's inputted username.
     * @param password The user's inputted password.
     */
    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    /**
     * Performs logout by resetting the login form state and invoking the repository's logout method.
     */
    fun logout() {
        _loginForm.value = LoginFormState(isDataValid = false)
        loginRepository.logout()
    }

    /**
     * Checks if the provided username is valid based on specific criteria.
     * If the username contains an "@", it validates it as an email address.
     * Otherwise, it checks that the username is not blank.
     *
     * @param username The username to validate.
     * @return Boolean indicating whether the username is valid.
     */
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    /**
     * Validates the password based on its length.
     * A valid password must be longer than 3 characters.
     *
     * @param password The password to validate.
     * @return Boolean indicating whether the password is considered valid.
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 3
    }

    fun changePassword(newPassword: String) {
        loginRepository.changePassword(newPassword)
    }

    /**
     * Evaluates the validity of the new password input and updates the form state.
     * If the password is invalid (less than 4 characters), it sets a password error in the form state.
     * If valid, it marks the form as valid.
     *
     * @param newPassword The new password to validate.
     */
    fun newPasswordDataChanged(newPassword: String) {
        if (!isPasswordValid(newPassword)) {
            _newPasswordForm.value = newPasswordFormState(passwordError = R.string.invalid_password)
        } else {
            _newPasswordForm.value = newPasswordFormState(isDataValid = true)
        }
    }

    /**
     * Resets the form state for changing passwords and clears any previous results from the repository.
     * This method resets the local form state to its initial, default values and also instructs
     * the login repository to clear any stored results related to password changes.
     */
    fun resetChangePasswordResult() {
        _newPasswordForm.value = newPasswordFormState()
        loginRepository.resetChangePasswordResult()
    }

    fun loadUser() {
        loginRepository.getUser()
    }
}