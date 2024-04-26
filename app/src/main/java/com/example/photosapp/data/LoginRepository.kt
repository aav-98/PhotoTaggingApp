package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.ui.login.LoginResult

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
class LoginRepository(val dataSource: LoginDataSource, context: Context) {

    private val TAG = javaClass.simpleName

    private val appContext = context.applicationContext

    // shared preferences file to store user information
    private val sharedPref: SharedPreferences =
        appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

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
     * If the login is successful, the user's details are saved to SharedPreferences
     * and the success result is posted to the loginResult LiveData.
     * If the login fails, the error is posted to the loginResult LiveData.
     *
     * @param username The username entered by the user.
     * @param password The password entered by the user.
     *
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
     * Upon execution, the method also updates a LiveData `user` with the loggedInUser object,
     * enabling the app's UI to react to changes in user details immediately.
     */
    private fun saveUserDetailsToSharedPreferences(loggedInUser: LoggedInUser) {
        user.postValue(loggedInUser)
        with (sharedPref.edit()) {
            putBoolean(appContext.getString(R.string.is_logged_in), true)
            putString(appContext.getString(R.string.user_id_key), loggedInUser.id)
            putString(appContext.getString(R.string.first_name_key), loggedInUser.firstName)
            putString(appContext.getString(R.string.last_name_key), loggedInUser.lastName)
            putString(appContext.getString(R.string.passHash_key), loggedInUser.passHash)
            putString(appContext.getString(R.string.email_key), loggedInUser.email)
            putString(appContext.getString(R.string.living_city_key), loggedInUser.livingCity)
            putString(appContext.getString(R.string.year_of_birth_key), loggedInUser.yearOfBirth)
            apply()
        }
    }

    /**
     * Continues the logout process by interacting with the dataSource to terminate the user session.
     * This method calls the logout function of the dataSource to ensure that all user session data is cleared.
     * Additionally, it updates the loginResult LiveData to reflect that the user has logged out.
     *
     */
    fun logout() {
        dataSource.logout()
        loginResult.postValue(LoginResult(loggedOut = R.string.log_out))
    }

    /**
     * Requests a password change through the dataSource.
     * This function updates the user's password using a callback to handle the result,
     * which is then posted to the _changePasswordResult LiveData.
     *
     * @param newPassword The new password to be set for the user.
     */
    fun changePassword(newPassword: String) {
        dataSource.changePassword(newPassword, object: ChangePasswordResultCallback {
            override fun onResult(result: Result<String>) {
                _changePasswordResult.value = result
            }
        })
    }

    /**
     * Retrieves user details from SharedPreferences and posts them to the user LiveData.
     * This function fetches stored user attributes such as ID, names, password hash, email,
     * living city, and year of birth, packaging them into a LoggedInUser object which is then
     * shared via LiveData to update the UI or other observers.
     */
    fun getUser() {
        user.postValue(LoggedInUser(
            id = sharedPref.getString(appContext.getString(R.string.user_id_key), appContext.getString(R.string.no_value)),
            firstName = sharedPref.getString(appContext.getString(R.string.first_name_key),  appContext.getString(R.string.no_value)),
            lastName = sharedPref.getString(appContext.getString(R.string.last_name_key),  appContext.getString(R.string.no_value)),
            passHash = sharedPref.getString(appContext.getString(R.string.passHash_key),  appContext.getString(R.string.no_value)),
            email = sharedPref.getString(appContext.getString(R.string.email_key),  appContext.getString(R.string.no_value)),
            livingCity = sharedPref.getString(appContext.getString(R.string.living_city_key),  appContext.getString(R.string.no_value)),
            yearOfBirth = sharedPref.getString(appContext.getString(R.string.year_of_birth_key),  appContext.getString(R.string.no_value))
        ))
    }

    /**
     * Resets the change password result LiveData to an empty state.
     * This function is used to clear any previous password change results from the LiveData,
     * ensuring that observers receive a clean state before new results are posted.
     */
    fun resetChangePasswordResult() {
        _changePasswordResult.value = Result.Empty()
    }
}