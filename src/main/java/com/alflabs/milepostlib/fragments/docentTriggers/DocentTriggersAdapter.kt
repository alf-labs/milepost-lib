package com.alflabs.milepostlib.fragments.docentTriggers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepostlib.databinding.DocentTriggerItemBinding
import com.alflabs.milepostlib.fragments.audioSegments.AudioSegmentItem
import com.alflabs.milepostlib.trigger.PlaybackTriggerDirection

class DocentTriggersAdapter(
    private val onUpdate: (DocentTriggerItem) -> Unit,
) :
    ListAdapter<DocentTriggerItem, DocentTriggersAdapter.DocentTriggersViewHolder>(DocentTriggersDiffCallback()) {

    companion object {
        private val TAG = DocentTriggersAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentTriggersViewHolder {
        val binding = DocentTriggerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocentTriggersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocentTriggersViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DocentTriggersViewHolder(
        private val binding: DocentTriggerItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocentTriggerItem) {
            val trigger = item.playbackTrigger
            binding.triggerTitle.text = if (item.audioDescriptor.isNotEmpty()) {
                AudioSegmentItem.toDisplayTitle(item.audioDescriptor)
            } else {
                "Unknown Audio ${item.audioSegmentId}"
            }

            if (trigger != null) {
                binding.triggerInfo.text =
                    if (trigger.includeInColdStart) "Include in Cold Start"
                    else "Exclude from Cold Start"
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
                binding.triggerInfo.text = "--"
            }
            val mpDur = String.format("%.2f", item.durationMp)
            binding.durationInfo.text = "Duration: ${item.durationDisplay} ($mpDur MP)"

            binding.root.setOnClickListener {
                onUpdate(item)
            }
        }
    }
}

class DocentTriggersDiffCallback : DiffUtil.ItemCallback<DocentTriggerItem>() {
    override fun areItemsTheSame(oldItem: DocentTriggerItem, newItem: DocentTriggerItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DocentTriggerItem, newItem: DocentTriggerItem): Boolean {
        return oldItem == newItem
    }
}
