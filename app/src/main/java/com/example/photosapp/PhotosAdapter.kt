package com.example.photosapp

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying the list of photos in a RecyclerView, used by [HomeFragment]
 *
 * This adapter binds a list of photos represented as pairs of [String] and [Bitmap] to ImageView items
 * in the RecyclerView. It also provides a click listener interface to handle clicks on individual photo items.
 *
 * @property photos The list of photo data represented as pairs of filenames and corresponding bitmaps.
 * @property clickListener The click listener interface to handle clicks on individual photo items.
 */
class PhotosAdapter(private val photos: List<Pair<String,Bitmap>>, private val clickListener: OnPhotoClickListener):
    RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {
    class PhotoViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val bitmap = photos[position].second
        holder.imageView.setImageBitmap(bitmap)
        holder.itemView.setOnClickListener {
            clickListener.onPhotoClick(position)
        }
    }

    override fun getItemCount() = photos.size
}

interface OnPhotoClickListener {
    fun onPhotoClick(position: Int)
}

