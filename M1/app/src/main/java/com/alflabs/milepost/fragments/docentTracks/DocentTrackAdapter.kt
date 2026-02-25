package com.alflabs.milepost.fragments.docentTracks

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.DocentTrackItemBinding

class DocentTrackAdapter(
    private val onClick: (DocentTrackItem) -> Unit,
    private val onEdit: (DocentTrackItem) -> Unit,
    private val onDelete: (DocentTrackItem) -> Unit,
    private val docentPlaybackTrackId: Long,
) : ListAdapter<DocentTrackItem, DocentTrackAdapter.DocentTrackViewHolder>(DocentTrackDiffCallback()) {

    companion object {
        private val TAG = DocentTrackAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentTrackViewHolder {
        val binding = DocentTrackItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocentTrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocentTrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    inner class DocentTrackViewHolder(
        private val binding: DocentTrackItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocentTrackItem) {
            binding.root.setOnClickListener {
                onClick(item)
            }
            binding.root.setOnLongClickListener {
                val menu = PopupMenu(itemView.context, itemView)
                menu.menu.add("Edit").setOnMenuItemClickListener {
                    onEdit(item)
                    true
                }
                menu.menu.add("Delete").setOnMenuItemClickListener {
                    onDelete(item)
                    true
                }
                menu.show()
                true
            }

            val isPlayback = if (item.id == docentPlaybackTrackId) " ★" else ""
            binding.userTitle.text = "[${item.id}] ${item.userTitle?.ifBlank { "(No Title)" }}$isPlayback"
            binding.creationTime.text = item.creationTime
            binding.gpsTrackLabel.text = item.gpsTrackLabel ?:
                "GPS Track #${item.gpsTrackId}"
        }
    }
}

class DocentTrackDiffCallback : DiffUtil.ItemCallback<DocentTrackItem>() {
    override fun areItemsTheSame(
        oldItem: DocentTrackItem,
        newItem: DocentTrackItem
    ) : Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: DocentTrackItem,
        newItem: DocentTrackItem
    ) : Boolean {
        return oldItem == newItem
    }
}
