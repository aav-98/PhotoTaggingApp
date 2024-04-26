package com.example.photosapp


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.photosapp.data.model.PhotoDetails
import com.example.photosapp.databinding.FragmentPostDetailBinding
import com.google.android.gms.maps.model.LatLng
import com.example.photosapp.data.model.Modes

/**
 * Fragment responsible for displaying detailed information about a specific photo post.
 *
 * This fragment retrieves photo details and corresponding tags from the ViewModel and displays
 * them to the user. It also provides options to edit or delete the photo post.
 */
class PostDetailFragment : BaseMapFragment() {

    private var _binding : FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val TAG = javaClass.simpleName
    private val args: PostDetailFragmentArgs by navArgs()
    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

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

        //Fetching the photos corresponding tags
        val fn = args.fn
        val tags = photoViewModel.tagsLiveData.value
        val position = tags?.tagPhoto?.indexOf(fn)

        //Displaying the photo and the corresponding tags to the user
        position?.let {
            binding.imageView.setImageBitmap(photoViewModel.currentPhoto.value)

            val description = tags.tagDes[position]
            val peopleNames = tags.tagPeopleName[position]
            val location = tags.tagLocation[position]
            photoViewModel.setCurrentPhotoDetails(PhotoDetails(position.toString(), description, location, peopleNames))

            binding.descriptionView.text = description
            binding.peopleView.text = peopleNames
            binding.locationView.text = location

            val locationParts = location.split(",")
            if (locationParts.size == 2) {
                try {
                    queueLocationUpdate(LatLng(locationParts[0].toDouble(), locationParts[1].toDouble()))
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Invalid location format", e)
                }
            }

            binding.deleteButton.setOnClickListener {
                photoViewModel.updatePost(position.toString(), "na", "na", "na", "na")
                findNavController().navigate(R.id.action_PostDetailFragment_to_HomeFragment)
            }

            binding.editPostButton.setOnClickListener {
                val action =
                    PostDetailFragmentDirections.actionPostDetailFragmentToPreviewFragment(mode = Modes.EDIT)
                findNavController().navigate(action)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}