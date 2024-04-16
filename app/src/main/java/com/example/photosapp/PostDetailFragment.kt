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

    var photoString = ""
    var description = ""
    var peopleNames = ""
    var location = ""

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

                description = tags.tagDes[position]
                peopleNames = tags.tagPeopleName[position]
                location = tags.tagLocation[position]

                if (description != "") binding.descriptionView.text = description
                if (peopleNames != "") binding.peopleView.text = peopleNames
                if (location != "") binding.locationView.text = location

                photoViewModel.currentImageBitmap.observe(viewLifecycleOwner) { bitmap ->
                    binding.imageView.setImageBitmap(bitmap)

                }
                /*
                val photosMap = photoViewModel.photoLiveData.value

                if (photosMap != null) {
                    photosMap[tags.tagPhoto[position]]?.let { base64Image ->
                        photoString = base64Image
                        binding.imageView.setImageBitmap(photoString.toBitmap())
                    }

                }

                 */
            }

            binding.deleteButton.setOnClickListener {
                photoViewModel.updatePost(position.toString(), "na", "na", "na", "na")
                findNavController().navigate(R.id.action_PostDetailFragment_to_HomeFragment)
            }

            binding.editPostButton.setOnClickListener {
                val action =
                    PostDetailFragmentDirections.actionPostDetailFragmentToPreviewFragment(
                        mode = "edit", description = description,
                        peopleNames = peopleNames, location = location, position= position.toString())
                findNavController().navigate(action)

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