package com.alflabs.mpgpstrack.fragments.tracks

import com.alflabs.mpgpstrack.tracks.TrackSummary
import com.alflabs.mpgpstrack.utils.Units

fun TrackSummary?.toText(useImperialUnits: Boolean): CharSequence {
    val s = this ?: return "No summary found"
    return String.format(
        "MP: %s - %s, %s %s, %s s",
        if (s.hasMpMin()) String.format("%.1f", s.mpMin) else "??",
        if (s.hasMpMax()) String.format("%.1f", s.mpMax) else "??",
        if (s.hasLengthM()) String.format("%.1f", Units.mToUnit(s.lengthM, useImperialUnits)) else "??",
        Units.mUnitName(useImperialUnits),
        s.durationS,
    )
}

