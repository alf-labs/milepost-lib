package com.alflabs.mpgpstrack.utils

class Units {
    companion object {
        const val SECONDS_PER_HOUR = 3600
        const val METERS_PER_MILE = 1609.344f
        const val MPS_TO_MPH = SECONDS_PER_HOUR / METERS_PER_MILE

        fun mps_to_mph(mps: Float): Float = mps * MPS_TO_MPH

        fun m_to_miles(m: Float): Float = m / METERS_PER_MILE
    }
}
