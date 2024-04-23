package com.example.photosapp


import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import com.example.photosapp.data.model.PhotoDetails
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayOutputStream

//TODO: Find out how to make the app scroll/pan to the right edit box when keyboard is opened
//TODO: Should the user be able to edit the Lat long to be what they want it to be?

class PreviewFragment : BaseMapFragment() {

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
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        mapView = _binding?.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.mode == "new") {
            Log.d(TAG, "er jeg new")
            Uri.parse(args.photoURI)?.let { uri ->
                imageBitMap = uriToBitmap(uri)
                binding.imageView.setImageBitmap(imageBitMap)
                photoViewModel.setCurrentPhoto(imageBitMap as Bitmap)
                getPhotoLocation(uri)
            }
        }
        else {
            if (args.mode != "newEdit") binding.submitButton.text = getString(R.string.submit_changes_button_text)
            if (args.mode == "editEdit") photoChange = true
            Log.d(TAG, "er jeg gammel")
            photoViewModel.currentPhotoDetails.value?.let {photoDetails ->
                if (photoDetails.people != "") {
                    photoDetails.people.split(",").forEach { name ->
                        addChipToGroup(name)
                    }   }
                position = photoDetails.id
                binding.imageDescriptionEditText.setText(photoDetails.description)
                binding.locationView.text = photoDetails.location
                val locationParts = photoDetails.location.split(",")
                if (locationParts.size == 2) {
                    try {
                        val lat = locationParts[0].toDouble()
                        val lng = locationParts[1].toDouble()
                        queueLocationUpdate(LatLng(lat, lng))
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Invalid location format", e)
                    }
                }
                Log.d(TAG, "KOM DU HIT nr1")
                photoViewModel.currentPhoto.value?.let { bitmap ->
                    Log.d(TAG, "KOM DU HIT nr2")
                    binding.imageView.setImageBitmap(bitmap)
                    imageBitMap = bitmap
                }
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

        binding.editPhotoButton.setOnClickListener {
            val description = binding.imageDescriptionEditText.text.toString()
            val location = binding.locationView.text.toString()
            val people = binding.namesChipGroup.children
                .map { it as Chip }
                .joinToString(", ") { it.text.toString() }

            photoViewModel.setCurrentPhotoDetails(PhotoDetails(position.toString(), description, location, people))

            photoViewModel.setCurrentPhoto(imageBitMap as Bitmap)
            val action = PreviewFragmentDirections.actionPreviewFragmentToEditPhotoFragment(args.mode)
            findNavController().navigate(action)
        }

        binding.submitButton.setOnClickListener {
            imageBitMap?. let { bitmap ->
                val imageBase64 = bitmapToBase64(bitmap)

                val description = binding.imageDescriptionEditText.text.toString()
                val location = binding.locationView.text.toString()
                val people = binding.namesChipGroup.children
                    .map { it as Chip }
                    .joinToString(", ") { it.text.toString() }

                if (args.mode == "new" || args.mode == "newEdit") {
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
            }
        }
        binding.namesChipGroup.addView(chip)
    }


    private fun getPhotoLocation(photoUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLong = exifInterface?.latLong

            if (latLong != null) {
                val lat = latLong[0]
                val long = latLong[1]
                val locationText = String.format("%.3f", lat) + ", " + String.format("%.3f", long)
                binding.locationView.text = locationText
                queueLocationUpdate(LatLng(lat, long))
            } else {
                mapView?.visibility = View.GONE
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