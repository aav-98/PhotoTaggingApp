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
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.photosapp.databinding.FragmentPostDetailBinding
import com.google.android.material.chip.Chip


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

        val filename = PostDetailFragmentArgs.fromBundle(requireArguments()).photoFn

        val tags = photoViewModel.tagsLiveData.value
        tags?.tagPhoto?.indexOf(filename)?.let { position ->
            if (position < tags.tagId.size) {

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

            binding.deleteButton.setOnClickListener {
                photoViewModel.updateTags(position.toString(), "na", "na", "na", "na")
                //I dont know why but it does so automatically? And if i do it explicitly it is slower
                //findNavController().navigate(R.id.action_PostDetailFragment_to_HomeFragment)
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