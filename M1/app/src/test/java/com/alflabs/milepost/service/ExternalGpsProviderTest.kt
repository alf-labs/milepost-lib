package com.alflabs.milepost.service

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
        assertThat(provider.filterUsbDevices(emptyList())).isNull()
        assertThat(provider.filterUsbDevices(listOf(null))).isNull()
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

        val result = provider.filterUsbDevices(listOf(matchingDevice))

        assertThat(result).isEqualTo(matchingDevice)
    }

}
