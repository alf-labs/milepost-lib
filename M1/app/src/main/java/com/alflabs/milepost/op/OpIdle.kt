package com.alflabs.milepost.op

class OpIdle : IOpActor {
    override val currentTrackId: Long?
        get() = null

    override fun stop() {
        // no-op
    }
}
