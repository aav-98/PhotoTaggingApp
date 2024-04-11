package com.example.photosapp

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PhotosAdapter(private val photos: List<Pair<String,Bitmap>>, private val clickListener: OnPhotoClickListener): RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photoImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_item, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val (photoFn, bitmap) = photos[position]
        holder.imageView.setImageBitmap(bitmap)
        holder.itemView.setOnClickListener {
            clickListener.onPhotoClick(photoFn)
        }
    }

    override fun getItemCount() = photos.size
}

