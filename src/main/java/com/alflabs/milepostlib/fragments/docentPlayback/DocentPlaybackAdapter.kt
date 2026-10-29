package com.alflabs.milepostlib.fragments.docentPlayback

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.databinding.DocentPlaybackItemBinding
import com.alflabs.milepostlib.fragments.audioSegments.AudioSegmentItem
import com.alflabs.milepostlib.trigger.PlaybackTriggerDirection
import com.alflabs.milepostlib.utils.Time

class DocentPlaybackAdapter(
    private val onAction: (DocentPlaybackItem) -> Unit
) :
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
                item.isPlaying && item.isPaused -> "Paused"
                item.isPlaying && !item.isPaused -> "Playing"
                item.isPending -> "Pending"
                else -> ""
            }

            if (item.btnAction != binding.actionBtn.tag) {
                val iconRes = when(item.btnAction) {
                    DocentPlaybackItem.BtnAction.ForcePlay -> R.drawable.ic_btn_play_next
                    DocentPlaybackItem.BtnAction.Pause -> R.drawable.ic_btn_pause
                    DocentPlaybackItem.BtnAction.Unpause -> R.drawable.ic_btn_unpause
                }
                val scale = when(item.btnAction) {
                    DocentPlaybackItem.BtnAction.Pause,
                    DocentPlaybackItem.BtnAction.Unpause -> 2
                    else -> 1
                }

                binding.actionBtn.setImageResource(iconRes)
                binding.actionBtn.scaleX = scale.toFloat()
                binding.actionBtn.scaleY = scale.toFloat()
                binding.actionBtn.tag = item.btnAction
            }

            binding.actionBtn.setOnClickListener {
                onAction(item)
            }

            if (item.isPlaying) {
                binding.playRange.text = "${Time.formatMillis(item.currentMs.toLong())} / ${Time.formatMillis(item.durationMs.toLong())}"
            } else {
                binding.playRange.text = ""
            }

            binding.triggerTitle.setBackgroundColor(
                if (item.isPlaying) {
                    binding.triggerTitle.context.getColor(R.color.list_highlight)
                } else {
                    Color.TRANSPARENT
                }
            )
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
