package com.alflabs.milepostlib.utils

fun Int.pluralize(single: String, many: String): String =
    when(this) {
        1 -> single
        else -> many
    }
