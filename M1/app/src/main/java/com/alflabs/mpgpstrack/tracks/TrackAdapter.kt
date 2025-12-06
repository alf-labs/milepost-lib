package com.alflabs.mpgpstrack.tracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.databinding.TrackItemBinding

class TrackAdapter(
    private val onClick: (TrackEntity) -> Unit,
    private val onEdit: (TrackEntity) -> Unit,
    private val onDelete: (TrackEntity) -> Unit
) : ListAdapter<TrackEntity, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

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
        private val onClick: (TrackEntity) -> Unit,
        private val onEdit: (TrackEntity) -> Unit,
        private val onDelete: (TrackEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: TrackEntity? = null

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

        fun bind(track: TrackEntity) {
            currentTrack = track
            binding.trackName.text = "${track.id} - ${track.label}"
            binding.trackDate.text = track.createdDateISO
            binding.trackMinMp.text = "Min MP: ${track.minMP}"
            binding.trackMaxMp.text = "Max MP: ${track.maxMP}"
        }
    }
}

class TrackDiffCallback : DiffUtil.ItemCallback<TrackEntity>() {
    override fun areItemsTheSame(oldItem: TrackEntity, newItem: TrackEntity):
        Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TrackEntity, newItem: TrackEntity):
        Boolean {
        return oldItem == newItem
    }
}
