package com.alflabs.milepost.fragments.docentTracks

/**
 * A derivative of [com.alflabs.milepost.db.DocentTrackEntity]
 * suitable for the [DocentTracksFragment] recycler view.
 */
data class DocentTrackItem(
    val id: Long,
    val userTitle: String,
    val creationTime: String,
    val gpsTrackId: Long,
    val gpsTrackLabel: String?,
)

