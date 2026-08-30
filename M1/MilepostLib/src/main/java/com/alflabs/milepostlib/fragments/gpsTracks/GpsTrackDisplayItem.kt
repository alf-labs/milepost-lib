package com.alflabs.milepostlib.fragments.gpsTracks

import com.alflabs.milepostlib.db.GpsTrackSummary

/**
 * A subset of [com.alflabs.milepostlib.db.GpsTrackEntity] for displaying in the
 * [GpsTracksFragment] recycler view. This allows the DAO to only load the columns
 * we need to display in the recycler view.
 */
data class GpsTrackDisplayItem(
    val id: Long = 0,
    val createdDateISO: String,
    val trackLabel: String? = null,
    val summary: GpsTrackSummary? = null,
)
