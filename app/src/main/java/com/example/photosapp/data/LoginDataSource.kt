package com.example.photoapp.data

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.photosapp.MainActivity
import com.example.photosapp.R
import com.example.photosapp.data.model.LoggedInUser
import com.google.gson.Gson
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import com.example.photosapp.data.Result

/**
 * Interface that will be used for callbacks when the login request is completed
 */
interface LoginResultCallback {
    fun onResult(result: Result<LoggedInUser>)
}

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource(context: Context) {

    private val appContext = context.applicationContext

    private val TAG = javaClass.simpleName

    fun login(username: String, password: String, callback: LoginResultCallback) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/methodPostRemoteLogin"

        val hashedPassword = md5(password)

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                Log.d(TAG, response)
                if (response != "") {
                    val gson = Gson()
                    val user: LoggedInUser = gson.fromJson(response, LoggedInUser::class.java)
                    callback.onResult(Result.Success(user))
                } else {
                    callback.onResult(Result.Error(IOException("Error logging in")))
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Login failed: ${error.message}")
                callback.onResult(Result.Error(IOException("Error logging in", error)))
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
        val sharedPreferences = appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
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
}