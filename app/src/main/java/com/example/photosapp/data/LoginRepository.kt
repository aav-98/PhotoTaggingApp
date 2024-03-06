package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.photoapp.data.LoginDataSource
import com.example.photoapp.data.LoginResultCallback
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.data.Result

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource, appContext: Context) {

    private val TAG = javaClass.simpleName

    private val context = appContext.applicationContext

    // shared preferences file to store user information
    private val sharedPref: SharedPreferences

    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
        sharedPref = context.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    val loginResult = MutableLiveData<Result<LoggedInUser>>()

    fun login(username: String, password: String): LiveData<Result<LoggedInUser>> {
        // handle login
        dataSource.login(username, password, object: LoginResultCallback {
            override fun onResult(result: Result<LoggedInUser>) {
                if (result is Result.Success) {
                    setLoggedInUser(result.data)
                    Log.d(TAG, user.toString())
                }
                loginResult.postValue(result)
            }
        })
        return loginResult
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        with (sharedPref.edit()) {
            putBoolean(context.getString(R.string.is_logged_in), true)
            putString(context.getString(R.string.user_id_key), loggedInUser.id)
            putString(context.getString(R.string.first_name_key), loggedInUser.firstName)
            putString(context.getString(R.string.last_name_key), loggedInUser.lastName)
            putString(context.getString(R.string.passHash_key), loggedInUser.passHash)
            putString(context.getString(R.string.email_key), loggedInUser.email)
            putString(context.getString(R.string.living_city_key), loggedInUser.livingCity)
            putString(context.getString(R.string.year_of_birth_key), loggedInUser.yearOfBirth)
        }
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}