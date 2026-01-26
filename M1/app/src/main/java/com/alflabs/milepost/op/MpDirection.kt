package com.alflabs.milepost.op

enum class MpDirection {
    /** Direction not known yet. */
    UNKNOWN,
    /** Direction is towards the start of the recorded GPS track. */
    TO_START,
    /** Direction is towards the end of the recorded GPS track. */
    TO_END,
}
