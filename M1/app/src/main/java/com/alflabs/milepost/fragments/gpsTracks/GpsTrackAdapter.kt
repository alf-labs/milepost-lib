package com.alflabs.milepost.fragments.gpsTracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.GpsTrackItemBinding

class GpsTrackAdapter(
    private val onClick: (GpsTrackDisplayItem) -> Unit,
    private val onEdit: (GpsTrackDisplayItem) -> Unit,
    private val onDelete: (GpsTrackDisplayItem) -> Unit,
    private val useImperialUnits: Boolean,
    private val playbackTrackId: Long,
) : ListAdapter<GpsTrackDisplayItem, GpsTrackAdapter.GpsTrackViewHolder>(GpsTrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GpsTrackViewHolder {
        val binding = GpsTrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GpsTrackViewHolder(binding, onClick, onEdit, onDelete, useImperialUnits, playbackTrackId)
    }

    override fun onBindViewHolder(holder: GpsTrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class GpsTrackViewHolder(
        private val binding: GpsTrackItemBinding,
        private val onClick: (GpsTrackDisplayItem) -> Unit,
        private val onEdit: (GpsTrackDisplayItem) -> Unit,
        private val onDelete: (GpsTrackDisplayItem) -> Unit,
        private val useImperialUnits: Boolean,
        private val playbackTrackId: Long,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: GpsTrackDisplayItem? = null

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

        fun bind(track: GpsTrackDisplayItem) {
            currentTrack = track
            val isPlayback = if (track.id == playbackTrackId) " ★" else ""
            binding.trackName.text = "[${track.id}] ${track.trackLabel?.ifBlank { "(No Title)" }}$isPlayback"
            binding.trackDate.text = track.createdDateISO
            binding.trackSummary.text = track.summary?.toText(useImperialUnits)
        }
    }
}

class GpsTrackDiffCallback : DiffUtil.ItemCallback<GpsTrackDisplayItem>() {
    override fun areItemsTheSame(oldItem: GpsTrackDisplayItem, newItem: GpsTrackDisplayItem):
        Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GpsTrackDisplayItem, newItem: GpsTrackDisplayItem):
        Boolean {
        return oldItem == newItem
    }
}
