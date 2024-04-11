package com.example.photoapp.data

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.google.gson.Gson
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import com.example.photosapp.data.Result
import com.example.photosapp.ui.login.LoginResult

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

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val sharedPreferences = appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

    private val TAG = javaClass.simpleName

    private val queue = Volley.newRequestQueue(appContext)

    fun login(username: String, password: String, callback: LoginResultCallback) {

        val url = "http://10.0.2.2:8080/methodPostRemoteLogin"

        val hashedPassword = md5(password)

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                Log.d(TAG, response)
                if (response != "") {
                    Log.d("LOGIN", "API call successful")
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

    fun logout() {
        sharedPreferences.edit().clear().apply()
        with (sharedPreferences.edit()) {
            putBoolean(appContext.getString(com.example.photosapp.R.string.is_logged_in), false)
            apply()
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = BigInteger(1, md.digest(input.toByteArray(Charsets.UTF_8)))
        return String.format("%032x", hash)
    }

    fun changePassword(newPassword: String, callback: ChangePasswordResultCallback) {

        val url = "http://10.0.2.2:8080/methodPostChangePasswd"

        val hashedNewPassword = md5(newPassword)
        val email = sharedPreferences.getString(appContext.getString(R.string.email_key), "default_email").toString()

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
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