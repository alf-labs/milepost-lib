package com.alflabs.milepost.fragments.docentPlayback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.DocentPlaybackItemBinding
import com.alflabs.milepost.fragments.audioSegments.AudioSegmentItem
import com.alflabs.milepost.trigger.PlaybackTriggerDirection

class DocentPlaybackAdapter :
    ListAdapter<DocentPlaybackItem, DocentPlaybackAdapter.DocentEditorViewHolder>(DocentPlaybackDiffCallback()) {

    companion object {
        private val TAG = DocentPlaybackAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentEditorViewHolder {
        val binding = DocentPlaybackItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocentEditorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocentEditorViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DocentEditorViewHolder(
        private val binding: DocentPlaybackItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocentPlaybackItem) {
            val trigger = item.playbackTrigger

            binding.triggerTitle.text = if (item.descriptor.isNotEmpty()) {
                AudioSegmentItem.toDisplayTitle(item.descriptor)
            } else {
                "Unknown Audio ${item.audioSegmentId}"
            }

            if (trigger != null) {
                val mpMin = String.format("%.2f", trigger.mpMin)
                val mpMax = String.format("%.2f", trigger.mpMax)
                val mpDir = when (trigger.mpDirection) {
                    PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP -> "<⟞"       // < 27DE
                    PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP -> "⟝>"      // 27DD >
                    else -> "<⟛>" // < 27DB >
                }

                binding.triggerRange.text = "MP Range: $mpMin $mpDir $mpMax"
            } else {
                binding.triggerRange.text = "--"
            }

            binding.playStatus.text = when {
                item.isPlaying -> "Playing"
                item.isPending -> "Pending"
                else -> ""
            }

            if (item.isPlaying) {
                binding.playRange.text = item.playDisplay
            } else {
                binding.playRange.text = ""
            }

//            itemView.setBackgroundColor(
//                if (isSelected) {
//                    itemView.context.getColor(R.color.list_highlight)
//                } else {
//                    Color.TRANSPARENT
//                }
//            )
        }
    }
}

class DocentPlaybackDiffCallback : DiffUtil.ItemCallback<DocentPlaybackItem>() {
    override fun areItemsTheSame(oldItem: DocentPlaybackItem, newItem: DocentPlaybackItem): Boolean {
        return oldItem.triggerId == newItem.triggerId
    }

    override fun areContentsTheSame(oldItem: DocentPlaybackItem, newItem: DocentPlaybackItem): Boolean {
        return oldItem == newItem
    }
}
