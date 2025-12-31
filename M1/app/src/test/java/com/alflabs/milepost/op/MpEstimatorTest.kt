package com.alflabs.milepost.op

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MpEstimatorTest {

    private val estimator = MpEstimator()

    @Test
    fun projectOnLine() {
        val a =latLongData {
            latitude = 1.0
            longitude = 2.0
            timestampMillis = 1000
        }
        val b = latLongData {
            latitude = 1.02
            longitude = 2.0
            timestampMillis = 1000
        }

        // Test a point on the line segment
        var c = latLongData {
            latitude = 1.01
            longitude = 2.0
            timestampMillis = 1000
        }
        var t = estimator.projectOnLine(c, a, b)
        assertThat(t).isWithin(1e-6).of(0.5)

        // Test a point on the line, but outside the segment
        c = latLongData {
            latitude = 1.03
            longitude = 2.0
            timestampMillis = 1000
        }
        t = estimator.projectOnLine(c, a, b)
        assertThat(t).isWithin(1e-6).of(1.5)

        // Test a point off the line
        c = latLongData {
            latitude = 1.01
            longitude = 2.01
            timestampMillis = 1000
        }
        t = estimator.projectOnLine(c, a, b)
        assertThat(t).isWithin(1e-6).of(0.5)

        // Test with same start and end points
        c = latLongData {
            latitude = 1.01
            longitude = 2.01
            timestampMillis = 1000
        }
        val d = latLongData {
            latitude = 1.0
            longitude = 2.0
            timestampMillis = 1000
        }
        t = estimator.projectOnLine(c, a, d)
        assertThat(t).isNull()
    }

    private val locationList3 = listOf(
        locationData {
            flags = 0
            mp = 10.0f
            latLong = latLongData { latitude = 1.0; longitude = 1.0 }
        },
        locationData {
            flags = 0
            mp = 20.0f
            latLong = latLongData { latitude = 2.0; longitude = 1.0 }
        },
        locationData {
            flags = 0
            mp = 30.0f
            latLong = latLongData { latitude = 3.0; longitude = 1.0 }
        }
    )

    private val locationList5 = listOf(
        locationData {
            flags = 0
            mp = 10.0f
            latLong = latLongData { latitude = 1.0; longitude = 1.0 }
        },
        locationData {
            flags = 0
            mp = 20.0f
            latLong = latLongData { latitude = 2.0; longitude = 2.0 }
        },
        locationData {
            flags = 0
            mp = 30.0f
            latLong = latLongData { latitude = 3.0; longitude = 3.0 }
        },
        locationData {
            flags = 0
            mp = 40.0f
            latLong = latLongData { latitude = 4.0; longitude = 4.0 }
        },
        locationData {
            flags = 0
            mp = 50.0f
            latLong = latLongData { latitude = 5.0; longitude = 5.0 }
        }
    )

    @Test
    fun interpolateMp_atExactPoints() {
        val locations = locationList3

        // Start
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 1.0; longitude = 1.1 })
        ).isWithin(0.01f).of(10f)

        // Mid
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 2.0; longitude = 1.1 })
        ).isWithin(0.01f).of(20f)

        // End
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 3.0; longitude = 1.1 })
        ).isWithin(0.01f).of(30f)
    }

    @Test
    fun interpolateMp_betweenPoints() {
        val locations = locationList3

        // Start-Mid
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 1.2; longitude = -2.0 })
        ).isWithin(0.01f).of(12f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 1.5; longitude = 2.0 })
        ).isWithin(0.01f).of(15f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 1.7; longitude = 3.0 })
        ).isWithin(0.01f).of(17f)

        // Mid-End
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 2.2; longitude = -2.0 })
        ).isWithin(0.01f).of(22f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 2.5; longitude = 2.0 })
        ).isWithin(0.01f).of(25f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 2.7; longitude = 3.0 })
        ).isWithin(0.01f).of(27f)
    }

    @Test
    fun interpolateMp_beyondPoints() {
        val locations = locationList3

        // Start-Mid
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 0.0; longitude = -1.0 })
        ).isWithin(0.01f).of(0f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 0.5; longitude = 0.0 })
        ).isWithin(0.01f).of(5f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 0.9; longitude = 1.0 })
        ).isWithin(0.01f).of(9f)

        // Mid-End
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 3.1; longitude = -1.0 })
        ).isWithin(0.01f).of(31f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 3.5; longitude = 0.0 })
        ).isWithin(0.01f).of(35f)
        assertThat(
            estimator.interpolateMp(locations, 1, latLongData { latitude = 4.0; longitude = 1.0 })
        ).isWithin(0.01f).of(40f)
    }

    @Test
    fun findFirstNearest() {
        val locations = locationList5

        // A point near the second location
        var target = latLongData { latitude = 2.1; longitude = 2.1 }
        var nearest = estimator.findFirstNearest(locations, target)
        assertThat(nearest).isEqualTo(1)

        // A point near the start of the list
        target = latLongData { latitude = 1.1; longitude = 1.1 }
        nearest = estimator.findFirstNearest(locations, target)
        assertThat(nearest).isEqualTo(0)

        // A point near the end of the list
        target = latLongData { latitude = 4.9; longitude = 4.9 }
        nearest = estimator.findFirstNearest(locations, target)
        assertThat(nearest).isEqualTo(4)

        // A point equidistant from two points, either 1 or 2 are good results
        target = latLongData { latitude = 2.5; longitude = 2.5 }
        nearest = estimator.findFirstNearest(locations, target)
        assertThat(nearest).isEqualTo(2)
    }

    @Test
    fun examineNearby() {
        val locations = locationList5

        // Target is closer to index 2
        val target2 = latLongData { latitude = 2.9; longitude = 2.9 }

        var nearestIndex = estimator.examineNearby(locations, target2, 4)
        assertThat(nearestIndex).isEqualTo(2)
        nearestIndex = estimator.examineNearby(locations, target2, 3)
        assertThat(nearestIndex).isEqualTo(2)
        nearestIndex = estimator.examineNearby(locations, target2, 2)
        assertThat(nearestIndex).isEqualTo(2)
        nearestIndex = estimator.examineNearby(locations, target2, 1)
        assertThat(nearestIndex).isEqualTo(2)
        nearestIndex = estimator.examineNearby(locations, target2, 0)
        assertThat(nearestIndex).isEqualTo(2)

        // Target is closer to index 1
        val target1 = latLongData { latitude = 2.1; longitude = 2.1 }

        nearestIndex = estimator.examineNearby(locations, target1, 4)
        assertThat(nearestIndex).isEqualTo(1)
        nearestIndex = estimator.examineNearby(locations, target1, 3)
        assertThat(nearestIndex).isEqualTo(1)
        nearestIndex = estimator.examineNearby(locations, target1, 2)
        assertThat(nearestIndex).isEqualTo(1)
        nearestIndex = estimator.examineNearby(locations, target1, 1)
        assertThat(nearestIndex).isEqualTo(1)
        nearestIndex = estimator.examineNearby(locations, target1, 0)
        assertThat(nearestIndex).isEqualTo(1)
    }
}
