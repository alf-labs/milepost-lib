package com.alflabs.milepostlib.op

/** An operation actor (implementation) that can handle Docent entities and triggers. */
interface IOpDocentActor : IOpGpsActor {
    val currentDocentTrackId: Long?
}
