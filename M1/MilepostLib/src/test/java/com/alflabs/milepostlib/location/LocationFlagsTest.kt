package com.alflabs.milepostlib.location

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
        var flags = 0.setFlag(LocationFlags.USER_MP)
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

    @Test
    fun testChangeFlag() {
        var flags = 0.changeFlag(LocationFlags.USER_MP, true)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isFalse()

        flags = flags.changeFlag(LocationFlags.STREAMLINE, true)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isTrue()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()

        flags = flags.changeFlag(LocationFlags.USER_MP, false)
        assertThat(flags.hasFlag(LocationFlags.USER_MP)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.IGNORE)).isFalse()
        assertThat(flags.hasFlag(LocationFlags.STREAMLINE)).isTrue()
    }
}
