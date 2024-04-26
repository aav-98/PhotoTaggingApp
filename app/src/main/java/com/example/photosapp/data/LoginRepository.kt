package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
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

    var loginResult = MutableLiveData<LoginResult>()
    var user = MutableLiveData<LoggedInUser>()

    private val _changePasswordResult = MutableLiveData<Result<String>>()
    val changePasswordResult: LiveData<Result<String>> = _changePasswordResult

    /**
     * Initiates a login request with the specified username and password.
     * This function interacts with the dataSource to perform the login operation.
     * The result of the login operation is handled by a callback that updates the LiveData
     * loginResult based on the success or failure of the login attempt.
     *
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     *
     * If the login is successful, the user's details are saved to SharedPreferences
     * and the success result is posted to the loginResult LiveData.
     * If the login fails, the error is posted to the loginResult LiveData.
     */
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

    /**
     * Saves the details of a logged-in user to SharedPreferences.
     *
     * @param loggedInUser The LoggedInUser instance containing the user's details.
     *
     * Upon execution, the method also updates a LiveData user with the loggedInUser object,
     * enabling the app's UI to react to changes in user details immediately.
     */
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
                _changePasswordResult.value = result
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

    fun resetChangePasswordResult() {
        _changePasswordResult.value = Result.Empty()
    }
}