package com.alflabs.milepost.fragments.review

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackSummary
import com.google.common.truth.Expect
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

    @get:Rule
    val expect: Expect = Expect.create()

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
        expect.that(summary.hasMpMin()).isFalse()
        expect.that(summary.hasMpMax()).isFalse()
        expect.that(summary.lengthM).isEqualTo(0f)
        expect.that(summary.durationS).isEqualTo(0f)
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
        expect.that(summary.mpMin).isEqualTo(5f)
        expect.that(summary.mpMax).isEqualTo(5f)
        expect.that(summary.lengthM).isEqualTo(0f)
        expect.that(summary.durationS).isEqualTo(0f)
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
                    latitude = 1.01
                    longitude = 2.01
                    timestampMillis = 2000
                }
                mp = 11f
            },
            locationData {
                latLong = latLongData {
                    latitude = 1.02
                    longitude = 2.02
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
        expect.that(summary.mpMin).isEqualTo(10f)
        expect.that(summary.mpMax).isEqualTo(12f)
        expect.that(summary.durationS).isEqualTo(2f)
        expect.that(summary.lengthM).isWithin(0.1f).of(3_137.8306f)
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
                    latitude = 1.01
                    longitude = 2.01
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
        expect.that(summary.hasMpMin()).isFalse()
        expect.that(summary.hasMpMax()).isFalse()
        expect.that(summary.durationS).isEqualTo(1f)
        expect.that(summary.lengthM).isWithin(0.1f).of(1_568.9165f)
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
                    latitude = 1.01
                    longitude = 2.01
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
        expect.that(summary.mpMin).isEqualTo(99f) // Keeps existing mpMin
        expect.that(summary.mpMax).isEqualTo(100.1f)
        expect.that(summary.durationS).isEqualTo(1f) // Re-calculates duration
        expect.that(summary.lengthM).isWithin(0.0001f).of(1_568.9165f)
    }

    @Test
    fun convert_withNullLocationDataList() {
        val result = convert(null)
        assertThat(result).hasSize(1) // Header only
        assertThat(result[0].locIdx).isEqualTo(-1)
    }

    @Test
    fun convert_withEmptyLocationDataList() {
        val locationList = LocationDataList.getDefaultInstance()
        val result = convert(locationList)
        assertThat(result).hasSize(1) // Header only
        assertThat(result[0].locIdx).isEqualTo(-1)
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
        expect.that(result[0].locIdx).isEqualTo(-1) // Header
        expect.that(result[1].locIdx).isEqualTo(0)
        expect.that(result[1].data).isEqualTo(location)
        expect.that(result[1].deltaTS).isEqualTo(0)
        expect.that(result[1].distanceMiles).isEqualTo(0f)
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
                    latitude = 1.01
                    longitude = 2.01
                    timestampMillis = 2000
                }
            }
        )
        val locationList = LocationDataList.newBuilder().addAllLocations(locations).build()

        val result = convert(locationList)
        assertThat(result).hasSize(3)
        expect.that(result[0].locIdx).isEqualTo(-1) // Header

        expect.that(result[1].locIdx).isEqualTo(0)
        expect.that(result[1].data).isEqualTo(locations[0])
        expect.that(result[1].deltaTS).isEqualTo(0)
        expect.that(result[1].distanceMiles).isEqualTo(0f)

        expect.that(result[2].locIdx).isEqualTo(1)
        expect.that(result[2].data).isEqualTo(locations[1])
        expect.that(result[2].deltaTS).isEqualTo(1000)
        expect.that(result[2].distanceMiles).isWithin(0.1f).of(0.9748795f)
    }

    @Test
    fun computeInterpolatedMp_withLessThan2Locations() {
        val reviewList = convert(LocationDataList.getDefaultInstance())
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
                    latLong = latLongData { latitude = 1.01; longitude = 2.01; timestampMillis = 2000 }
                }
            )
        ).build()
        val reviewList = convert(locationList)
        assertThat(reviewList).hasSize(3)
        assertThat(reviewList[0].locIdx).isEqualTo(-1) // Header

        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNotNull()
        assertThat(result!!).hasSize(2)
        expect.that(result[0].locIdx).isEqualTo(0) // Header has been removed
        expect.that(result[0].data!!.mp).isEqualTo(0f)
        expect.that(result[1].data!!.mp).isWithin(0.1f).of(0.9748795f)
    }

    @Test
    fun computeInterpolatedMp_withNoInterpolationNeeded() {
        val locationList = LocationDataList.newBuilder().addAllLocations(
            listOf(
                locationData {
                    latLong = latLongData {
                        latitude = 1.0; longitude = 2.0; timestampMillis = 1000
                    }
                    mp = 10f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.01
                        longitude = 2.01
                        timestampMillis = 2000
                    }
                    mp = 11f
                    flags = LocationFlags.USER_MP.value
                }
            )
        ).build()
        val reviewList = convert(locationList)
        assertThat(reviewList).hasSize(3)
        assertThat(reviewList[0].locIdx).isEqualTo(-1) // Header

        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNull()
    }

    @Test
    fun computeInterpolatedMp_withInterpolation3() {

        val locationList = LocationDataList.newBuilder().addAllLocations(
            listOf(
                locationData {
                    latLong = latLongData {
                        latitude = 1.0; longitude = 2.0; timestampMillis = 1000
                    }
                    mp = 10f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.02; longitude = 2.0; timestampMillis = 3000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.04; longitude = 2.0; timestampMillis = 5000
                    }
                    mp = 14f
                    flags = LocationFlags.USER_MP.value
                }
            )
        ).build()
        val reviewList = convert(locationList)
        assertThat(reviewList).hasSize(4)
        assertThat(reviewList[0].locIdx).isEqualTo(-1) // Header

        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNotNull()
        assertThat(result!!).hasSize(3)
        expect.that(result[0].locIdx).isEqualTo(0) // Header has been removed
        expect.that(result[0].data!!.mp).isEqualTo(10f)
        expect.that(result[1].locIdx).isEqualTo(1)
        expect.that(result[1].data!!.mp).isWithin(0.1f).of(12f)
        expect.that(result[2].locIdx).isEqualTo(2)
        expect.that(result[2].data!!.mp).isEqualTo(14f)
    }

    @Test
    fun computeInterpolatedMp_withInterpolation5() {

        val locationList = LocationDataList.newBuilder().addAllLocations(
            listOf(
                locationData {
                    latLong = latLongData {
                        latitude = 1.0; longitude = 2.0; timestampMillis = 1000
                    }
                    mp = 10f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.01; longitude = 2.0; timestampMillis = 2000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.02; longitude = 2.0; timestampMillis = 3000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.03; longitude = 2.0; timestampMillis = 4000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.04; longitude = 2.0; timestampMillis = 5000
                    }
                    mp = 14f
                    flags = LocationFlags.USER_MP.value
                }
            )
        ).build()
        val reviewList = convert(locationList)
        assertThat(reviewList).hasSize(6)
        assertThat(reviewList[0].locIdx).isEqualTo(-1) // Header

        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNotNull()
        assertThat(result!!).hasSize(5)
        expect.that(result[0].locIdx).isEqualTo(0) // Header has been removed
        expect.that(result[0].data!!.mp).isEqualTo(10f)
        expect.that(result[1].locIdx).isEqualTo(1)
        expect.that(result[1].data!!.mp).isWithin(0.1f).of(11f)
        expect.that(result[2].locIdx).isEqualTo(2)
        expect.that(result[2].data!!.mp).isWithin(0.1f).of(12f)
        expect.that(result[3].locIdx).isEqualTo(3)
        expect.that(result[3].data!!.mp).isWithin(0.1f).of(13f)
        expect.that(result[4].locIdx).isEqualTo(4)
        expect.that(result[4].data!!.mp).isEqualTo(14f)
    }

    @Test
    fun computeInterpolatedMp_withInterpolation7() {

        val locationList = LocationDataList.newBuilder().addAllLocations(
            listOf(
                locationData {
                    latLong = latLongData {
                        latitude = 1.0; longitude = 2.0; timestampMillis = 1000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.01; longitude = 2.0; timestampMillis = 2000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.02; longitude = 2.0; timestampMillis = 3000
                    }
                    mp = 12f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.03; longitude = 2.0; timestampMillis = 4000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.04; longitude = 2.0; timestampMillis = 5000
                    }
                    mp = 14f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.05; longitude = 2.0; timestampMillis = 6000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.06; longitude = 2.0; timestampMillis = 7000
                    }
                    mp = 16f
                    flags = LocationFlags.USER_MP.value
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.07; longitude = 2.0; timestampMillis = 8000
                    }
                },
                locationData {
                    latLong = latLongData {
                        latitude = 1.08; longitude = 2.0; timestampMillis = 9000
                    }
                }
            )
        ).build()
        val reviewList = convert(locationList)
        assertThat(reviewList).hasSize(10)
        assertThat(reviewList[0].locIdx).isEqualTo(-1) // Header

        val result = computeInterpolatedMp(reviewList)
        assertThat(result).isNotNull()
        assertThat(result!!).hasSize(9)
        expect.that(result[0].locIdx).isEqualTo(0) // Header has been removed
        expect.that(result[0].data!!.mp).isWithin(0.1f).of(10.625842f)
        expect.that(result[1].locIdx).isEqualTo(1)
        expect.that(result[1].data!!.mp).isWithin(0.1f).of(11.312922f)
        expect.that(result[2].locIdx).isEqualTo(2)
        expect.that(result[2].data!!.mp).isEqualTo(12f)
        expect.that(result[3].locIdx).isEqualTo(3)
        expect.that(result[3].data!!.mp).isWithin(0.1f).of(13f)
        expect.that(result[4].locIdx).isEqualTo(4)
        expect.that(result[4].data!!.mp).isEqualTo(14f)
        expect.that(result[5].locIdx).isEqualTo(5)
        expect.that(result[5].data!!.mp).isWithin(0.1f).of(15f)
        expect.that(result[6].locIdx).isEqualTo(6)
        expect.that(result[6].data!!.mp).isEqualTo(16f)
        expect.that(result[7].locIdx).isEqualTo(7)
        expect.that(result[7].data!!.mp).isWithin(0.1f).of(16.68708f)
        expect.that(result[8].locIdx).isEqualTo(8)
        expect.that(result[8].data!!.mp).isWithin(0.1f).of(17.374159f)
    }

    @Test
    fun computeHintStartEndLabels_withExistingStartLabel() {
        val (start, end) = computeHintStartEndLabels("start", null, null, null)
        expect.that(start).isEqualTo("start")
        expect.that(end).isEqualTo("")
    }

    @Test
    fun computeHintStartEndLabels_withExistingEndLabel() {
        val (start, end) = computeHintStartEndLabels(null, "end", null, null)
        expect.that(start).isEqualTo("")
        expect.that(end).isEqualTo("end")
    }

    @Test
    fun computeHintStartEndLabels_withNullLocations() {
        val (start, end) = computeHintStartEndLabels(null, null, null, null)
        expect.that(start).isEqualTo("")
        expect.that(end).isEqualTo("")
    }

    @Test
    fun computeHintStartEndLabels_withSameLocations() {
        val loc = locationData {
            latLong = latLongData {
                latitude = 1.0
                longitude = 2.0
            }
        }
        val (start, end) = computeHintStartEndLabels(null, null, loc, loc)
        expect.that(start).isEqualTo("")
        expect.that(end).isEqualTo("")
    }

    @Test
    fun computeHintStartEndLabels_Northbound() {
        val loc1 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.0 } }
        val loc2 = locationData { latLong = latLongData { latitude = 1.1; longitude = 2.0 } }
        val (start, end) = computeHintStartEndLabels(null, null, loc1, loc2)
        expect.that(start).isEqualTo("Southbound")
        expect.that(end).isEqualTo("Northbound")
    }

    @Test
    fun computeHintStartEndLabels_Eastbound() {
        val loc1 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.0 } }
        val loc2 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.1 } }
        val (start, end) = computeHintStartEndLabels(null, null, loc1, loc2)
        expect.that(start).isEqualTo("Westbound")
        expect.that(end).isEqualTo("Eastbound")
    }

    @Test
    fun computeHintStartEndLabels_Southbound() {
        val loc1 = locationData { latLong = latLongData { latitude = 1.1; longitude = 2.0 } }
        val loc2 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.0 } }
        val (start, end) = computeHintStartEndLabels(null, null, loc1, loc2)
        expect.that(start).isEqualTo("Northbound")
        expect.that(end).isEqualTo("Southbound")
    }

    @Test
    fun computeHintStartEndLabels_Westbound() {
        val loc1 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.1 } }
        val loc2 = locationData { latLong = latLongData { latitude = 1.0; longitude = 2.0 } }
        val (start, end) = computeHintStartEndLabels(null, null, loc1, loc2)
        expect.that(start).isEqualTo("Eastbound")
        expect.that(end).isEqualTo("Westbound")
    }
}
