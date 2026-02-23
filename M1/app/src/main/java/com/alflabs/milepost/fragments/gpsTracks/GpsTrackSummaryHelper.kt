package com.alflabs.milepost.fragments.gpsTracks

import com.alflabs.milepost.db.GpsTrackSummary
import com.alflabs.milepost.utils.Units

fun GpsTrackSummary?.toText(useImperialUnits: Boolean): CharSequence {
    val s = this ?: return "No summary found"
    return String.format(
        "MP: %s - %s, %s %s, %s s",
        if (s.hasMpMin()) String.format("%.2f", s.mpMin) else "??",
        if (s.hasMpMax()) String.format("%.2f", s.mpMax) else "??",
        if (s.hasLengthM()) String.format("%.2f", Units.mToUnit(s.lengthM, useImperialUnits)) else "??",
        Units.mUnitName(useImperialUnits),
        s.durationS,
    )
}

