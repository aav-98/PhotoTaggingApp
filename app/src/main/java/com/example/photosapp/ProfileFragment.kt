package com.example.photosapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.photosapp.data.model.LoggedInUser
import com.example.photosapp.databinding.FragmentProfileBinding
import com.example.photosapp.ui.login.LoginViewModel

/**
 * Fragment responsible for displaying user profile information and providing options to change password or log out.
 */
class ProfileFragment : Fragment() {

    private val TAG = javaClass.simpleName

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        

        binding.changePwdButton.setOnClickListener {
            findNavController().navigate(R.id.action_ProfileFragment_to_ChangePasswordFragment)
        }

        binding.logOutButton.setOnClickListener {
            loginViewModel.logout()
        }

        loginViewModel.user.observe(viewLifecycleOwner) { user ->
            displayUserProfile(user)
        }

        return binding.root

    }

    private fun displayUserProfile(user: LoggedInUser) {
        val name = user.firstName + " " + user.lastName
        binding.name.text = name
        binding.cityOfResidence.text = user.livingCity
        binding.dateOfBirth.text = user.yearOfBirth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel.loginResult.observe(viewLifecycleOwner) { loginResult ->
            loginResult.loggedOut?.let {
                findNavController().navigate(R.id.action_ProfileFragment_to_LoginFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}