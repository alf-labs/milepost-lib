package com.alflabs.milepost.op

import com.alflabs.milepost.location.latLongData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
