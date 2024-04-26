package com.example.photosapp.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.photosapp.R
import com.example.photosapp.data.model.SyncItem
import com.example.photosapp.data.model.SyncOperation
import com.example.photosapp.data.model.Tags
import com.google.gson.Gson
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class PhotoRepository(context: Context) {
    private var periodicTaskScheduled = false
    private var executorService : ScheduledExecutorService? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("OFFLINE_MODE", "NETWORK IS AVAILABLE")
                Log.d(TAG, "Network is available")
                if (loadUnsyncedPost().isNotEmpty()) {
                    isServerReachable { reachable ->
                        if (reachable) {
                            retryUnsynchedPosts()
                        } else {
                            Log.d("OFFLINE_MODE", "BUT THE SERVER IS NOT")
                            if (!periodicTaskScheduled) startPeriodicServerCheck()
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("OFFLINE_MODE", "NETWORK IS LOST, starting periodic server check...")
                Log.d(TAG, "Network is lost")
                if (periodicTaskScheduled) stopPeriodicServerCheck()
            }
        })
    }

    private fun isServerReachable(onComplete: (Boolean) -> Unit) {
        val queue = Volley.newRequestQueue(appContext)
        val url = "http://10.0.2.2:8080/getMethodTesting"

        val stringRequest = object : StringRequest(
            Method.GET, url,
            Response.Listener<String> { response ->
                onComplete(true)
                Log.d(TAG, "The server is online")
            },
            Response.ErrorListener { error ->
                if (error.cause is java.net.ConnectException) {
                    onComplete(false)
                    Log.d(TAG, "The server is offline")
                }
            }) {}
        queue.add(stringRequest)
    }

    private fun startPeriodicServerCheck() {
        Log.d("OFFLINE_MODE", "Started the periodic server check")
        periodicTaskScheduled = true
        executorService = Executors.newSingleThreadScheduledExecutor()
        val periodicTask = Runnable {
            Log.d("OFFLINE_MODE", "EXECUTING A SERVER CHECK")
            if (isNetworkAvailable()) {
                isServerReachable { reachable ->
                    if (reachable) {
                        retryUnsynchedPosts()
                    }
                }
            }
        }
        executorService?.scheduleAtFixedRate(periodicTask, 0, 10, TimeUnit.SECONDS)
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun stopPeriodicServerCheck() {
        Log.d("OFFLINE_MODE", "Stopping the periodic server check")
        executorService?.shutdownNow()
        executorService = null
        periodicTaskScheduled = false
    }




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
        //I need to get the tags from shared preferences if there are any
        val savedTags = loadTagsFromSharedPref()
        if (savedTags != null) {
            tagsLiveData.postValue(savedTags)
        } else {
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
                            Log.d("MY_TAGS_BRO", "GOT THE TAGS FROM THE SERVER")
                        } else {
                            Toast.makeText(appContext, "Could not load posts", Toast.LENGTH_SHORT).show()
                        }

                    },
                    Response.ErrorListener { error ->
                        Log.d(TAG, "Loading tags from server failed: ${error.message}")
                    }) {}
                queue.add(stringRequest)
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

    //TODO: I need to make this work offline...
    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
        //retryUnsynchedPosts()
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)
        val numberOfTags = tagsLiveData.value?.numberOfTags?.toIntOrNull()

        if (userId == null || numberOfTags == null) {
            Log.d(TAG, "User ID or tag count unavailable.")
            return
        }
        val indexUpdateTag = (findEmptySpaceId().takeIf { it != -1 } ?: numberOfTags)
        val fileName = userId + indexUpdateTag


        uploadPhoto(userId, indexUpdateTag.toString(), fileName, imageBase64,
            onSuccess = {
                addPhoto(fileName, imageBase64) //TODO: SHould i just do this after insertNew tags is a sucess, and not needing to remove it..
                if (indexUpdateTag == numberOfTags) { // Implies a new tag insertion
                    insertNewTags(userId, indexUpdateTag.toString(), newTagDes, fileName, newTagLoc, newTagPeopleName,
                        onSuccess = { offline ->
                            addTag(indexUpdateTag, newTagDes, fileName, newTagLoc, newTagPeopleName)
                            if (offline) addUnsynchedPost(indexUpdateTag, SyncOperation.NEW)
                        },
                        onError = {
                            removePhoto(fileName)
                        })
                } else { // Implies adding the new photos / tags in an existing position (where a photo has been deleted)
                    updateTags(indexUpdateTag.toString(), newTagDes, fileName, newTagLoc, newTagPeopleName,
                        onSuccess = { offline ->
                            addTag(indexUpdateTag, newTagDes, fileName, newTagLoc, newTagPeopleName)
                            if (offline) addUnsynchedPost(indexUpdateTag, SyncOperation.UPDATE)
                        },
                        onError = {
                            removePhoto(fileName)
                        })
                }
            },
            onError = {
                Toast.makeText(appContext, "Could not upload post", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun updatePost(indexUpdateTag: String, updateTagDes: String, updateTagPho: String, updateTagLoc: String, updateTagPeopleName: String) {
        //retryUnsynchedPosts()
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)
        if (userId.isNullOrEmpty() || indexUpdateTag.isEmpty()) {
            Log.d(TAG, "User ID or tag index is missing.")
            return
        }
        val fileName = userId + indexUpdateTag

        when {
            //Only updating tags when the photo has not changed or the post is being deleted
            updateTagPho.isEmpty() || updateTagPho == "na" -> {
                updateTags(indexUpdateTag, updateTagDes, if (updateTagPho == "na") updateTagPho else fileName, updateTagLoc, updateTagPeopleName,
                    onSuccess = {offline ->
                        addTag(indexUpdateTag.toInt(), updateTagDes, if (updateTagPho == "na") updateTagPho else fileName, updateTagLoc, updateTagPeopleName)
                        if (offline) addUnsynchedPost(indexUpdateTag.toInt(), SyncOperation.UPDATE_TAGS_OR_DELETE)
                    },
                    onError = {})
            }
            //Else attempt to upload the new photo and update the tags if that is successful
            else -> {
                uploadPhoto(userId, indexUpdateTag, fileName, updateTagPho,
                    onSuccess = {
                        addPhoto(fileName, updateTagPho)
                        updateTags(indexUpdateTag, updateTagDes, fileName, updateTagLoc, updateTagPeopleName,
                        onSuccess = {offline ->
                            addTag(indexUpdateTag.toInt(), updateTagDes, updateTagPho, updateTagLoc, updateTagPeopleName)
                            if (offline) addUnsynchedPost(indexUpdateTag.toInt(), SyncOperation.UPDATE)
                        },
                        onError = {
                            removePhoto(fileName)
                        }) },
                    onError = { Toast.makeText(appContext, "Could not change post", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
    private fun updateTags(indexUpdateTag: String, updateTagDes: String, updateTagPho: String, updateTagLoc: String, updateTagPeopleName: String, onSuccess: (offline : Boolean) -> Unit, onError: () -> Unit) {
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)
        if (userId != null ) {
            val queue = Volley.newRequestQueue(appContext)

            val url = "http://10.0.2.2:8080/postUpdateTag"

            val stringRequest = object : StringRequest(
                Method.POST, url,
                Response.Listener<String> { response ->
                    if (response.toString() == "OK") {
                        Log.d(TAG, "Successful updating of tags")
                        onSuccess(false)
                    } else {
                        Log.d(TAG, "Unsuccessful updating of tags, removing the photo")
                        onError()
                    }
                },
                Response.ErrorListener { error ->
                    if (error.cause is java.net.ConnectException) {
                        Log.d(TAG, "Error $error: adding the photo to liveData only")
                        onSuccess(true)
                    } else {
                        Log.d(TAG, "Error $error: removing the photo")
                        onError()
                    }
                }) {

                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params["userId"] = userId
                    params["indexUpdateTag"] = indexUpdateTag
                    params["updateTagDes"] = updateTagDes
                    params["updateTagPho"] = updateTagPho
                    params["updateTagLoc"] = updateTagLoc
                    params["updateTagPeopleName"] = updateTagPeopleName
                    return params
                }
            }
            queue.add(stringRequest)
        }
    }

    private fun insertNewTags(userId: String, indexUpdateTag: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String, onSuccess: (offline : Boolean) -> Unit, onError: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/postInsertNewTag"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                if (response.toString() == "OK") {
                    Log.d(TAG, "Successful inserting of new tags")
                    onSuccess(false)
                } else {
                    Log.d(TAG, "Unsuccessful inserting of new tags, removing the photo")
                    onError()
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Insert tags failed: ${error.message}")
                if (error.cause is java.net.ConnectException) {
                    Log.d(TAG, "Error $error: adding the photo to liveData only")
                    onSuccess(true)
                } else {
                    Log.d(TAG, "Error $error: removing the photo")
                    onError()
                }
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
                if (response.toString() == "OK") onSuccess()
                else onError()
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Upload photo failed: ${error.message}")
                if (error.cause is java.net.ConnectException) onSuccess()
                else onError()
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


    private fun findEmptySpaceId() : Int {
        val numberOfTags = tagsLiveData.value?.numberOfTags?.toIntOrNull() ?: 0
        val photoFilenames = tagsLiveData.value?.tagPhoto?.take(numberOfTags)?: listOf()
        return photoFilenames.indexOfFirst { it == "na" }
    }

    private fun addTag(tagId : Int, tagDes: String, tagPho: String, tagLoc: String, tagPeopleName: String) {
        Log.d(TAG, "Adding the photo to the liveData")
        tagsLiveData.value?.let {tags ->
            tags.tagId[tagId] = tagId.toString()
            tags.tagDes[tagId] = tagDes
            tags.tagPhoto[tagId] = tagPho
            tags.tagLocation[tagId] = tagLoc
            tags.tagPeopleName[tagId] = tagPeopleName
            if (tags.numberOfTags.toInt() == tagId) tags.numberOfTags = (tags.numberOfTags.toInt() + 1).toString()
            tagsLiveData.postValue(tags)
            saveTagsInSharedPref(tags)
        }
    }

    private fun addPhoto(fn : String, photoString : String) {
        photoLiveData.value?.let {map ->
            val newMap = map.toMutableMap()
            newMap[fn] = photoString
            savePhotoInSharedPref(fn, photoString)
            photoLiveData.postValue(newMap.toMap())
        }
    }

    private fun removePhoto(fileName: String) {
        val currentPhotos = photoLiveData.value?.toMutableMap() ?: mutableMapOf()
        currentPhotos.remove(fileName)?.let { photoLiveData.postValue(currentPhotos) }
    }

    private fun loadUnsyncedPost() : List<SyncItem> {
        val json = sharedPref.getString("unsynchedPosts", "[]")
        return Gson().fromJson(json, Array<SyncItem>::class.java).toList()

    }

    private fun addUnsynchedPost(id : Int, operation : SyncOperation) {
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        unsynchedPosts.add(SyncItem(id,operation))
        Log.d("OFFLINE_MODE", "Adding the post with id $id to the unsynched posts. PeriodicTaskSceduled $periodicTaskScheduled")
        sharedPref.edit().putString("unsynchedPosts", Gson().toJson(unsynchedPosts)).apply()
        if (!periodicTaskScheduled && isNetworkAvailable()) startPeriodicServerCheck()
    }

    private fun removeSyncedPost(syncItemId: Int) {
        tagsLiveData.value?.let { Log.d("OFFLINE_MODE", it.toString()) }
        Log.d("OFFLINE_MODE", "Success of synching with server: Removing post with id $syncItemId from unsynched posts")
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        unsynchedPosts.removeAll { it.id == syncItemId }
        sharedPref.edit().putString("unsynchedPosts", Gson().toJson(unsynchedPosts)).apply()
    }

    fun retryUnsynchedPosts() {
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        if (unsynchedPosts.isEmpty()) {
            Log.d("OFFLINE_MODE", "There are no unsynched posts to retry")
            return
        }
        retryUnsynchedPost(unsynchedPosts, 0)
    }

    private fun retryUnsynchedPost(posts: List<SyncItem>, index : Int) {
        if (index >= posts.size) {
            if (periodicTaskScheduled) stopPeriodicServerCheck()
            Log.d("OFFLINE_MODE", "All posts have been processed")
            return
        }
        val item = posts[index]
        tagsLiveData.value?.let { tags ->
            val des = tags.tagDes[item.id]
            val fn = tags.tagPhoto[item.id]
            val loc = tags.tagLocation[item.id]
            val people = tags.tagPeopleName[item.id]

            publishUnsyncedPost(item.id.toString(), fn, des, loc, people, item.operation,
                onComplete = {retryUnsynchedPost(posts, index + 1)},
                onOffline = {Log.d("OFFLINE_MODE", "Is offline again not everything was updated..")}
            )
        }
    }


    private fun publishUnsyncedPost(id: String, fn: String, des: String, loc: String, people: String, operation: SyncOperation, onComplete: () -> Unit, onOffline: () -> Unit) {
        Log.d("OFFLINE_MODE", "INSIDE PublishUnsyncedPost FOR id $id and operation $operation")
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null) ?: run {
            Log.d(TAG, "User ID is unavailable.")
            return
        }

        val onSuccessCallback: (Boolean) -> Unit = { offline ->
            if (offline) onOffline()
            if (!offline) removeSyncedPost(id.toInt())
            else Log.d("OFFLINE_MODE", "Could not upload the tags, server still not connected")
            onComplete()
        }
        val onErrorCallback: () -> Unit = {
            Log.d("OFFLINE_MODE", "Some real error has occured when updating the tags")
            removePhoto(fn)
            onComplete()
        }

        if (operation == SyncOperation.UPDATE_TAGS_OR_DELETE) {
            Log.d("OFFLINE_MODE", "UPDATE TAGS OR DELETE: Only trying to update tags. id: $id, fn:$fn")
            updateTags(id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
            return
        }

        photoLiveData.value?.get(fn)?.let { photo ->
            Log.d("OFFLINE_MODE", "Found photo with filename: $fn")
            Log.d("OFFLINE_MODE", "UPDATE OR NEW: Trying to upload photo with fn $fn")
            uploadPhoto(userId, id, fn, photo, onSuccess = {
                Log.d("OFFLINE_MODE", "UPDATE OR NEW: Uploaded photo with fn $fn")
                when (operation) {
                    SyncOperation.NEW -> insertNewTags(userId, id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
                    SyncOperation.UPDATE -> updateTags(id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
                    else -> {} // No operation here, handled by the early return
                }
            }, onError = onErrorCallback)
        } ?: {
            Log.d("OFFLINE_MODE", "No photo found for filename: $fn")
            onComplete()
        }
    }
}