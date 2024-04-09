package com.example.photosapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.photosapp.databinding.FragmentProfileBinding
import android.content.SharedPreferences
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.photosapp.ui.login.LoginViewModel
import com.example.photosapp.ui.login.LoginViewModelFactory

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        setUserDetails()
        
        binding.logOutButton.setOnClickListener {
            logout()
            findNavController().navigate(R.id.action_ProfileFragment_to_loginFragment)
        }
        return binding.root

    }

    private fun setUserDetails() {
        val sharedPreferences = activity?.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
        val firstName = sharedPreferences?.getString("first_name", "Default Name")
        val lastName = sharedPreferences?.getString("last_name", "Default Name")
        val cityOfResidence = sharedPreferences?.getString("city_of_residence", "Default City")
        binding.name.text = "$firstName $lastName"
    }

    private fun logout(){
        loginViewModel.logout()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}