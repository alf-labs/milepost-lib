package com.alflabs.milepostlib.op

import com.alflabs.milepostlib.location.LatLongData

/** An MP Estimate Event. */
data class MpEstimate(
    val mp: Float,
    val locIndex: Int,
    val latLong: LatLongData,
    val direction: MpDirection
)
