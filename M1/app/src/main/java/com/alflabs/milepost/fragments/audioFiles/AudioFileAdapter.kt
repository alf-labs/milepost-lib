package com.alflabs.milepost.fragments.audioFiles

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.AudioFileItemBinding
import java.util.concurrent.TimeUnit

class AudioFileAdapter(
    private val onPlay: (AudioFileItem) -> Unit,
    private val onUpdate: (AudioFileItem) -> Unit,
) : ListAdapter<AudioFileItem, AudioFileAdapter.AudioFileViewHolder>(AudioFileDiffCallback()) {

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

            val metadata = item.metadata?.let { data ->
                "Duration: ${formatMillis(data.durationMs)}"
            } ?: ""

            binding.fileName.text = item.fileName
            binding.lastAccessTime.text = item.lastAccessTime
            binding.metadata.text = when (item.uriValid) {
                false -> "File not found"
                else -> metadata
            }
            binding.metadata.typeface = if (item.uriValid == false) {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.DEFAULT
            }

            binding.playBtn.setOnClickListener {
                onPlay(item)
            }

            binding.playBtn.setImageResource(
                if (item.isPlaying) R.drawable.ic_btn_pause
                else R.drawable.ic_btn_play
            )
            binding.playBtn.isEnabled = item.uriValid == true || item.isPlaying

            if (item.isPlaying) {
                val tintColor = ContextCompat.getColor(
                    binding.root.context,
                    R.color.item_play_btn_tint)
                binding.playBtn.imageTintList = ColorStateList.valueOf(tintColor)
            } else {
                binding.playBtn.imageTintList = null
            }

            binding.updateBtn.setOnClickListener {
                onUpdate(item)
            }
            binding.updateBtn.visibility = if (item.uriValid == false) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
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

    @SuppressLint("DefaultLocale")
    private fun formatMillis(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

        return if (hours > 0) {
            String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format("%02dm %02ds", minutes, seconds)
        }
    }


}

class AudioFileDiffCallback : DiffUtil.ItemCallback<AudioFileItem>() {
    override fun areItemsTheSame(
        oldItem: AudioFileItem,
        newItem: AudioFileItem
    ) : Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: AudioFileItem,
        newItem: AudioFileItem
    ) : Boolean {
        return oldItem == newItem
    }
}
