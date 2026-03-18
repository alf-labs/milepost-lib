package com.alflabs.milepost.utils

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit

object Time {
    @SuppressLint("DefaultLocale")
    fun formatMillis(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60

        return if (hours > 0) {
            String.format("%dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format("%dm %02ds", minutes, seconds)
        }
    }

}
