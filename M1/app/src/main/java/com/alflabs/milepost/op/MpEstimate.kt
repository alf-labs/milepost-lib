package com.alflabs.milepost.op

import com.alflabs.milepost.location.LatLongData

/** An MP Estimate Event. */
data class MpEstimate(
    val mp: Float,
    val locIndex: Int,
    val latLong: LatLongData,
    val direction: MpDirection
)
