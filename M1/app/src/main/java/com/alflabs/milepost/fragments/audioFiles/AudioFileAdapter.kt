package com.alflabs.milepost.fragments.audioFiles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.AudioFileItemBinding
import com.alflabs.milepost.tracks.AudioFileEntity

class AudioFileAdapter :
    ListAdapter<AudioFileEntity, AudioFileAdapter.AudioFileViewHolder>(AudioFileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
    AudioFileViewHolder {
        val binding = AudioFileItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AudioFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioFileViewHolder, position: Int) {
        val audioFile = getItem(position)
        holder.bind(audioFile)
    }

    class AudioFileViewHolder(val binding: AudioFileItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(audioFile: AudioFileEntity) {
            binding.userTitle.text = audioFile.userTitle
            binding.fileName.text = audioFile.fileName
            binding.lastAccessTime.text = audioFile.lastAccessTime
            binding.uriValid.text = "TBD maybe?"
        }
    }
}

class AudioFileDiffCallback : DiffUtil.ItemCallback<AudioFileEntity>() {
    override fun areItemsTheSame(oldItem: AudioFileEntity, newItem: AudioFileEntity):
    Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AudioFileEntity, newItem: AudioFileEntity):
    Boolean {
        return oldItem == newItem
    }
}
