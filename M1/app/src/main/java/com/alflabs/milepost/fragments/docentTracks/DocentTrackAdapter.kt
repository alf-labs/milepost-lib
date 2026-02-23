package com.alflabs.milepost.fragments.docentTracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.DocentTrackItemBinding
import com.alflabs.milepost.db.DocentTrackEntity

class DocentTrackAdapter(
    private val onClick: (DocentTrackEntity) -> Unit,
    private val onEdit: (DocentTrackEntity) -> Unit,
    private val onDelete: (DocentTrackEntity) -> Unit,
    private val useImperialUnits: Boolean,
    private val playbackTrackId: Long,
) : ListAdapter<DocentTrackEntity, DocentTrackAdapter.DocentTrackViewHolder>(DocentTrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentTrackViewHolder {
        val binding = DocentTrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocentTrackViewHolder(binding, onClick, onEdit, onDelete, useImperialUnits, playbackTrackId)
    }

    override fun onBindViewHolder(holder: DocentTrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    class DocentTrackViewHolder(
        private val binding: DocentTrackItemBinding,
        private val onClick: (DocentTrackEntity) -> Unit,
        private val onEdit: (DocentTrackEntity) -> Unit,
        private val onDelete: (DocentTrackEntity) -> Unit,
        private val useImperialUnits: Boolean,
        private val playbackTrackId: Long,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentTrack: DocentTrackEntity? = null

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

        fun bind(track: DocentTrackEntity) {
            currentTrack = track
            val isPlayback = if (track.id == playbackTrackId) " ★" else ""
            binding.userTitle.text = "[${track.id}] ${track.userTitle?.ifBlank { "(No Title)" }}$isPlayback"
            binding.creationTime.text = track.creationTime
            binding.lastUsedTime.text = track.lastUsedTime ?: "new"
        }
    }
}

class DocentTrackDiffCallback : DiffUtil.ItemCallback<DocentTrackEntity>() {
    override fun areItemsTheSame(
        oldItem: DocentTrackEntity,
        newItem: DocentTrackEntity
    ) : Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: DocentTrackEntity,
        newItem: DocentTrackEntity
    ) : Boolean {
        return oldItem == newItem
    }
}
