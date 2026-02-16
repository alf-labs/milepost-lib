package com.alflabs.milepost.fragments.review

import com.alflabs.milepost.location.LocationData

/*
    A derivative of LocationData suitable for the Review recycler view.
    This add precomputed values wanted at display time.
*/
data class GpsReviewLocationData(
    val locIdx: Int,
    val data: LocationData?,
    val deltaTS: Long = 0,
    val distanceMiles: Float = 0f,
)

