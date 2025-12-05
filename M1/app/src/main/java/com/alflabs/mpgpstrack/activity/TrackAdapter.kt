package com.alflabs.mpgpstrack.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.app.TrackData
import com.alflabs.mpgpstrack.databinding.TrackItemBinding

class TrackAdapter(
    private val onClick: (TrackData) -> Unit
) : ListAdapter<TrackData, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = TrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class TrackViewHolder(
        private val binding: TrackItemBinding,
        private val onClick: (TrackData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: TrackData? = null

        init {
            binding.root.setOnClickListener {
                currentTrack?.let(onClick)
            }
        }

        fun bind(track: TrackData) {
            currentTrack = track
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
