package com.alflabs.milepost.fragments.review

import com.alflabs.milepost.location.LocationData

/*
    A derivative of LocationData suitable for the Review recycler view.
    This add precomputed values wanted at display time.
*/
data class ReviewLocationData(
    val data: LocationData?,
    val deltaTS: Long = 0,
    val distanceMeters: Float = 0f,
)

