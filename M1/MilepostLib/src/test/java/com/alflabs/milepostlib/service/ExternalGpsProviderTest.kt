package com.alflabs.milepostlib.service

import android.hardware.usb.UsbDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.utils.ILogger
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class ExternalGpsProviderTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    companion object {
        private val TAG = ExternalGpsProviderTest::class.java.simpleName
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var provider: ExternalGpsProvider
    private val fakeLogger get() = logger as StringLogger


    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun filterUsbDevices_handlesNullAndEmpty() {
        assertThat(provider.filterUsbDevices("", emptyList())).isNull()
        assertThat(provider.filterUsbDevices("1659:9123", listOf(null))).isNull()
    }

    private fun mkUsbDev(vid: Int, pid: Int, manuf: String, product: String) : UsbDevice
    = mock<UsbDevice> {
        on { vendorId } doReturn vid
        on { productId } doReturn pid
        on { manufacturerName } doReturn manuf
        on { productName } doReturn product
    }

    @Test
    fun filterUsbDevices_matchesCorrectDevice() {
        val wrongVendor         = mkUsbDev(1234, 9123, "Prolific", "USB-Serial")
        val wrongProduct        = mkUsbDev(1659, 4321, "Prolific", "USB-Serial")
        val wrongManufacturer   = mkUsbDev(1659, 9123, "Wrong",    "USB-Serial")
        val wrongProductName    = mkUsbDev(1659, 9123, "Prolific", "Wrong")
        val matchingDevice      = mkUsbDev(1659, 9123, "Prolific", "USB-Serial")

        val result = provider.filterUsbDevices(
            "1659:9123 Prolific USB-Serial",
            listOf(wrongVendor,
                wrongProduct,
                wrongProductName,
                wrongManufacturer,
                matchingDevice))

        assertThat(result).isEqualTo(matchingDevice)
        assertThat(fakeLogger.string).isEmpty()
    }

    @Test
    fun filterUsbDevices_ignoresCase() {
        val matchingDevice = mkUsbDev(1659, 9123, "PROLIFIC", "usb-serial")

        val result = provider.filterUsbDevices("1659:9123 prolific serial", listOf(matchingDevice))

        assertThat(result).isEqualTo(matchingDevice)
    }

    @Test
    fun filterUsbDevices_matchesMultipleVidPid() {
        val dev1 = mkUsbDev(0x1234, 0x5678, "A", "B")
        val dev2 = mkUsbDev(0xABCD, 0x1234, "C", "D")
        val devices = listOf(dev1, dev2)

        assertThat(provider.filterUsbDevices("0x1234:5678 ABCD:1234", devices)).isEqualTo(dev1)
        assertThat(provider.filterUsbDevices("ABCD:1234", devices)).isEqualTo(dev2)
    }

    @Test
    fun filterUsbDevices_matchesHexAndDecimal() {
        val dev = mkUsbDev(0x12AB, 4660, "A", "B") // 4660 decimal is 0x1234
        val devices = listOf(dev)

        assertThat(provider.filterUsbDevices("12AB:1234", devices)).isEqualTo(dev)
        assertThat(provider.filterUsbDevices("0x12AB:0x1234", devices)).isEqualTo(dev)
        assertThat(provider.filterUsbDevices("0x12AB:1234", devices)).isEqualTo(dev) // hex mix
        assertThat(provider.filterUsbDevices("12AB:0x1234", devices)).isEqualTo(dev) // hex mix
        assertThat(provider.filterUsbDevices("4779:4660", devices)).isEqualTo(dev) // 0x12AB is 4779 decimal
    }

    @Test
    fun filterUsbDevices_requiresAllKeywords() {
        val dev = mkUsbDev(1659, 9123, "GlobalSat", "GPS Receiver")
        val devices = listOf(dev)

        assertThat(provider.filterUsbDevices("GlobalSat GPS", devices)).isEqualTo(dev)
        assertThat(provider.filterUsbDevices("GlobalSat Glonass", devices)).isNull()
        // Matches can be partial, and not ordered
        assertThat(provider.filterUsbDevices("Receiv Sat", devices)).isEqualTo(dev)
    }

}
