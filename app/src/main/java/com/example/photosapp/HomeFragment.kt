package com.example.photosapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.databinding.FragmentHomeBinding

/**
 * A fragment responsible for displaying a grid of photos.
 *
 * This fragment sets up a RecyclerView to display photos fetched from the ViewModel.
 * It observes changes in tags and photos data and updates the UI accordingly.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val TAG = javaClass.simpleName
    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Setting up the recycleView and the OnPhotoClickListener
        val photosList = mutableListOf<Pair<String,Bitmap>>()
        val adapter = PhotosAdapter(photosList, object : OnPhotoClickListener {
            override fun onPhotoClick(position : Int) {
                val bitmap = photosList[position].second
                val fn = photosList[position].first
                photoViewModel.setCurrentPhoto(bitmap)
                photoViewModel.setEditedPhoto(bitmap)
                val action =
                    HomeFragmentDirections.actionHomeFragmentToPostDetailFragment(fn)
                findNavController().navigate(action)
            }
        })
        binding.photosRecyclerView.adapter = adapter
        binding.photosRecyclerView.layoutManager = GridLayoutManager(context, 2)

        //Updating the recycleView with the tags and photos observed from tagsLiveData and photoLiveData
        photoViewModel.tagsLiveData.observe(viewLifecycleOwner) { tags ->
            tags?.let {
                photoViewModel.loadPhotos()

                photoViewModel.photoLiveData.observe(viewLifecycleOwner) { photosMap ->
                    photosList.clear()
                    tags.tagPhoto.toList().take(tags.numberOfTags.toInt()).forEach { fn ->
                        photosMap[fn]?.let { base64String ->
                            base64String.toBitmap()?.let { bitmap ->
                                photosList.add(Pair(fn,bitmap))
                            }
                        } ?: Log.d(TAG, "No photo found for filename: $fn")
                    }
                    adapter.notifyDataSetChanged()
                    (activity as? MainActivityCallback)?.onPhotoCountChanged(photosList.size)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface MainActivityCallback {
        fun onPhotoCountChanged(count: Int)
    }

    /**
     * Converts a Base64 encoded string to a Bitmap object
     */
    private fun String.toBitmap(): Bitmap? {
        return try {
            val bytes = Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}