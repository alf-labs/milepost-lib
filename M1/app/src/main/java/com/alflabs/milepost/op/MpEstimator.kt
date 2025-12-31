package com.alflabs.milepost.op

import com.alflabs.milepost.location.LatLongData
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDistance
import kotlin.math.abs

class MpEstimator {
    private val locationDistance = LocationDistance()

    internal fun findFirstNearest(
        locations: List<LocationData>,
        newLatLong: LatLongData
    ): Int {
        val sz = locations.size

        if (sz == 1) {
            return 0
        }

        var minDist = Float.MAX_VALUE
        var minIndex = Int.MAX_VALUE

        // TBD optimize with a binary search of a minima. This is a good simple start.
        for ((index, loc) in locations.withIndex()) {
            val dist = locationDistance.between(newLatLong, loc.latLong)
            if (dist < minDist) {
                minDist = dist
                minIndex = index
            }
        }

        return minIndex
    }

    internal fun examineNearby(
        locations: List<LocationData>,
        newLatLong: LatLongData,
        startIndex: Int
    ): Int {
        val sz = locations.size

        val startDist = locationDistance.between(
            newLatLong,
            locations[startIndex].latLong
        )

        // Case 1 : Direction + 1
        var upIndex = startIndex
        var minDist = startDist
        var index = startIndex + 1
        while (index < sz) {
            val dist = locationDistance.between(
                newLatLong,
                locations[index].latLong
            )
            if (dist < minDist) {
                minDist = dist
                upIndex = index
                index++
            } else {
                break
            }
        }

        if (upIndex > startIndex) {
            return upIndex
        }

        // Case 2 : Direction - 1
        var dwIndex = startIndex
        minDist = startDist
        index = startIndex - 1
        while (index >= 0) {
            val dist = locationDistance.between(
                newLatLong,
                locations[index].latLong
            )
            if (dist < minDist) {
                minDist = dist
                dwIndex = index
                index++
            } else {
                break
            }
        }

        if (dwIndex < startIndex) {
            return dwIndex
        }

        return startIndex
    }

    internal fun interpolateMp(
        locations: List<LocationData>,
        midIndex: Int,
        newLatLong: LatLongData,
    ) : Float {
        val sz = locations.size
        val midLoc = locations[midIndex]
        val midMp = midLoc.mp

        if (midIndex == 0 || midIndex == sz - 1) {
            return midMp
        }

        // Try upper segment
        val upLoc = locations[midIndex + 1]
        val upT = projectOnLine(newLatLong, midLoc.latLong, upLoc.latLong)
        val upMp = upT?.let { t -> midMp + (upLoc.mp - midMp) * t.toFloat() }
        if (upT != null && upMp != null && upT in 0.0..1.0) {
            return upMp
        }

        // Try lower segment
        val downLoc = locations[midIndex - 1]
        val downT = projectOnLine(newLatLong, midLoc.latLong, downLoc.latLong)
        val downMp = downT?.let { t -> midMp + (downLoc.mp - midMp) * t.toFloat() }
        if (downT != null && downMp != null && downT in 0.0..1.0) {
            return downMp
        }

        // In the unlikely case both are out of bounds, use the closest value.
        if (upT != null && downT != null && upMp != null && downMp != null) {
            return if (abs(upT) < abs(downT)) upMp else downMp
        }

        // Give up. We should not hit this case. Just return something.
        return midMp
    }

    /**
     * Compute the scalar projection of a 2D point C onto the line going through A-B.
     * Returns a scalar in the range 0..1 matching A..B,
     * or outside if out of bounds,
     * or null A & B are too close.
     */
    internal fun projectOnLine(
        c: LatLongData,
        a: LatLongData,
        b: LatLongData
    ) : Double? {
        // Projection of C onto a line going through A->B:
        // P = A + t(B - A)
        // Vector v = line direction = B - A
        // Vector u = point to origin = C - A
        // The scalar is the dot product of both vectors:
        // t = ((C - A) . (B - A)) / ||B - A||^2
        //
        // For simplification, we directly use latitude = X, longitude = Y.
        // Since all the points are fairly close, we can ignore sphericity.

        val denominator =
            (b.latitude - a.latitude) * (b.latitude - a.latitude) +
                    (b.longitude - a.longitude) * (b.longitude - a.longitude)

        if (denominator < 1e-6) {
            return null
        }

        val numerator =
            (c.latitude - a.latitude) * (b.latitude - a.latitude) +
                    (c.longitude - a.longitude) * (b.longitude - a.longitude)

        return numerator / denominator
    }
}
