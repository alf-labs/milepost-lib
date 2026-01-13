package com.alflabs.milepost.fragments.review

import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.ReviewLocationItemBinding
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.hasFlag
import com.alflabs.milepost.utils.Units

class ReviewLocationAdapter(
    private val useImperialUnits: Boolean,
    private val onMpClicked: (ReviewLocationData) -> Unit,
    private val onSelection: (ReviewLocationData) -> Unit
) : ListAdapter<ReviewLocationData, ReviewLocationAdapter.ReviewViewHolder>(LocationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewLocationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding, useImperialUnits, onMpClicked, onSelection)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ReviewViewHolder(
        private val binding: ReviewLocationItemBinding,
        private val useImperialUnits: Boolean,
        private val onMpClicked: (ReviewLocationData) -> Unit,
        private val onSelection: (ReviewLocationData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val italic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

        fun bind(item: ReviewLocationData) {
            val ld = item.data
            if (ld == null) {
                binding.index.text = "Idx"
                binding.timestamp.text = "Time"
                binding.latLong.text = "GPS"
                binding.distAcc.text = "Acc."
                binding.speed.text = "Speed"
                binding.mp.text = "MP"

                binding.timestamp.typeface = Typeface.DEFAULT_BOLD
                binding.latLong.typeface = Typeface.DEFAULT_BOLD
                binding.mp.typeface = Typeface.DEFAULT_BOLD
            } else {
                val ignore = ld.flags.hasFlag(LocationFlags.IGNORE)
                val userMp = ld.flags.hasFlag(LocationFlags.USER_MP)

                binding.index.text = adapterPosition.toString()
                binding.timestamp.text = String.format("%.1f s", item.deltaTS / 1000f)
                binding.latLong.text = "${ld.latLong.latitude}, ${ld.latLong.longitude}"
                binding.distAcc.text = String.format("%.1f m", ld.latLong.accuracy)
                binding.speed.text = String.format("%.1f %s",
                    Units.mpsToUnit(ld.speed.speedMps, useImperialUnits),
                    Units.mpsUnitName(useImperialUnits)
                )
                binding.mp.text = if (ignore) "Ignored" else String.format("%.2f mi", ld.mp)

                binding.mp.setOnClickListener {
                    onMpClicked(item)
                }
                binding.root.setOnClickListener {
                    onSelection(item)
                }

                binding.timestamp.typeface = Typeface.DEFAULT
                binding.latLong.typeface = Typeface.DEFAULT

                binding.mp.typeface = if (userMp) {
                    Typeface.DEFAULT_BOLD
                } else {
                    italic
                }

                binding.index.strikeThrough(ignore)
                binding.timestamp.strikeThrough(ignore)
                binding.latLong.strikeThrough(ignore)
                binding.distAcc.strikeThrough(ignore)
                binding.speed.strikeThrough(ignore)
                binding.mp.strikeThrough(ignore)
            }
        }
    }
}

private fun TextView.strikeThrough(strike: Boolean) {
    if (strike) {
        this.paintFlags = this.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
        this.paintFlags = this.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}


class LocationDiffCallback : DiffUtil.ItemCallback<ReviewLocationData>() {
    override fun areItemsTheSame(
        oldItem: ReviewLocationData,
        newItem: ReviewLocationData
    ): Boolean {
        val oldTS = oldItem.data?.latLong?.timestampMillis ?: -1
        val newTS = newItem.data?.latLong?.timestampMillis ?: -1
        return oldTS == newTS
    }

    override fun areContentsTheSame(
        oldItem: ReviewLocationData,
        newItem: ReviewLocationData
    ): Boolean = oldItem == newItem
}
