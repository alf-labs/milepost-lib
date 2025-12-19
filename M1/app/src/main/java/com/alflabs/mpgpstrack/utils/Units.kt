package com.alflabs.mpgpstrack.utils

object Units {
    const val SECONDS_PER_HOUR = 3600
    const val METERS_PER_MILE = 1609.344f
    const val METERS_PER_KM = 1000f
    const val MPS_TO_MPH = SECONDS_PER_HOUR / METERS_PER_MILE
    const val MPS_TO_KMH = SECONDS_PER_HOUR / METERS_PER_KM

    fun mps_to_mph(mps: Float): Float = mps * MPS_TO_MPH

    fun mps_to_kmh(mps: Float): Float = mps * MPS_TO_KMH

    fun m_to_miles(m: Float): Float = m / METERS_PER_MILE

    fun m_to_km(m: Float): Float = m / METERS_PER_KM

    fun mToUnit(m: Float, useImperialUnits: Boolean): Float =
        if (useImperialUnits) m_to_miles(m)
        else m_to_km(m)

    fun mpsToUnit(mps: Float, useImperialUnits: Boolean): Float =
        if (useImperialUnits) mps_to_mph(mps)
        else mps_to_kmh(mps)

    fun mUnitName(useImperialUnits: Boolean): String =
        if (useImperialUnits) "mi" else "km"

    fun mpsUnitName(useImperialUnits: Boolean): String =
        if (useImperialUnits) "mph" else "km/h"
}
