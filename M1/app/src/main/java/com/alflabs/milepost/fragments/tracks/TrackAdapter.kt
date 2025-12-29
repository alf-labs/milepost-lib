package com.alflabs.milepost.fragments.tracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.TrackItemBinding
import com.alflabs.milepost.tracks.TrackDisplayInfo

class TrackAdapter(
    private val onClick: (TrackDisplayInfo) -> Unit,
    private val onEdit: (TrackDisplayInfo) -> Unit,
    private val onDelete: (TrackDisplayInfo) -> Unit,
    private val useImperialUnits: Boolean
) : ListAdapter<TrackDisplayInfo, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = TrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TrackViewHolder(binding, onClick, onEdit, onDelete, useImperialUnits)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class TrackViewHolder(
        private val binding: TrackItemBinding,
        private val onClick: (TrackDisplayInfo) -> Unit,
        private val onEdit: (TrackDisplayInfo) -> Unit,
        private val onDelete: (TrackDisplayInfo) -> Unit,
        private val useImperialUnits: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: TrackDisplayInfo? = null

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

        fun bind(track: TrackDisplayInfo) {
            currentTrack = track
            binding.trackName.text = "[${track.id}] ${track.trackLabel?.ifBlank { "(No Title)" }}"
            binding.trackDate.text = track.createdDateISO
            binding.trackSummary.text = track.summary?.toText(useImperialUnits)
        }
    }
}

class TrackDiffCallback : DiffUtil.ItemCallback<TrackDisplayInfo>() {
    override fun areItemsTheSame(oldItem: TrackDisplayInfo, newItem: TrackDisplayInfo):
        Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TrackDisplayInfo, newItem: TrackDisplayInfo):
        Boolean {
        return oldItem == newItem
    }
}
