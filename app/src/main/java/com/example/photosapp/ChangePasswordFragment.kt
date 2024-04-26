package com.example.photosapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.photosapp.databinding.FragmentChangePasswordBinding
import com.example.photosapp.ui.login.LoginViewModel
import com.example.photosapp.data.Result

/**
 * Fragment responsible for handling password change functionality.
 * Allows users to input a new password and initiates the change password process.
 */
class ChangePasswordFragment : Fragment() {

    private val TAG = javaClass.simpleName

    private var _binding: FragmentChangePasswordBinding? = null

    private val binding get() = _binding!!

    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val passwordEditText = binding.newPassword
        val changePasswordButton = binding.changePasswordButton
        val loadingProgressBar = binding.loading

        loginViewModel.resetChangePasswordResult()

        loginViewModel.newPasswordFormState.observe(viewLifecycleOwner,
            Observer { newPasswordFormState ->
                if (newPasswordFormState == null) {
                    return@Observer
                }
                changePasswordButton.isEnabled = newPasswordFormState.isDataValid
                newPasswordFormState.passwordError?.let {
                    passwordEditText.error = getString(it)
                }
            })


        loginViewModel.changePasswordResult.observe(viewLifecycleOwner, Observer { result ->
            result ?: return@Observer
            loadingProgressBar.visibility = View.GONE

            when (result) {
                is Result.Success -> {
                    updateFragmentWithSuccess()
                    Log.d(TAG, "Change Password Success")
                }
                is Result.Error -> {
                    showChangePasswordFailed()
                }
                else -> {
                    Log.d(TAG, "Change Password Result is empty")}
            }
        })



        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                loginViewModel.newPasswordDataChanged(
                    passwordEditText.text.toString()
                )
            }
        }

        passwordEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.changePassword(
                    passwordEditText.text.toString()
                )
            }
            false
        }

        changePasswordButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            loginViewModel.changePassword(
                passwordEditText.text.toString()
            )
            Log.d(TAG, passwordEditText.text.toString())
        }
    }

    private fun updateFragmentWithSuccess() {
        val welcome = getString(R.string.password_successfully_changed)
        findNavController().navigate(R.id.action_ChangePasswordFragment_to_HomeFragment)
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, welcome, Toast.LENGTH_LONG).show()
    }

    private fun showChangePasswordFailed() {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, getString(R.string.change_password_failed), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}