package com.alflabs.milepost.fragments.docentTriggers

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.DocentTriggerItemBinding
import com.alflabs.milepost.trigger.PlaybackTriggerDirection

class DocentTriggersAdapter(
    private val onUpdate: (DocentTriggerItem) -> Unit,
) :
    ListAdapter<DocentTriggerItem, DocentTriggersAdapter.DocentTriggersViewHolder>(DocentTriggersDiffCallback()) {

    companion object {
        private val TAG = DocentTriggersAdapter::class.java.simpleName
    }

    @Deprecated("may not want to use this, remove later")
    var selectedPosition = RecyclerView.NO_POSITION
        private set

    @Deprecated("may not want to use this, remove later")
    fun setSelectedPosition(position: Int) {
        // TBD remove if not used
//        val previousPosition = selectedPosition
//        selectedPosition = position
//        if (previousPosition != RecyclerView.NO_POSITION) {
//            notifyItemChanged(previousPosition)
//        }
//        if (selectedPosition != RecyclerView.NO_POSITION) {
//            notifyItemChanged(selectedPosition)
//        }
    }

    @Deprecated("may not want to use this, remove later")
    fun getSelected(): DocentTriggerItem? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            getItem(selectedPosition)
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentTriggersViewHolder {
        val binding = DocentTriggerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocentTriggersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocentTriggersViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
    }

    inner class DocentTriggersViewHolder(
        private val binding: DocentTriggerItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocentTriggerItem, isSelected: Boolean) {
            val trigger = item.playbackTrigger
            binding.triggerTitle.text = "${item.fileName.ifEmpty { "Unknown Audio ${item.audioFileId}" }}"
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

            binding.root.setOnClickListener {
                onUpdate(item)

                // TBD remove if not used
//                // Highlight the selected item in the recycler view
//                val currentPosition = bindingAdapterPosition
//                if (currentPosition != RecyclerView.NO_POSITION) {
//                    if (currentPosition != selectedPosition) {
//                        setSelectedPosition(currentPosition)
//                    } else {
//                        setSelectedPosition(RecyclerView.NO_POSITION)
//                    }
//                }
            }

            itemView.setBackgroundColor(
                if (isSelected) {
                    itemView.context.getColor(R.color.list_highlight)
                } else {
                    Color.TRANSPARENT
                }
            )
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
