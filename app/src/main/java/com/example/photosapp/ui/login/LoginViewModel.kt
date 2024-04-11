package com.example.photosapp.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import com.example.photosapp.data.LoginRepository
import com.example.photosapp.data.Result

import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.data.newPasswordFormState
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val TAG = javaClass.simpleName

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _newPasswordForm = MutableLiveData<newPasswordFormState>()
    val newPasswordFormState: LiveData<newPasswordFormState> = _newPasswordForm

    val loginResult: LiveData<LoginResult> = loginRepository.loginResult
    val user : LiveData<LoggedInUser> = loginRepository.user

    val changePasswordResult: LiveData<Result<String>> = loginRepository.changePasswordResult

    fun login(username: String, password: String) {
        Log.d(TAG, "Login started")
        loginRepository.login(username, password)
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    fun logout() {
        Log.d(TAG, "logout initiated in view model")
        _loginForm.value = LoginFormState(isDataValid = false)
        loginRepository.logout()
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 3
    }

    fun changePassword(newPassword: String) {
        loginRepository.changePassword(newPassword)
    }

    fun newPasswordDataChanged(newPassword: String) {
        if (!isPasswordValid(newPassword)) {
            _newPasswordForm.value = newPasswordFormState(passwordError = R.string.invalid_password)
        } else {
            _newPasswordForm.value = newPasswordFormState(isDataValid = true)
        }
    }

    fun loadUser() {
        loginRepository.getUser()
    }
}