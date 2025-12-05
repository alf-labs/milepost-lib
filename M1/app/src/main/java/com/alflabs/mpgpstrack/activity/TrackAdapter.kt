package com.alflabs.mpgpstrack.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.app.TrackData
import com.alflabs.mpgpstrack.databinding.TrackItemBinding

class TrackAdapter : ListAdapter<TrackData, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        return TrackViewHolder(
            TrackItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class TrackViewHolder(private val binding: TrackItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(track: TrackData) {
            binding.trackName.text = track.name
            binding.trackDate.text = track.date
            binding.trackMinMp.text = "Min MP: ${track.minMP}"
            binding.trackMaxMp.text = "Max MP: ${track.maxMP}"
        }
    }
}

class TrackDiffCallback : DiffUtil.ItemCallback<TrackData>() {
    override fun areItemsTheSame(oldItem: TrackData, newItem: TrackData):
        Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TrackData, newItem: TrackData):
        Boolean {
        return oldItem == newItem
    }
}
