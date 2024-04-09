package com.example.photosapp

import android.media.ExifInterface
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            val args = PreviewFragmentArgs.fromBundle(it)
            val imageUri = Uri.parse(args.photoURI)
            binding.imageView.setImageURI(imageUri)
            getPhotoLocation(imageUri)
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

        /*
        /TODO: Handle the logic of enditing a photo
        binding.editPhotoButton.setOnClickListener {

        }
         */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun addChipToGroup(chipText: String) {
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

    fun updatePlaceholderVisibility() {
        val chipCount = binding.namesChipGroup.childCount - 1
        if (chipCount > 0) {
            binding.placeholderChip.visibility = View.GONE
        } else {
            binding.placeholderChip.visibility = View.INVISIBLE
        }
    }

    fun getPhotoLocation(photoUri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLong = FloatArray(2)

            val hasLatLong = exifInterface?.getLatLong(latLong) ?: false

            if (hasLatLong) {
                val lat = latLong[0]
                val long = latLong[1]
                binding.locationView.text = "Latitude: $lat, Longitude: $long"
                Log.d("Location", "Latitude: $lat, Longitude: $long")
                // Handle the latitude and longitude (e.g., update the UI)
            } else {
                Log.d("Location", "This photo does not have GPS info.")
                // Handle the absence of location data
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}