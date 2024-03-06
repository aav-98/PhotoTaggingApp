package com.example.photosapp.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.photoapp.data.LoginDataSource
import com.example.photoapp.data.LoginResultCallback
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.data.Result

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    private val TAG = javaClass.simpleName

    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
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
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}