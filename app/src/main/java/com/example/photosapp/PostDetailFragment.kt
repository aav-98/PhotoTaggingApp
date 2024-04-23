package com.example.photosapp


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.photosapp.databinding.FragmentPostDetailBinding
import com.google.android.gms.maps.model.LatLng


class PostDetailFragment : BaseMapFragment() {

    private var _binding : FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

    private val TAG = javaClass.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        mapView = _binding?.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoViewModel.currentPhotoDetails.observe(viewLifecycleOwner) { photoDetails ->
            if (photoDetails.description != "") binding.descriptionView.text = photoDetails.description
            if (photoDetails.people != "") binding.peopleView.text = photoDetails.people

            if (photoDetails.location != "") {
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
            }
            binding.imageView.setImageBitmap(photoViewModel.currentPhoto.value)


            binding.deleteButton.setOnClickListener {
                photoViewModel.updatePost(photoDetails.id, "na", "na", "na", "na")
                findNavController().navigate(R.id.action_PostDetailFragment_to_HomeFragment)
            }

            binding.editPostButton.setOnClickListener {
                val action =
                    PostDetailFragmentDirections.actionPostDetailFragmentToPreviewFragment(mode = "edit")
                findNavController().navigate(action)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}