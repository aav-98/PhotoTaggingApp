package com.example.photosapp.data

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.ui.login.LoginResult
import com.google.gson.Gson
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource(context: Context) {

    private val TAG = javaClass.simpleName

    private val appContext = context.applicationContext

    private val sharedPreferences = appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

    private val queue = Volley.newRequestQueue(appContext)

    /**
     * Initiates a login request to authenticate a user.
     *
     * This function sends a POST request to a predefined server URL to authenticate a user using
     * their username and password. Upon receiving a response, it parses the JSON response into
     * a `LoggedInUser` object if successful, or handles errors accordingly.
     *
     * Error Handling:
     * - On a successful HTTP response containing non-empty content, the JSON is parsed into a `LoggedInUser`.
     * - On empty response or network errors, the callback is invoked with an error.
     *
     * @param username The username of the user attempting to log in.
     * @param password The password of the user, which will be hashed using MD5 before sending.
     * @param callback A callback interface through which results are returned. Results include successful
     *                 user login data or error information in case of failure.
     *
     */
    fun login(username: String, password: String, callback: LoginResultCallback) {

        val url = "http://10.0.2.2:8080/methodPostRemoteLogin"

        val hashedPassword = md5(password)

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Log.d(TAG, "Response from login post request: $response")
                if (response != "") {
                    val gson = Gson()
                    val user: LoggedInUser = gson.fromJson(response, LoggedInUser::class.java)
                    callback.onResult(LoginResult(success = user))
                } else {
                    callback.onResult(LoginResult(error = IOException("Error logging in")))
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Login failed: ${error.message}")
                callback.onResult(LoginResult(error = IOException("Error logging in")))
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["em"] = username
                params["ph"] = hashedPassword
                return params
            }
        }
        queue.add(stringRequest)
    }

    /**
     * Invoked when user chooses to log out of application
     *
     * This function clears the user information and corresponding photos stored in Shared Preferences.
     *
     */
    fun logout() {
        sharedPreferences.edit().clear().apply()
        with (sharedPreferences.edit()) {
            putBoolean(appContext.getString(R.string.is_logged_in), false)
            apply()
        }
    }

    /**
     * This function hashes the cleartext using the MD5 hashing algorithm
     *
     * @param password The password of the user
     *
     */
    private fun md5(password: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = BigInteger(1, md.digest(password.toByteArray(Charsets.UTF_8)))
        return String.format("%032x", hash)
    }

    /**
     * Submits a request to change the user's password on the server.
     *
     * This function sends a POST request to change the password of the user identified by an email
     * stored in SharedPreferences. The function listens for a response to determine whether the
     * password change was successful or if an error occurred.
     *
     * Error Handling:
     * - The server is expected to return "OK" if the password change is successful. Any other response or network errors
     *   will trigger a callback indicating an error.
     *
     * @param newPassword The new password the user wants to set, which will be hashed using MD5 before sending.
     * @param callback A callback interface through which results are returned. Results include
     * success or error information.
     *
     */
    fun changePassword(newPassword: String, callback: ChangePasswordResultCallback) {

        val url = "http://10.0.2.2:8080/methodPostChangePasswd"

        val hashedNewPassword = md5(newPassword)
        val email = sharedPreferences.getString(appContext.getString(R.string.email_key), "default_email").toString()

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Log.d(TAG, response)
                if (response == "OK") {
                    Result.Success(response)
                    callback.onResult(Result.Success(response))
                } else {
                    callback.onResult(Result.Error(IOException("Error changing password")))
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Login failed: ${error.message}")
                callback.onResult(Result.Error(IOException("Error changing password", error)))
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["em"] = email
                params["np"] = newPassword
                params["ph"] = hashedNewPassword
                return params
            }
        }

        queue.add(stringRequest)
    }

}

/**
 * Interface that will be used for callbacks when the login request is completed
 */
interface LoginResultCallback {
    fun onResult(result: LoginResult)
}

/**
 * Interface that will be used for callbacks when the change password request is completed
 */
interface ChangePasswordResultCallback {
    fun onResult(result: Result<String>)
}
