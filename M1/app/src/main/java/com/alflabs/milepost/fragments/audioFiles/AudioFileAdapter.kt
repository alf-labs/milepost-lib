package com.alflabs.milepost.fragments.audioFiles

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.AudioFileItemBinding

class AudioFileAdapter : ListAdapter<AudioFileItem, AudioFileAdapter.AudioFileViewHolder>(AudioFileDiffCallback()) {

    companion object {
        private val TAG = AudioFileAdapter::class.java.simpleName
    }

    var selectedPosition = RecyclerView.NO_POSITION
        private set

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelected() : AudioFileItem? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            getItem(selectedPosition)
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : AudioFileViewHolder {
        val binding = AudioFileItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AudioFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioFileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
    }

    inner class AudioFileViewHolder(
        private val binding: AudioFileItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AudioFileItem, isSelected: Boolean) {
            binding.fileName.text = item.fileName
            binding.lastAccessTime.text = item.lastAccessTime
            binding.uriValid.text = when (item.uriValid) {
                null -> "Pending"
                true -> "Valid"
                else -> "Invalid"
            }

            binding.root.setOnClickListener {
                // Highlight the selected item in the recycler view
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (currentPosition != selectedPosition) {
                        setSelectedPosition(currentPosition)
                    } else {
                        setSelectedPosition(RecyclerView.NO_POSITION)
                    }
                }
            }

            itemView.setBackgroundColor(if (isSelected) {
                itemView.context.getColor(R.color.list_highlight)
            } else {
                Color.TRANSPARENT
            })
        }
    }
}

class AudioFileDiffCallback : DiffUtil.ItemCallback<AudioFileItem>() {
    override fun areItemsTheSame(oldItem: AudioFileItem, newItem: AudioFileItem):
    Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AudioFileItem, newItem: AudioFileItem):
    Boolean {
        return oldItem == newItem
    }
}
