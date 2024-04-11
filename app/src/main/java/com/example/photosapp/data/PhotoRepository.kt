package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.photosapp.R
import com.example.photosapp.data.model.Tags
import com.google.gson.Gson


class PhotoRepository(context: Context) {

    private val appContext = context.applicationContext
    private val TAG = javaClass.simpleName

    private val sharedPref: SharedPreferences =
        appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

    val tagsLiveData = MutableLiveData<Tags?>()
    val photoLiveData = MutableLiveData<Map<String, String>>()
    private val tempPhotoUpdates = mutableMapOf<String, String>()

    /**
     * If network is available, method tries to fetch the tags from the server, if it is successful this is posted to tagsLiveData,
     * if it fails the tags are fetched from shared preferences.
     * If the network is unavailable the method directly fetches the tags from shared pref.
     */
    fun getTags() {
        if (isNetworkAvailable()) {
            Log.d(TAG, "Network available, trying to fetch tags from server")
            val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)

            if (userId != null) {
                val queue = Volley.newRequestQueue(appContext)

                val uri = java.lang.String.format("http://10.0.2.2:8080/getMethodMyTags?id=%1\$s", userId)

                val stringRequest = object : StringRequest(
                    Method.GET, uri,
                    Response.Listener<String> { response ->
                        val gson = Gson()
                        Log.d(TAG, "Response: $response")
                        if (response != null) {
                            val tags: Tags = gson.fromJson(response, Tags::class.java)
                            Log.d(TAG, "Tags fetched: $tags")
                            tagsLiveData.postValue(tags)
                            saveTagsInSharedPref(tags)
                        } else {
                            Toast.makeText(appContext, "Could not load posts", Toast.LENGTH_SHORT).show()
                        }

                    },
                    Response.ErrorListener { error ->
                        Log.e(TAG, "Get tags failed: ${error.message}")
                        val savedTags = loadTagsFromSharedPref()
                        if (savedTags != null) tagsLiveData.postValue(savedTags)

                    }) {}
                queue.add(stringRequest)
            }

        } else {
            Toast.makeText(appContext, "Network is unavailable", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Network is not available, trying to get saved tags")
            val savedTags = loadTagsFromSharedPref()
            if (savedTags != null) {
                tagsLiveData.postValue(savedTags)
            } else {
                Log.e(TAG, "No internet connection and no saved tags found.")
            }
        }
    }

    /**
     * Populates photoLiveData by going through all the photo filenames from tagsLiveData and fetching the corresponding pictures.
     * It initially tries to fetch the photo from shared preferences, but if its unavailable the photo is downloaded from the server.
     * @see downloadPhoto
     */
    fun getPhotos() {
        val numberOfTags = tagsLiveData.value?.numberOfTags?.toIntOrNull()
        if (numberOfTags != null) {
            val photoFilenames = tagsLiveData.value?.tagPhoto?.take(numberOfTags)?: listOf()
            Log.d(TAG, "getPhotos(): Filenames to find $photoFilenames")

            var downloadsInit = 0
            var downloadsComp = 0

            photoFilenames.forEach { fn ->
                if (fn != "na") {
                    val savedPhoto = loadPhotoFromSharedPref(fn)
                    if (savedPhoto != null) {
                        tempPhotoUpdates[fn] = savedPhoto
                        Log.d(TAG, "getPhotos(): Loaded image $fn from shared preferences")
                    } else if (isNetworkAvailable()) {
                        downloadsInit++
                        downloadPhoto(fn) {
                            downloadsComp++
                            if (downloadsComp == downloadsInit) {
                                updatePhotoLiveData()
                            }
                        }
                        Log.d(TAG, "getPhotos(): Loaded image $fn from server")
                    }
                }
            }
            if (downloadsInit == 0) {
                updatePhotoLiveData()
            }
        }
    }

    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
        val indexUpdateTag = tagsLiveData.value?.numberOfTags
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)

        if (indexUpdateTag != null && userId != null) {
            val fileName = userId + indexUpdateTag

            uploadPhoto(userId = userId, tagId = indexUpdateTag, fileName=fileName, imageBase64 = imageBase64,
                onSuccess = {
                    insertNewTags(userId, indexUpdateTag, newTagDes, newTagPho=fileName, newTagLoc, newTagPeopleName,
                        onSuccess = { getTags() },
                        onError = { removePhoto(fileName)})
                },
                onError = {
                    Toast.makeText(appContext, "Could not upload post", Toast.LENGTH_SHORT).show()
                })
        }
    }

    private fun insertNewTags(userId: String, indexUpdateTag: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/postInsertNewTag"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                Log.d(TAG, "Insert tags response: $response")
                if (response.toString() == "OK") onSuccess()
                else onError()
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Insert tags failed: ${error.message}")
                onError()
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["userId"] = userId
                params["indexUpdateTag"] = indexUpdateTag
                params["newTagDes"] = newTagDes
                params["newTagPho"] = newTagPho
                params["newTagLoc"] = newTagLoc
                params["newTagPeopleName"] = newTagPeopleName
                return params
            }
        }
        queue.add(stringRequest)


    }

    private fun uploadPhoto(userId: String, tagId: String, fileName: String, imageBase64: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/postMethodUploadPhoto"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                Log.d(TAG, "Upload photo response: $response")
                if (response.toString() == "OK") {
                    savePhotoInSharedPref(fileName, imageBase64)
                    onSuccess()
                }
                else onError()
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Upload photo failed: ${error.message}")
                onError()
            }) {

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["userId"] = userId
                params["tagId"] = tagId
                params["fileName"] = fileName
                params["imageStringBase64"] = imageBase64
                return params
            }
        }
        queue.add(stringRequest)
    }

    private fun downloadPhoto(fileName: String, onCompletion: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val uri = java.lang.String.format(
            "http://10.0.2.2:8080/getMethodDownloadPhoto?fileName=%1\$s", fileName)

        val stringRequest = object : StringRequest(
            Method.GET, uri,
            Response.Listener<String> { response ->
                if (response != "" && response != null) {
                    Log.d(TAG, "Download of photo with filename $fileName SUCCESS")
                    tempPhotoUpdates[fileName] = response
                    savePhotoInSharedPref(fileName, response)
                }
                else Log.d(TAG, "Download of photo with filename $fileName FAILED")
                onCompletion()

            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Download photo failed: ${error.message}")
                onCompletion()
            }) {}
        queue.add(stringRequest)

    }
    private fun updatePhotoLiveData() {
        photoLiveData.postValue(tempPhotoUpdates.toMap())
    }
    private fun saveTagsInSharedPref(tags: Tags) {
        val gson = Gson()
        val tagsJson = gson.toJson(tags)
        with(sharedPref.edit()) {
            putString("tags", tagsJson)
            apply()
        }
    }

    private fun loadTagsFromSharedPref(): Tags? {
        val tagsJson = sharedPref.getString("tags", null)?: return null
        return Gson().fromJson(tagsJson, Tags::class.java)
    }

    private fun savePhotoInSharedPref(fileName: String, imageBase64: String) {
        with(sharedPref.edit()) {
            putString(fileName, imageBase64)
            apply()
        }
    }

    private fun loadPhotoFromSharedPref(fileName: String): String? {
        return sharedPref.getString(fileName, null)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun removePhoto(fileName: String) {
        //I don't think i need this anymore
        /*
        with(sharedPref.edit()) {
            remove(fileName)
            apply()
        }
         */
        val currentPhotos = photoLiveData.value?.toMutableMap() ?: mutableMapOf()
        currentPhotos.remove(fileName)
        photoLiveData.postValue(currentPhotos)
    }



}