package com.alflabs.milepost.fragments.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackSummary
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class ReviewFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun computeTrackSummary_withEmptyTrack() {
        val track = TrackEntity(
            id = 1,
            createdDateISO = "2021-02-03T04:05:06Z",
        )
        val summary = computeTrackSummary(TrackSummary.getDefaultInstance(), track)
        assertThat(summary.hasMpMin()).isFalse()
        assertThat(summary.hasMpMax()).isFalse()
        assertThat(summary.lengthM).isEqualTo(0f)
        assertThat(summary.durationS).isEqualTo(0f)
    }

    @Test
    fun computeTrackSummary_withSingleLocation() {
        val location = locationData {
            latLong = latLongData {
                latitude = 10.0
                longitude = 10.0
                timestampMillis = 1000
            }
            mp = 5f
        }
        val locationList = LocationDataList.newBuilder().addLocations(location).build()
        val track = TrackEntity(
            id = 1,
            locationList = locationList,
            createdDateISO = "2021-02-03T04:05:06Z",
        )

        val summary = computeTrackSummary(TrackSummary.getDefaultInstance(), track)
        assertThat(summary.mpMin).isEqualTo(5f)
        assertThat(summary.mpMax).isEqualTo(5f)
        assertThat(summary.lengthM).isEqualTo(0f)
        assertThat(summary.durationS).isEqualTo(0f)
    }

    @Test
    fun computeTrackSummary_withMultipleLocations() {
        val locations = listOf(
            locationData {
                latLong = latLongData {
                    latitude = 1.0
                    longitude = 2.0
                    timestampMillis = 1000
                }
                mp = 10f
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.0001
                    longitude = 2.0001
                    timestampMillis = 2000
                }
                mp = 11f
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.0002
                    longitude = 2.0002
                    timestampMillis = 3000
                }
                mp = 12f
            }
        )
        val locationList = LocationDataList.newBuilder().addAllLocations(locations).build()
        val track = TrackEntity(
            id = 1,
            locationList = locationList,
            createdDateISO = "2021-02-03T04:05:06Z",
            )

        val summary = computeTrackSummary(TrackSummary.getDefaultInstance(), track)
        assertThat(summary.mpMin).isEqualTo(10f)
        assertThat(summary.mpMax).isEqualTo(12f)
        assertThat(summary.durationS).isEqualTo(2f)
        assertThat(summary.lengthM).isGreaterThan(0f)
    }

     @Test
    fun computeTrackSummary_withNoMpValues() {
        val locations = listOf(
            locationData {
                latLong = latLongData {
                    latitude = 1.0
                    longitude = 2.0
                    timestampMillis = 1000
                }
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.0001
                    longitude = 2.0001
                    timestampMillis = 2000
                }
            }
        )
        val locationList = LocationDataList.newBuilder().addAllLocations(locations).build()
        val track = TrackEntity(
            id = 1,
            locationList = locationList,
            createdDateISO = "2021-02-03T04:05:06Z",
        )

        val summary = computeTrackSummary(TrackSummary.getDefaultInstance(), track)
        assertThat(summary.hasMpMin()).isFalse()
        assertThat(summary.hasMpMax()).isFalse()
        assertThat(summary.durationS).isEqualTo(1f)
        assertThat(summary.lengthM).isGreaterThan(0f)
    }

    @Test
    fun computeTrackSummary_withExistingSummary() {
        val locations = listOf(
            locationData {
                latLong = latLongData {
                    latitude = 1.0
                    longitude = 2.0
                    timestampMillis = 1000
                }
                mp = 100f
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.0001
                    longitude = 2.0001
                    timestampMillis = 2000
                }
                mp = 100.1f
            }
        )
        val locationList = LocationDataList.newBuilder().addAllLocations(locations).build()
        val track = TrackEntity(
            id = 1,
            locationList = locationList,
            createdDateISO = "2021-02-03T04:05:06Z",
        )

        val currentSummary = TrackSummary.newBuilder().setMpMin(99f).setDurationS(5f).build()

        val summary = computeTrackSummary(currentSummary, track)
        assertThat(summary.mpMin).isEqualTo(99f) // Keeps existing mpMin
        assertThat(summary.mpMax).isEqualTo(100.1f)
        assertThat(summary.durationS).isEqualTo(1f) // Re-calculates duration
        assertThat(summary.lengthM).isGreaterThan(0f)
    }

    @Test
    fun convert_withNullLocationDataList() {
        val result = convert(null)
        assertThat(result).hasSize(1) // Header only
        assertThat(result[0].index).isEqualTo(-1)
    }

    @Test
    fun convert_withEmptyLocationDataList() {
        val locationList = LocationDataList.getDefaultInstance()
        val result = convert(locationList)
        assertThat(result).hasSize(1) // Header only
        assertThat(result[0].index).isEqualTo(-1)
    }

    @Test
    fun convert_withSingleLocation() {
        val location = locationData {
            latLong = latLongData {
                latitude = 10.0
                longitude = 10.0
                timestampMillis = 1000
            }
        }
        val locationList = LocationDataList.newBuilder().addLocations(location).build()

        val result = convert(locationList)
        assertThat(result).hasSize(2)
        assertThat(result[0].index).isEqualTo(-1) // Header
        assertThat(result[1].index).isEqualTo(0)
        assertThat(result[1].data).isEqualTo(location)
        assertThat(result[1].deltaTS).isEqualTo(0)
        assertThat(result[1].distanceMeters).isEqualTo(0f)
    }

    @Test
    fun convert_withMultipleLocations() {
        val locations = listOf(
            locationData {
                latLong = latLongData {
                    latitude = 1.0
                    longitude = 2.0
                    timestampMillis = 1000
                }
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.0001
                    longitude = 2.0001
                    timestampMillis = 2000
                }
            }
        )
        val locationList = LocationDataList.newBuilder().addAllLocations(locations).build()

        val result = convert(locationList)
        assertThat(result).hasSize(3)
        assertThat(result[0].index).isEqualTo(-1) // Header

        assertThat(result[1].index).isEqualTo(0)
        assertThat(result[1].data).isEqualTo(locations[0])
        assertThat(result[1].deltaTS).isEqualTo(0)
        assertThat(result[1].distanceMeters).isEqualTo(0f)

        assertThat(result[2].index).isEqualTo(1)
        assertThat(result[2].data).isEqualTo(locations[1])
        assertThat(result[2].deltaTS).isEqualTo(1000)
        assertThat(result[2].distanceMeters).isGreaterThan(0f)
        assertThat(result[2].distanceMiles).isGreaterThan(0f)
    }

    @Test
    fun computeInterpolatedMp_withLessThan2Locations() {
        val reviewList = listOf(ReviewLocationData(index = 0, data = locationData { }))
        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNull()
    }

    @Test
    fun computeInterpolatedMp_withNoUserMPs() {
        val locationList = LocationDataList.newBuilder().addAllLocations(
            listOf(
                locationData {
                    latLong = latLongData { latitude = 1.0; longitude = 2.0; timestampMillis = 1000 }
                },
                locationData {
                    latLong = latLongData { latitude = 1.001; longitude = 2.001; timestampMillis = 2000 }
                }
            )
        ).build()
        val reviewList = convert(locationList)

        val result = computeInterpolatedMp(reviewList.filter{it.index >=0})
        assertThat(result).isNotNull()
        assertThat(result).hasSize(2)
        assertThat(result!![0].data!!.mp).isEqualTo(0f)
        assertThat(result[1].data!!.mp).isGreaterThan(result[0].data!!.mp)
    }

    @Test
    fun computeInterpolatedMp_withNoInterpolationNeeded() {
        val locations = listOf(
            ReviewLocationData(
                index = 0,
                data = locationData {
                    latLong = latLongData { timestampMillis = 1000 }
                    mp = 100f
                    flags = LocationFlags.USER_MP.value
                },
                distanceMiles = 0f
            ),
            ReviewLocationData(
                index = 1,
                data = locationData {
                    latLong = latLongData { timestampMillis = 2000 }
                    mp = 101f
                    flags = LocationFlags.USER_MP.value
                },
                distanceMiles = 1f
            )
        )
        val result = computeInterpolatedMp(locations)
        assertThat(result).isNull()
    }
}
