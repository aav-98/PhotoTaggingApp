package com.example.photosapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.photosapp.databinding.FragmentPostDetailBinding


class PostDetailFragment : Fragment() {

    private var _binding : FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position = PostDetailFragmentArgs.fromBundle(requireArguments()).pos

        val tags = photoViewModel.tagsLiveData.value
        if (tags != null && position < tags.tagId.size) {

            val description = tags.tagDes[position]
            val peopleNames = tags.tagPeopleName[position]
            val location = tags.tagLocation[position]

            if (description != "") binding.descriptionView.text = description
            if (peopleNames != "") binding.peopleView.text = peopleNames
            if (location != "") binding.locationView.text = location

            val photosMap = photoViewModel.photoLiveData.value
            if (photosMap != null) {
                photosMap[tags.tagPhoto[position]]?.let { base64Image ->
                    binding.imageView.setImageBitmap(base64Image.toBitmap())
                }

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun String.toBitmap(): Bitmap? {
        return try {
            val bytes = Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}