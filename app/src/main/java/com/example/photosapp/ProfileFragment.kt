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
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.ui.login.LoginViewModel
import com.example.photosapp.ui.login.LoginViewModelFactory

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ProfileFragment : Fragment() {

    private val TAG = javaClass.simpleName

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()

    val sharedPreferences = activity?.getSharedPreferences("com.example.photosapp.USER_DETAILS", Context.MODE_PRIVATE)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        

        binding.changePwdButton.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_ChangePasswordFragment)
        }

        binding.logOutButton.setOnClickListener {
            Log.d("LOGIN", "log out button clicked")
            loginViewModel.logout()
        }

        loginViewModel.user.observe(viewLifecycleOwner) { user ->
            displayUserProfile(user)
        }

        return binding.root

    }

    private fun displayUserProfile(user: LoggedInUser) {
        Log.d(TAG, "user profile displayed")
        binding.name.text = "${user.firstName} ${user.lastName}"
        binding.cityOfResidence.text = user.livingCity
        binding.dateOfBirth.text = user.yearOfBirth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel.loginResult.observe(viewLifecycleOwner) { loginResult ->
            Log.d(TAG, "Entered login result observer in profile")
            loginResult.loggedOut?.let {
                Log.d(TAG, "User logged out registered in profile fragment")
                findNavController().navigate(R.id.action_ProfileFragment_to_LoginFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}