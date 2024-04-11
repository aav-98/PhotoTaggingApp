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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.photosapp.databinding.FragmentHomeBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

    private val TAG = javaClass.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoViewModel.tagsLiveData.observe(viewLifecycleOwner) { tags ->
            Log.d(TAG, "Tags observed changed")
            if (tags != null) {
                val photosList = mutableListOf<Pair<String,Bitmap>>()
                val adapter = PhotosAdapter(photosList, object : OnPhotoClickListener {
                    override fun onPhotoClick(photoFn : String) {
                        Log.d(TAG, "Photo with filename $photoFn clicked")
                        val action =
                            HomeFragmentDirections.actionHomeFragmentToPostDetailFragment(photoFn)
                        findNavController().navigate(action)
                    }
                })
                binding.photosRecyclerView.adapter = adapter
                binding.photosRecyclerView.layoutManager = GridLayoutManager(context, 2)

                photoViewModel.photoLiveData.observe(viewLifecycleOwner) { photosMap ->
                    Log.d(TAG, "Photos observed changed")
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

    private fun String.toBitmap(): Bitmap? {
        return try {
            val bytes = Base64.decode(this, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    interface MainActivityCallback {
        fun onPhotoCountChanged(count: Int)
    }

}