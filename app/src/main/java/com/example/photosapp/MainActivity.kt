package com.example.photosapp

import android.content.Context
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.photosapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (isLoggedIn()) {
            navController.navigate(R.id.action_global_HomeFragment)
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
        val sharedPreferences = getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
        val loggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val user_id = sharedPreferences.getString("USER ID: ", "no idea")
        Log.d("SHAREDP", loggedIn.toString())
        Log.d("SHAREDP", user_id.toString())
        return loggedIn
    }

    private fun navigateToPreviewFragment(imageUri: Uri) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val action = HomeFragmentDirections.actionHomeFragmentToPreviewFragment(imageUri.toString())
        navController.navigate(action)
    }
}