package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.photoapp.data.LoginDataSource
import com.example.photoapp.data.LoginResultCallback
import com.example.photoapp.data.ChangePasswordResultCallback
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.ui.login.LoginResult

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource, appContext: Context) {

    private val TAG = javaClass.simpleName

    private val context = appContext.applicationContext

    // shared preferences file to store user information
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    var loginResult = MutableLiveData<LoginResult>()
    var user = MutableLiveData<LoggedInUser>()

    var changePasswordResult = MutableLiveData<Result<String>>()

    fun login(username: String, password: String) {
        dataSource.login(username, password, object: LoginResultCallback {
            override fun onResult(result: LoginResult) {
                if (result.success != null) {
                    loginResult.postValue(LoginResult(success = result.success))
                    saveUserDetailsToSharedPreferences(result.success)
                } else {
                    loginResult.postValue(LoginResult(error = result.error))
                }
            }
        })
    }

    private fun saveUserDetailsToSharedPreferences(loggedInUser: LoggedInUser) {
        user.postValue(loggedInUser)
        with (sharedPref.edit()) {
            putBoolean(context.getString(R.string.is_logged_in), true)
            putString(context.getString(R.string.user_id_key), loggedInUser.id)
            putString(context.getString(R.string.first_name_key), loggedInUser.firstName)
            putString(context.getString(R.string.last_name_key), loggedInUser.lastName)
            putString(context.getString(R.string.passHash_key), loggedInUser.passHash)
            putString(context.getString(R.string.email_key), loggedInUser.email)
            putString(context.getString(R.string.living_city_key), loggedInUser.livingCity)
            putString(context.getString(R.string.year_of_birth_key), loggedInUser.yearOfBirth)
            apply()
        }
    }

    fun logout() {
        dataSource.logout()
        loginResult.postValue(LoginResult(loggedOut = R.string.log_out))
    }
    fun changePassword(newPassword: String) {

        dataSource.changePassword(newPassword, object: ChangePasswordResultCallback {
            override fun onResult(result: Result<String>) {
                if (result is Result.Success) {
                    Log.d(TAG, "Changed password")
                    changePasswordResult.value = result
                } else {
                    changePasswordResult.value = result
                }
            }
        })
    }


    fun getUser() {
        //TODO: update default values
        user.postValue(LoggedInUser(
            id = sharedPref.getString(context.getString(R.string.user_id_key), "id"),
            firstName = sharedPref.getString(context.getString(R.string.first_name_key), "firstName"),
            lastName = sharedPref.getString(context.getString(R.string.last_name_key), "lastName"),
            passHash = sharedPref.getString(context.getString(R.string.passHash_key), "passHash"),
            email = sharedPref.getString(context.getString(R.string.email_key), "email"),
            livingCity = sharedPref.getString(context.getString(R.string.living_city_key), "livingCity"),
            yearOfBirth = sharedPref.getString(context.getString(R.string.year_of_birth_key), "yearOfBirth")
        ))
    }
}