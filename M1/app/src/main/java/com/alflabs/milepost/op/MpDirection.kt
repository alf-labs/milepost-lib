package com.alflabs.milepost.op

enum class MpDirection(val value: Int) {
    UNKNOWN(0),
    TO_START(-1),
    TO_END(1);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}
