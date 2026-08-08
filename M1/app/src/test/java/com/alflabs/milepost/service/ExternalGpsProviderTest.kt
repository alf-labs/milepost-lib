package com.alflabs.milepost.service

import android.content.Context
import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.utils.ILogger
import com.github.petr_s.nmea.GpsSatellite
import com.github.petr_s.nmea.NMEAHandler
import com.github.petr_s.nmea.NMEAParser
import com.google.android.gms.location.LocationResult
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private val locationResults = mutableListOf<LocationResult>()


    @Before
    fun setup() {
        hiltRule.inject()
    }

    val nmeaParser = NMEAParser(object : NMEAHandler {
        override fun onStart() {}
        override fun onFinish() {}
        override fun onLocation(location: Location) {
            location.provider = "ExternalGps"
            logger.d(TAG, "NMEA : $location")
            locationResults.add(LocationResult.create(listOf(location)))
        }

        override fun onSatellites(satellites: MutableList<GpsSatellite>?) {}
        override fun onUnrecognized(sentence: String?) {}
        override fun onBadChecksum(expected: Int, actual: Int) {
            logger.d(TAG, "NMEA Bad checksum: expected $expected, actual $actual")
        }

        override fun onException(e: Exception?) {
            logger.d(TAG, "NMEA Parse exception: ${e?.message}")
        }
    })

    @Test
    fun testNmeaParser_GPdata() {
        // Parse test example from
        // https://github.com/petr-s/android-nmea-parser/blob/master/src/test/java/com/github/petr_s/nmea/NMEAParserTest.java
        nmeaParser.parse($$"$GPRMC,163407.000,A,5004.7485,N,01423.8956,E,0.04,36.97,180416,,*38")
        nmeaParser.parse($$"$GPGGA,163407.000,5004.7485,N,01423.8956,E,1,07,1.7,285.7,M,45.5,M,,0000*5F")
        assertThat(locationResults).hasSize(1)
        assertThat(locationResults.first().toString().replace(Regex("ert=[0-9: .-]+"), "ert=TEST")).isEqualTo(
            "LocationResult[{ExternalGps, 50.079142,14.398260±6.8m, alt=240.2m, spd=.02m/s, brg=36.97°, ert=TEST}]")
    }

    @Test
    fun testNmeaParser_GNdata() {
        nmeaParser.parse($$"$GNRMC,163407.000,A,5004.7485,N,01423.8956,E,0.04,36.97,180416,,*26")
        nmeaParser.parse($$"$GNGGA,163407.000,5004.7485,N,01423.8956,E,1,07,1.7,285.7,M,45.5,M,,0000*41")
        assertThat(locationResults).hasSize(1)
        assertThat(locationResults.first().toString().replace(Regex("ert=[0-9: .-]+"), "ert=TEST")).isEqualTo(
            "LocationResult[{ExternalGps, 50.079142,14.398260±6.8m, alt=240.2m, spd=.02m/s, brg=36.97°, ert=TEST}]")
    }

}
