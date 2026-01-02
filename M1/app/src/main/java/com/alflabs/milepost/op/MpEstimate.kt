package com.alflabs.milepost.op

import com.alflabs.milepost.location.LatLongData

data class MpEstimate(
    val mp: Float,
    val locIndex: Int,
    val latLong: LatLongData,
    val direction: MpDirection
)
