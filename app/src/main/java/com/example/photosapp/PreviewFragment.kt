package com.example.photosapp

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.photosapp.databinding.FragmentPreviewBinding
import java.io.IOException
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.photosapp.data.model.PhotoDetails
import com.google.android.gms.maps.model.LatLng
import java.io.ByteArrayOutputStream
import com.example.photosapp.data.model.Modes

/**
 * Fragment responsible for previewing photo and corresponding tags before submitting new post or changes to an old post.
 * This fragment allows users to view photo details such as description, location, and people names,
 * and to edit these details if necessary before submitting.
 * The fragment also provides functionality for selecting a photo from the device's gallery,
 * replacing the existing photo with the selected one, and submitting the changes.
 */
class PreviewFragment : BaseMapFragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private val TAG = javaClass.simpleName
    private val args: PreviewFragmentArgs by navArgs()

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var photoBitMap: Bitmap? = null
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

        // In the case of: "NEW" mode
        //Showing the user the photo loaded from the uri sent as an argument
        if (args.mode == Modes.NEW) {
            Uri.parse(args.photoURI)?.let { uri ->
                photoBitMap = uriToBitmap(uri)
                binding.imageView.setImageBitmap(photoBitMap)
                photoViewModel.setEditedPhoto(photoBitMap as Bitmap)
                getPhotoLocation(uri)
            }
        }

        // In the case of: "EDIT" mode, or "NEW_WITH_EDITED_PHOTO" mode or "EDIT_WITH_EDITED_PHOTO" mode
        //Showing the user the photo + corresponding tags loaded from view model
        else {
            if (args.mode != Modes.NEW_WITH_EDITED_PHOTO) binding.submitButton.text = getString(R.string.submit_changes_button_text)
            if (args.mode == Modes.EDIT_WITH_EDITED_PHOTO) photoChange = true

            photoViewModel.currentPhotoDetails.value?.let {photoDetails ->
                binding.peopleEditText.setText(photoDetails.people)
                binding.imageDescriptionEditText.setText(photoDetails.description)
                binding.locationView.text = photoDetails.location
                position = photoDetails.id

                val locParts = photoDetails.location.split(",")
                if (locParts.size == 2) {
                    try {
                        queueLocationUpdate(LatLng(locParts[0].toDouble(), locParts[1].toDouble()))
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Invalid location format", e)
                    }
                }
                photoViewModel.editedPhoto.value?.let { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)
                    photoBitMap = bitmap
                }
            }
        }

        //When the user clicks the replace photo button the system photo picker is launched, and a new photo is chosen by the user
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                photoBitMap = uriToBitmap(it)
                binding.imageView.setImageBitmap(photoBitMap)
                getPhotoLocation(it)
                photoChange = true
            }
        }

        binding.replacePhotoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        //When the user click the edit button, the application saves the current photo + tags in the view model
        // and navigates to the edit fragment
        binding.editPhotoButton.setOnClickListener {
            val description = binding.imageDescriptionEditText.text.toString()
            val location = binding.locationView.text.toString()
            val people = binding.peopleEditText.text.toString()

            photoViewModel.setCurrentPhotoDetails(PhotoDetails(position, description, location, people))

            photoViewModel.setEditedPhoto(photoBitMap as Bitmap)
            val action = PreviewFragmentDirections.actionPreviewFragmentToEditPhotoFragment(args.mode)
            findNavController().navigate(action)
        }

        //When the user clicks the submit button the publishPost or updatePost method are invoked in the viewModel
        binding.submitButton.setOnClickListener {
            photoViewModel.setCurrentPhoto(photoBitMap as Bitmap)
            
            photoBitMap?. let { bitmap ->
                val imageBase64 = bitmapToBase64(bitmap)
                val description = binding.imageDescriptionEditText.text.toString()
                val location = binding.locationView.text.toString()
                val people = binding.peopleEditText.text.toString()

                if (args.mode == Modes.NEW || args.mode == Modes.NEW_WITH_EDITED_PHOTO) {
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


    /**
     * Retrieves the location information (latitude and longitude) from the EXIF data of the photo
     * identified by the given URI. Updates the UI with the location information and queues a
     * location update on the map.
     * @param photoUri The URI of the photo from which to extract location information.
     */
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
                binding.locationView.text = getString(R.string.location_text_default)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Converts the provided URI of a photo to a Bitmap object.
     * @param photoUri The URI of the photo to convert.
     * @return The Bitmap object representing the photo, or null if conversion fails.
     */
    private fun uriToBitmap(photoUri: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Bitmap", e)
            null
        }
    }

    /**
     * Converts the provided Bitmap image to a Base64-encoded string.
     * @param bitmap The Bitmap image to convert.
     * @return The Base64-encoded string representation of the Bitmap.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}