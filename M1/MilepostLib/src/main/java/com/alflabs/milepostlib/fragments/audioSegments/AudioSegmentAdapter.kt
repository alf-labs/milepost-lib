package com.alflabs.milepostlib.fragments.audioSegments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.databinding.AudioSegmentItemBinding
import com.alflabs.milepostlib.utils.Time

class AudioSegmentAdapter(
    private val onPlay: (AudioSegmentItem) -> Unit,
    private val onUpdate: (AudioSegmentItem) -> Unit,
) : ListAdapter<AudioSegmentItem, AudioSegmentAdapter.AudioSegmentViewHolder>(AudioSegmentDiffCallback()) {

    companion object {
        private val TAG = AudioSegmentAdapter::class.java.simpleName
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

    fun getSelected() : AudioSegmentItem? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            getItem(selectedPosition)
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : AudioSegmentViewHolder {
        val binding = AudioSegmentItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AudioSegmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioSegmentViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
    }

    inner class AudioSegmentViewHolder(
        private val binding: AudioSegmentItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AudioSegmentItem, isSelected: Boolean) {

            val metadata = item.metadata?.let { data ->
                "Duration: ${Time.formatMillis(data.durationMs)}"
            } ?: ""

            binding.fileName.text = item.displayTitle()
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
                else R.drawable.ic_btn_play_next
            )
            binding.playBtn.isEnabled = item.uriValid == true || item.isPlaying

            Log.d(TAG, "@@ item id ${item.id} isPlaying: ${item.isPlaying}")
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
}

class AudioSegmentDiffCallback : DiffUtil.ItemCallback<AudioSegmentItem>() {
    override fun areItemsTheSame(
        oldItem: AudioSegmentItem,
        newItem: AudioSegmentItem
    ) : Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: AudioSegmentItem,
        newItem: AudioSegmentItem
    ) : Boolean {
        return oldItem == newItem
    }
}
