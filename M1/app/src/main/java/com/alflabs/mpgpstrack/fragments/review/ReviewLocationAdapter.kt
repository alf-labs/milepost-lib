package com.alflabs.mpgpstrack.fragments.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.databinding.ReviewLocationItemBinding
import com.alflabs.mpgpstrack.location.LocationData

class ReviewLocationAdapter : ListAdapter<LocationData, ReviewLocationAdapter.ReviewViewHolder>(LocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewLocationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ReviewViewHolder(val binding: ReviewLocationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationData) {
            binding.index.text = adapterPosition.toString()
            binding.timestamp.text = item.latLong.timestampMillis.toString()
            binding.latLong.text = "${item.latLong.latitude}, ${item.latLong.longitude}"
            binding.mp.text = item.mp.toString()
        }
    }
}

class LocationDiffCallback : DiffUtil.ItemCallback<LocationData>() {
    override fun areItemsTheSame(oldItem: LocationData, newItem: LocationData):
            Boolean = oldItem.latLong.timestampMillis == newItem.latLong.timestampMillis

    override fun areContentsTheSame(oldItem: LocationData, newItem: LocationData):
            Boolean = oldItem == newItem
}
