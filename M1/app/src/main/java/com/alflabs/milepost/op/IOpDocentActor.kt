package com.alflabs.milepost.op

/** An operation actor (implementation) that can handle Docent entities and triggers. */
interface IOpDocentActor : IOpGpsActor {
    val currentDocentTrackId: Long?
}
