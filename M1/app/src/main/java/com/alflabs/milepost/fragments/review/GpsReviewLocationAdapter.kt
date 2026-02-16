package com.alflabs.milepost.fragments.review

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.GpsReviewLocationItemBinding
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.hasFlag
import com.alflabs.milepost.utils.Units

class GpsReviewLocationAdapter(
    private val useImperialUnits: Boolean,
    private val onMpClicked: (GpsReviewLocationData) -> Unit,
    private val onSelection: (GpsReviewLocationData) -> Unit
) : ListAdapter<GpsReviewLocationData, GpsReviewLocationAdapter.ReviewViewHolder>(LocationDiffCallback()) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = GpsReviewLocationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding, useImperialUnits, onMpClicked, onSelection)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == selectedPosition)
    }

    inner class ReviewViewHolder(
        private val binding: GpsReviewLocationItemBinding,
        private val useImperialUnits: Boolean,
        private val onMpClicked: (GpsReviewLocationData) -> Unit,
        private val onSelection: (GpsReviewLocationData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val italic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

        fun bind(item: GpsReviewLocationData, isSelected: Boolean) {
            val ld = item.data
            if (ld == null) {
                binding.index.text = "Idx"
                binding.timestamp.text = "Time"
                binding.latLong.text = "GPS"
                binding.distAcc.text = "Acc."
                binding.speed.text = "Speed"
                binding.mp.text = "MP"
                binding.poiLabel.visibility = View.GONE

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

                binding.poiLabel.visibility = View.GONE
                if (ld.hasPoiLabel()) {
                    binding.poiLabel.text = ld.poiLabel
                    binding.poiLabel.visibility = View.VISIBLE
                } else {
                    binding.poiLabel.visibility = View.GONE
                }

                binding.root.setOnClickListener {
                    onSelection(item)
                    // Highlight the selected item in the recycler view
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION && currentPosition != selectedPosition) {
                        setSelectedPosition(currentPosition)
                    }
                }

                itemView.setBackgroundColor(if (isSelected) {
                    itemView.context.getColor(R.color.list_highlight)
                } else {
                    Color.TRANSPARENT
                })

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


class LocationDiffCallback : DiffUtil.ItemCallback<GpsReviewLocationData>() {
    override fun areItemsTheSame(
        oldItem: GpsReviewLocationData,
        newItem: GpsReviewLocationData
    ): Boolean {
        val oldTS = oldItem.data?.latLong?.timestampMillis ?: -1
        val newTS = newItem.data?.latLong?.timestampMillis ?: -1
        return oldTS == newTS
    }

    override fun areContentsTheSame(
        oldItem: GpsReviewLocationData,
        newItem: GpsReviewLocationData
    ): Boolean = oldItem == newItem
}
