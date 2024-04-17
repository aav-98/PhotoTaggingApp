package com.example.photosapp


import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.example.photosapp.databinding.FragmentPreviewBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.io.IOException

import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import java.io.ByteArrayOutputStream


class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private val args: PreviewFragmentArgs by navArgs()
    private val TAG = javaClass.simpleName

    private var imageBitMap: Bitmap? = null
    private var photoChange = false
    private var position = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.mode == "new") {
            Uri.parse(args.photoURI)?.let { uri ->
                imageBitMap = uriToBitmap(uri)
                binding.imageView.setImageBitmap(imageBitMap)
                getPhotoLocation(uri)
            }
        } else {
            binding.submitButton.text = "Submit changes"
            photoViewModel.currentPhotoDetails.value?.let {photoDetails ->
                if (photoDetails.people != "") {
                    photoDetails.people.split(",").forEach { name ->
                        addChipToGroup(name)
                    }   }
                position = photoDetails.id
                binding.imageDescriptionEditText.setText(photoDetails.description)
                binding.locationView.text = photoDetails.location
                binding.imageView.setImageBitmap(photoDetails.photoBitmap)
                imageBitMap = photoDetails.photoBitmap

            }
        }

        binding.nameEditText.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                event?.action == KeyEvent.ACTION_DOWN &&
                event.keyCode == KeyEvent.KEYCODE_ENTER) {
                addChipToGroup(textView.text.toString())
                textView.text = ""
                true
            } else {
                false
            }
        }

        binding.addNameButton.setOnClickListener {
            addChipToGroup(binding.nameEditText.text.toString())
            binding.nameEditText.text = null
        }


        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageBitMap = uriToBitmap(it)
                binding.imageView.setImageBitmap(imageBitMap)
                getPhotoLocation(it)
                photoChange = true
            }
        }

        binding.replacePhotoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        /*
        //TODO: Handle the logic of editing a photo
        binding.editPhotoButton.setOnClickListener {

        }
        */

        binding.submitButton.setOnClickListener {
            imageBitMap?. let { bitmap ->
                val imageBase64 = bitmapToBase64(bitmap)

                val description = binding.imageDescriptionEditText.text.toString()
                val location = binding.locationView.text.toString()
                val people = binding.namesChipGroup.children
                    .drop(1) //Placeholder chip
                    .map { it as Chip }
                    .joinToString(", ") { it.text.toString() }

                if (args.mode == "new") {
                    photoViewModel.publishPost(imageBase64, description, location, people)
                }
                else {
                    if (photoChange) photoViewModel.updatePost(position, description, imageBase64, location, people)
                    else photoViewModel.updatePost(position, description, "", location, people)
                }
            }
            findNavController().navigate(R.id.action_PreviewFragment_to_HomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addChipToGroup(chipText: String) {
        val chip = Chip(context).apply {
            text = chipText
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                (parent as? ChipGroup)?.removeView(this)
                updatePlaceholderVisibility()
            }
        }
        binding.namesChipGroup.addView(chip)
        updatePlaceholderVisibility()
    }

    private fun updatePlaceholderVisibility() {
        val chipCount = binding.namesChipGroup.childCount - 1
        if (chipCount > 0) {
            binding.placeholderChip.visibility = View.GONE
        } else {
            binding.placeholderChip.visibility = View.INVISIBLE
        }
    }

    private fun getPhotoLocation(photoUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLong = exifInterface?.latLong

            if (latLong != null) {
                val lat = String.format("%.3f", latLong[0])
                val long = String.format("%.3f", latLong[1])
                binding.locationView.text = "Latitude: $lat, Longitude: $long"
            } else {
                binding.locationView.text = "No location information available"
                Log.d("Location", "This photo does not have GPS info.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun uriToBitmap(photoUri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Bitmap", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}