package com.alflabs.mpgpstrack.fragments.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.mpgpstrack.databinding.ReviewLocationItemBinding
import com.alflabs.mpgpstrack.utils.Units

class ReviewLocationAdapter :
    ListAdapter<ReviewLocationData, ReviewLocationAdapter.ReviewViewHolder>(LocationDiffCallback())
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewLocationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ReviewViewHolder(val binding: ReviewLocationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReviewLocationData) {
            val ld = item.data
            if (ld == null) {
                binding.index.text = "Idx"
                binding.timestamp.text = "Time"
                binding.latLong.text = "GPS"
                binding.distAcc.text = "Acc."
                binding.speed.text = "Speed"
                binding.mp.text = "MP"
            } else {
                binding.index.text = adapterPosition.toString()
                binding.timestamp.text = String.format("%.1f s", item.deltaTS/1000f)
                binding.latLong.text = "${ld.latLong.latitude}, ${ld.latLong.longitude}"
                binding.distAcc.text = String.format("%.1f m", ld.latLong.accuracy)
                binding.speed.text = String.format("%.1f mph", Units.mps_to_mph(ld.speedBearing.speedMps))
                binding.mp.text = String.format("%.1f mi", Units.m_to_miles(item.distanceMeters)) // RM DEBUG
                // binding.mp.text = ld.mp.toString() // TBD
            }
        }
    }
}

class LocationDiffCallback : DiffUtil.ItemCallback<ReviewLocationData>() {
    override fun areItemsTheSame(oldItem: ReviewLocationData, newItem: ReviewLocationData):
            Boolean {
        val oldTS = oldItem.data?.latLong?.timestampMillis ?: -1
        val newTS = newItem.data?.latLong?.timestampMillis ?: -1
        return oldTS == newTS
    }

    override fun areContentsTheSame(oldItem: ReviewLocationData, newItem: ReviewLocationData):
            Boolean = oldItem == newItem
}
