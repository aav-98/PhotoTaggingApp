package com.example.photosapp

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.databinding.ActivityMainBinding
import com.example.photosapp.ui.login.LoginViewModel
import com.example.photosapp.ui.login.LoginViewModelFactory

class MainActivity : AppCompatActivity(), HomeFragment.MainActivityCallback {

    private val TAG = javaClass.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var photoViewModel: PhotoViewModel
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private val maxPhotos = 5 //Server can only have 5 photos per person

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory(this))
            .get(LoginViewModel::class.java)

        photoViewModel = ViewModelProvider(this, PhotoViewModelFactory(this))
            .get(PhotoViewModel::class.java)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (isLoggedIn()) {
            photoViewModel.loadTags()
            loginViewModel.loadUser()
            //Fixme: There is probably a better way to do this?
            photoViewModel.tagsLiveData.observe(this) {
                photoViewModel.loadPhotos()
            }
            navController.navigate(R.id.action_global_HomeFragment)
            Log.d(TAG, "Registered that user was logged in in main activity")
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.HomeFragment) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                binding.addButton.visibility = View.VISIBLE
                binding.profileButton.visibility = View.VISIBLE
            } else if (destination.id == R.id.LoginFragment) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                binding.addButton.visibility = View.GONE
                binding.profileButton.visibility = View.GONE
            }
            else {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                binding.addButton.visibility = View.GONE
                binding.profileButton.visibility = View.GONE
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                navigateToPreviewFragment(it)
            }
        }

        binding.profileButton.setOnClickListener {
            navController.navigate(R.id.action_HomeFragment_to_ProfileFragment)
        }

        binding.addButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun isLoggedIn(): Boolean {
        //TODO: should the user status be loaded from shared preferences in another file?
        val sharedPreferences = getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
        val loggedIn = sharedPreferences.getBoolean(this.getString(R.string.is_logged_in), false)
        val user_id = sharedPreferences.getString(this.getString(R.string.user_id_key), "no idea")
        Log.d(TAG, loggedIn.toString())
        Log.d(TAG, user_id.toString())
        return loggedIn
    }

    private fun navigateToPreviewFragment(imageUri: Uri) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val action = HomeFragmentDirections.actionHomeFragmentToPreviewFragment(imageUri.toString())
        navController.navigate(action)
    }

    /**
     * Disables/enables the add-button on the action bar based on how many photos have been uploaded and the max photos allowed
     */
    override fun onPhotoCountChanged(count: Int) {
        if (count >= maxPhotos) {
            binding.addButton.setOnClickListener {
                Toast.makeText(applicationContext, "Max number of photos added!", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.addButton.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
    }
}