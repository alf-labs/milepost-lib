package com.alflabs.milepost.service

import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.utils.StringLogger
import com.github.petr_s.nmea.GpsSatellite
import com.github.petr_s.nmea.NMEAHandler
import com.github.petr_s.nmea.NMEAParser
import com.google.android.gms.location.LocationResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NMEAParserTest {

    companion object {
        private val TAG = NMEAParserTest::class.java.simpleName
    }

    private val logger = StringLogger(/*useSysOut=*/ true)
    private val locationResults = mutableListOf<LocationResult>()


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
        // https://github.com/ralfoide/android-nmea-parser/blob/main/src/test/java/com/github/petr_s/nmea/NMEAParserTest.java
        nmeaParser.parse($$"$GPRMC,163407.000,A,4851.4858,N,00217.6816,E,0.04,36.97,180416,,*35")
        nmeaParser.parse($$"$GPGGA,163407.000,4851.4858,N,00217.6816,E,1,07,1.7,285.7,M,45.5,M,,0000*52")
        assertThat(logger.string).contains("NMEA : Location[ExternalGps")
        assertThat(locationResults).hasSize(1)
        assertThat(locationResults.first().toString().replace(Regex("ert=[0-9: .-]+"), "ert=TEST")).isEqualTo(
            "LocationResult[{ExternalGps, 48.858097,2.294693±6.8m, alt=240.2m, spd=.02m/s, brg=36.97°, ert=TEST}]")
    }

    @Test
    fun testNmeaParser_GNdata() {
        nmeaParser.parse($$"$GNRMC,163407.000,A,4851.4858,N,00217.6816,E,0.04,36.97,180416,,*2B")
        nmeaParser.parse($$"$GNGGA,163407.000,4851.4858,N,00217.6816,E,1,07,1.7,285.7,M,45.5,M,,0000*4C")
        assertThat(logger.string).contains("NMEA : Location[ExternalGps")
        assertThat(locationResults).hasSize(1)
        assertThat(locationResults.first().toString().replace(Regex("ert=[0-9: .-]+"), "ert=TEST")).isEqualTo(
            "LocationResult[{ExternalGps, 48.858097,2.294693±6.8m, alt=240.2m, spd=.02m/s, brg=36.97°, ert=TEST}]")
    }

    @Test
    fun testNmeaParser_GNdata_v4_10() {
        // Parse data from a BU-353N5 with the NMEA 0183 4.10+ MRC format
        nmeaParser.parse($$"$GNGGA,081330.000,4851.4858,N,00217.6816,E,2,24,0.61,193.0,M,-25.6,M,,*5A")
        nmeaParser.parse($$"$GNRMC,081330.000,A,4851.4858,N,00217.6816,E,0.03,180.02,080826,,,D,V*0E")
        assertThat(logger.string).contains("NMEA : Location[ExternalGps")
        assertThat(locationResults).hasSize(1)
        assertThat(locationResults.first().toString().replace(Regex("ert=[0-9: .-]+"), "ert=TEST")).isEqualTo(
            "LocationResult[{ExternalGps, 48.858097,2.294693±2.44m, alt=218.6m, spd=.01m/s, brg=180.02°, ert=TEST}]")
    }
}
