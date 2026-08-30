package com.alflabs.milepostlib.fragments.docentTracks

/**
 * A derivative of [com.alflabs.milepostlib.db.DocentTrackEntity]
 * suitable for the [DocentTracksFragment] recycler view.
 */
data class DocentTrackItem(
    val id: Long,
    val userTitle: String,
    val creationTime: String,
    val gpsTrackId: Long,
    val gpsTrackLabel: String?,
)

