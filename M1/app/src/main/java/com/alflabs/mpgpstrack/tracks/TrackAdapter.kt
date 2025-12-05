package com.alflabs.mpgpstrack.tracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.databinding.TrackItemBinding

class TrackAdapter(
    private val onClick: (TrackData) -> Unit,
    private val onEdit: (TrackData) -> Unit,
    private val onDelete: (TrackData) -> Unit
) : ListAdapter<TrackData, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = TrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding, onClick, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class TrackViewHolder(
        private val binding: TrackItemBinding,
        private val onClick: (TrackData) -> Unit,
        private val onEdit: (TrackData) -> Unit,
        private val onDelete: (TrackData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: TrackData? = null

        init {
            binding.root.setOnClickListener {
                currentTrack?.let(onClick)
            }
            binding.root.setOnLongClickListener {
                currentTrack?.let { track ->
                    val menu = PopupMenu(itemView.context, itemView)
                    menu.menu.add("Edit").setOnMenuItemClickListener {
                        onEdit(track)
                        true
                    }
                    menu.menu.add("Delete").setOnMenuItemClickListener {
                        onDelete(track)
                        true
                    }
                    menu.show()
                }
                true
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
