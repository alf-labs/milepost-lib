package com.alflabs.mpgpstrack.location

data class LocationDataListPojo(
    val locations: List<LocationDataPojo>,
) {
    constructor(proto: LocationDataList) : this(
        proto.locationsList.map { LocationDataPojo(it) },
    )
}

data class LocationDataPojo(
    val latLong: LatLongDataPojo,
    val speedBearing: SpeedBearingDataPojo? = null,
    val mp: Float? = null,
    val flags: Int = 0,
) {
    constructor(proto: LocationData) : this(
        LatLongDataPojo(proto.latLong),
        if (proto.hasSpeedBearing()) SpeedBearingDataPojo(proto.speedBearing) else null,
        if (proto.hasMp()) proto.mp else null,
        proto.flags,
    )
}

data class LatLongDataPojo(
    val timestampMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
) {
    constructor(proto: LatLongData) : this(
        proto.timestampMillis,
        proto.latitude,
        proto.longitude,
        if (proto.hasAccuracy()) proto.accuracy else null,
    )
}

data class SpeedBearingDataPojo(
    val speedMps: Float? = null,
    val bearingDeg: Float? = null,
    val speedAcc: Float? = null,
    val bearingAcc: Float? = null,
) {
    constructor(proto: SpeedBearingData) : this(
        proto.speedMps,
        proto.bearingDeg,
        if (proto.hasSpeedAcc()) proto.speedAcc else null,
        if (proto.hasBearingAcc()) proto.bearingAcc else null,
    )
}
