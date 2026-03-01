package com.alflabs.milepost.fragments.docentEditor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.DocentEditorItemBinding

class DocentEditAdapter() :
    ListAdapter<DocentEditorItem, DocentEditAdapter.DocentEditViewHolder>(DocentEditDiffCallback()) {

    companion object {
        private val TAG = DocentEditAdapter::class.java.simpleName
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

    fun getSelected(): DocentEditorItem? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            getItem(selectedPosition)
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocentEditViewHolder {
        val binding = DocentEditorItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DocentEditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocentEditViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
    }

    inner class DocentEditViewHolder(
        private val binding: DocentEditorItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DocentEditorItem, isSelected: Boolean) {

            binding.triggerTitle.text = "some trigger title"
            binding.triggerInfo.text = "some trigger info"

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

class DocentEditDiffCallback : DiffUtil.ItemCallback<DocentEditorItem>() {
    override fun areItemsTheSame(oldItem: DocentEditorItem, newItem: DocentEditorItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DocentEditorItem, newItem: DocentEditorItem): Boolean {
        return oldItem == newItem
    }
}
