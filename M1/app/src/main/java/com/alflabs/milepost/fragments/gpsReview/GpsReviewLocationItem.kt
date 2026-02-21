package com.alflabs.milepost.fragments.gpsReview

import com.alflabs.milepost.location.LocationData

/**
 * A derivative of LocationData suitable for the [GpsReviewFragment] recycler view.
 * This adds precomputed values wanted at display time.
 */
data class GpsReviewLocationItem(
    val locIdx: Int,
    val data: LocationData?,
    val deltaTS: Long = 0,
    val distanceMiles: Float = 0f,
)

