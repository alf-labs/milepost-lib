package com.alflabs.mpgpstrack.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitsTest {

    @Test
    fun mps_to_mph() {
        assertThat(Units.mps_to_mph(1f)).isWithin(0.00001f).of(2.23694f)
        assertThat(Units.mps_to_mph(100f)).isWithin(0.001f).of(223.694f)
    }

    @Test
    fun m_to_miles() {
        assertThat(Units.m_to_miles(1f)).isWithin(0.000000001f).of(0.000621371f)
        assertThat(Units.m_to_miles(100f)).isWithin(0.000001f).of(0.0621371f)
        assertThat(Units.m_to_miles(10_000f)).isWithin(0.000001f).of(6.2137117f)
    }


    @Test
    fun mps_to_kmh() {
        assertThat(Units.mps_to_kmh(1f)).isWithin(0.00001f).of(3.6f)
        assertThat(Units.mps_to_kmh(100f)).isWithin(0.001f).of(360f)
    }

    @Test
    fun m_to_km() {
        assertThat(Units.m_to_km(1f)).isWithin(0.00001f).of(0.001f)
        assertThat(Units.m_to_km(100f)).isWithin(0.001f).of(0.1f)
        assertThat(Units.m_to_km(10_000f)).isWithin(0.001f).of(10f)
    }
}
