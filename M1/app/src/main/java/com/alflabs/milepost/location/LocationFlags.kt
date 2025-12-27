package com.alflabs.milepost.location

enum class LocationFlags(val value: Int) {
    USER_MP(1),
    IGNORE(2),
    STREAMLINE(4),
    ;
}

fun Int.hasFlag(flag: LocationFlags): Boolean = (this and flag.value) != 0
fun Int.setFlag(flag: LocationFlags): Int = this or flag.value
fun Int.clearFlag(flag: LocationFlags): Int = this and flag.value.inv()
fun Int.changeFlag(flag: LocationFlags, value: Boolean): Int =
    if (value) setFlag(flag) else clearFlag(flag)
