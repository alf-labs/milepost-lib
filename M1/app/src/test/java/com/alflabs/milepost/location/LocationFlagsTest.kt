package com.alflabs.milepost.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationFlagsTest {

    @Test
    fun testHasFlag() {
        val flags = LocationFlags.USER_MP.value or LocationFlags.STREAMLINE.value
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()
    }

    @Test
    fun testSetFlag() {
        var flags = 0
        flags = flags.setFlag(LocationFlags.USER_MP)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isFalse()

        flags = flags.setFlag(LocationFlags.STREAMLINE)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()
    }

    @Test
    fun testClearFlag() {
        var flags = LocationFlags.USER_MP.value or LocationFlags.STREAMLINE.value
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()

        flags = flags.clearFlag(LocationFlags.USER_MP)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()
    }
}
