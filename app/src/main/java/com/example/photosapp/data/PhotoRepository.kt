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

/**
 * Repository class responsible for managing data operations related to photos,
 * including fetching/storing tags and photos, uploading and updating posts, and handling
 * offline synchronization.
 */
class PhotoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val TAG = javaClass.simpleName
    private val sharedPref: SharedPreferences =
        appContext.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)

    val tagsLiveData = MutableLiveData<Tags?>()
    val photoLiveData = MutableLiveData<Map<String, String>>()
    private val tempPhotoUpdates = mutableMapOf<String, String>()

    private var periodicTaskScheduled = false
    private var executorService : ScheduledExecutorService? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    init {
        registerNetworkCallback()
    }


    //MAIN METHODS ACCESSED BY VIEWMODEL

    /**
     * Retrieves tags associated with the user's account from the server or local storage,
     * and updates the [tagsLiveData] accordingly.
     */
    fun getTags() {
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
                        if (response != null) {
                            val tags: Tags = gson.fromJson(response, Tags::class.java)
                            tagsLiveData.postValue(tags)
                            saveTagsInSharedPref(tags)
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
     * Retrieves photos associated with the user's account based on the available tags,
     * from the server or local storage,
     * and updates the [photoLiveData] accordingly.
     */
    fun getPhotos() {
        val numberOfTags = tagsLiveData.value?.numberOfTags?.toIntOrNull()
        if (numberOfTags != null) {
            val photoFilenames = tagsLiveData.value?.tagPhoto?.take(numberOfTags)?: listOf()

            var downloadsInit = 0
            var downloadsComp = 0

            photoFilenames.forEach { fn ->
                if (fn != "na") {
                    val savedPhoto = loadPhotoFromSharedPref(fn)
                    if (savedPhoto != null) {
                        tempPhotoUpdates[fn] = savedPhoto
                    } else if (isNetworkAvailable()) {
                        downloadsInit++
                        downloadPhoto(fn) {
                            downloadsComp++
                            if (downloadsComp == downloadsInit) {
                                updatePhotoLiveData()
                            }
                        }
                    }
                }
            }
            if (downloadsInit == 0) {
                updatePhotoLiveData()
            }
        }
    }

    /**
     * Publishes a new post to the server, by uploading the photo and the corresponding tags to the server
     *
     * @param imageBase64 The Base64-encoded string representation of the image.
     * @param newTagDes The description of the new tag.
     * @param newTagLoc The location associated with the new tag.
     * @param newTagPeopleName The names of people associated with the new tag.
     */
    fun publishPost(imageBase64: String, newTagDes: String, newTagLoc: String, newTagPeopleName: String) {
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
                addPhoto(fileName, imageBase64)
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

    /**
     * Updates an existing post, by uploading the photo and the corresponding tags to the server
     *
     * @param indexUpdateTag The index of the tag to be updated.
     * @param updateTagDes The updated description of the tag.
     * @param updateTagPho The updated photo associated with the tag.
     * @param updateTagLoc The updated location associated with the tag.
     * @param updateTagPeopleName The updated names of people associated with the tag.
     */
    fun updatePost(indexUpdateTag: String, updateTagDes: String, updateTagPho: String, updateTagLoc: String, updateTagPeopleName: String) {
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
                            addTag(indexUpdateTag.toInt(), updateTagDes, fileName, updateTagLoc, updateTagPeopleName)
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


    //METHODS TO DO NETWORK CALLS TO THE SERVER

    /**
     * Uploads a photo to the server
     */
    private fun uploadPhoto(userId: String, tagId: String, fileName: String, imageBase64: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/postMethodUploadPhoto"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                if (response.toString() == "OK") onSuccess()
                else onError()
            },
            Response.ErrorListener { error ->
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

    /**
     * Uploads new tags to the server
     */
    private fun insertNewTags(userId: String, indexUpdateTag: String, newTagDes: String, newTagPho: String, newTagLoc: String, newTagPeopleName: String, onSuccess: (offline : Boolean) -> Unit, onError: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val url = "http://10.0.2.2:8080/postInsertNewTag"

        val stringRequest = object : StringRequest(
            Method.POST, url,
            Response.Listener<String> { response ->
                if (response.toString() == "OK") {
                    onSuccess(false)
                } else {
                    onError()
                }
            },
            Response.ErrorListener { error ->
                if (error.cause is java.net.ConnectException) {
                    onSuccess(true)
                } else {
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

    /**
     * Updates existing tags in the server
     */
    private fun updateTags(indexUpdateTag: String, updateTagDes: String, updateTagPho: String, updateTagLoc: String, updateTagPeopleName: String, onSuccess: (offline : Boolean) -> Unit, onError: () -> Unit) {
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null)
        if (userId != null ) {
            val queue = Volley.newRequestQueue(appContext)

            val url = "http://10.0.2.2:8080/postUpdateTag"

            val stringRequest = object : StringRequest(
                Method.POST, url,
                Response.Listener<String> { response ->
                    if (response.toString() == "OK") {
                        onSuccess(false)
                    } else {
                        onError()
                    }
                },
                Response.ErrorListener { error ->
                    if (error.cause is java.net.ConnectException) {
                        onSuccess(true)
                    } else {
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

    /**
     * Downloads a photo from the server.
     */
    private fun downloadPhoto(fileName: String, onCompletion: () -> Unit) {
        val queue = Volley.newRequestQueue(appContext)

        val uri = java.lang.String.format(
            "http://10.0.2.2:8080/getMethodDownloadPhoto?fileName=%1\$s", fileName)

        val stringRequest = object : StringRequest(
            Method.GET, uri,
            Response.Listener<String> { response ->
                if (response != "" && response != null) {
                    tempPhotoUpdates[fileName] = response
                    savePhotoInSharedPref(fileName, response)
                }
                onCompletion()

            },
            Response.ErrorListener { error ->
                onCompletion()
            }) {}
        queue.add(stringRequest)

    }


    //METHODS FOR HANDLING SHARED PREFERENCES

    /**
     * Saves tags information to shared preferences.
     */
    private fun saveTagsInSharedPref(tags: Tags) {
        val gson = Gson()
        val tagsJson = gson.toJson(tags)
        with(sharedPref.edit()) {
            putString("tags", tagsJson)
            apply()
        }
    }

    /**
     * Loads tags information from shared preferences.
     */
    private fun loadTagsFromSharedPref(): Tags? {
        val tagsJson = sharedPref.getString("tags", null)?: return null
        return Gson().fromJson(tagsJson, Tags::class.java)
    }

    /**
     * Saves a photo to shared preferences.
     */
    private fun savePhotoInSharedPref(fileName: String, imageBase64: String) {
        with(sharedPref.edit()) {
            putString(fileName, imageBase64)
            apply()
        }
    }

    /**
     * Loads a photo from shared preferences.
     */
    private fun loadPhotoFromSharedPref(fileName: String): String? {
        return sharedPref.getString(fileName, null)
    }


    //METHODS FOR ADDING TAGS AND PHOTOS TO LIVEDATA

    /**
     * Adds a tag to the [tagsLiveData] and saves it to shared preferences.
     */
    private fun addTag(tagId : Int, tagDes: String, tagPho: String, tagLoc: String, tagPeopleName: String) {
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

    /**
     * Adds a photo to the [photoLiveData] and saves it to shared preferences.
     */
    private fun addPhoto(fn : String, photoString : String) {
        photoLiveData.value?.let {map ->
            val newMap = map.toMutableMap()
            newMap[fn] = photoString
            savePhotoInSharedPref(fn, photoString)
            photoLiveData.postValue(newMap.toMap())
        }
    }

    /**
     * Removes a photo from [photoLiveData] and shared preferences.
     */
    private fun removePhoto(fileName: String) {
        val currentPhotos = photoLiveData.value?.toMutableMap() ?: mutableMapOf()
        currentPhotos.remove(fileName)?.let { photoLiveData.postValue(currentPhotos) }
    }

    /**
     * Updates [photoLiveData] with temporary photo updates.
     */
    private fun updatePhotoLiveData() {
        photoLiveData.postValue(tempPhotoUpdates.toMap())
    }

    /**
     * Finds the index of an empty space in the tag list.
     */
    private fun findEmptySpaceId() : Int {
        val numberOfTags = tagsLiveData.value?.numberOfTags?.toIntOrNull() ?: 0
        val photoFilenames = tagsLiveData.value?.tagPhoto?.take(numberOfTags)?: listOf()
        return photoFilenames.indexOfFirst { it == "na" }
    }


    //METHODS TO HANDLE UNSYNCHED POSTS

    /**
     * Loads unsynchronized posts from shared preferences.
     */
    private fun loadUnsyncedPost() : List<SyncItem> {
        val json = sharedPref.getString("unsynchedPosts", "[]")
        return Gson().fromJson(json, Array<SyncItem>::class.java).toList()

    }

    /**
     * Adds a post to unsynchronized posts in shared preferences.
     */
    private fun addUnsynchedPost(id : Int, operation : SyncOperation) {
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        unsynchedPosts.add(SyncItem(id,operation))
        sharedPref.edit().putString("unsynchedPosts", Gson().toJson(unsynchedPosts)).apply()
        if (!periodicTaskScheduled && isNetworkAvailable()) startPeriodicServerCheck()
    }

    /**
     * Removes a post to unsynchronized posts in shared preferences.
     */
    private fun removeSyncedPost(syncItemId: Int) {
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        unsynchedPosts.removeAll { it.id == syncItemId }
        sharedPref.edit().putString("unsynchedPosts", Gson().toJson(unsynchedPosts)).apply()
    }

    /**
     * Loads in the unsynchronized posts and start the synchronization
     */
    fun retryUnsynchedPosts() {
        val unsynchedPosts = loadUnsyncedPost().toMutableList()
        if (unsynchedPosts.isEmpty()) return
        retryUnsynchedPost(unsynchedPosts, 0)
    }

    /**
     * Retries a unsynchronized post at a specified index
     */
    private fun retryUnsynchedPost(posts: List<SyncItem>, index : Int) {
        if (index >= posts.size) {
            if (periodicTaskScheduled) stopPeriodicServerCheck()
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
                onOffline = {Log.d(TAG, "Could not synch everything")}
            )
        }
    }

    /**
     * Publishes an unsynchronized post to the server
     */
    private fun publishUnsyncedPost(id: String, fn: String, des: String, loc: String, people: String, operation: SyncOperation, onComplete: () -> Unit, onOffline: () -> Unit) {
        val userId = sharedPref.getString(appContext.getString(R.string.user_id_key), null) ?: run {
            Log.d(TAG, "User ID is unavailable.")
            return
        }

        val onSuccessCallback: (Boolean) -> Unit = { offline ->
            if (offline) onOffline()
            if (!offline) removeSyncedPost(id.toInt())
            onComplete()
        }
        val onErrorCallback: () -> Unit = {
            removePhoto(fn)
            onComplete()
        }

        if (operation == SyncOperation.UPDATE_TAGS_OR_DELETE) {
            updateTags(id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
            return
        }

        photoLiveData.value?.get(fn)?.let { photo ->
            uploadPhoto(userId, id, fn, photo, onSuccess = {
                when (operation) {
                    SyncOperation.NEW -> insertNewTags(userId, id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
                    SyncOperation.UPDATE -> updateTags(id, des, fn, loc, people, onSuccessCallback, onErrorCallback)
                    else -> {} // No operation here, handled by the early return
                }
            }, onError = onErrorCallback)
        } ?: {
            onComplete()
        }
    }


    // METHODS FOR HANDLING OFFLINE-MODE: CHECKING NETWORK/SERVER CONNECTION

    /**
     * Registers a network callback to monitor network availability.
     */
    private fun registerNetworkCallback() {
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (loadUnsyncedPost().isNotEmpty()) {
                    isServerReachable { reachable ->
                        if (reachable) {
                            retryUnsynchedPosts()
                        } else {
                            if (!periodicTaskScheduled) startPeriodicServerCheck()
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (periodicTaskScheduled) stopPeriodicServerCheck()
            }
        })
    }

    /**
     * Checks if the network is available.
     */
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Checks if the server is reachable.
     */
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

    /**
     * Starts a periodic check to determine server reachability.
     */
    private fun startPeriodicServerCheck() {
        periodicTaskScheduled = true
        executorService = Executors.newSingleThreadScheduledExecutor()
        val periodicTask = Runnable {
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

    /**
     * Stops the periodic server check.
     */
    fun stopPeriodicServerCheck() {
        executorService?.shutdownNow()
        executorService = null
        periodicTaskScheduled = false
    }
}

