package com.alflabs.mpgpstrack.location

import com.alflabs.serial.SerialReader
import com.alflabs.serial.SerialWriter
import com.fasterxml.jackson.annotation.JsonInclude

data class LocationDataListPojo(
    val locations: List<LocationDataPojo>,
) {
    constructor(proto: LocationDataList) : this(
        proto.locationsList.map { LocationDataPojo(it) },
    )

    fun toProto(): LocationDataList = locationDataList {
        this@LocationDataListPojo.locations.forEach { locations.add(it.toProto()) }
    }

    fun toSerial(): SerialWriter {
        val writer = SerialWriter()
        writer.addInt("size", locations.size)
        var i = 1
        locations.forEach {
            val subWriter = SerialWriter()
            it.saveInto(subWriter)
            writer.addSerial("L$i", subWriter)
            i++
        }
        return writer
    }

    constructor(reader: SerialReader) : this(
        List(reader.getInt("size")) { index ->
            LocationDataPojo(reader.getSerial("L${index + 1}"))
        },
    )

}

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    fun toProto(): LocationData = locationData {
        latLong = this@LocationDataPojo.latLong.toProto()
        this@LocationDataPojo.speedBearing?.let { speedBearing = it.toProto() }
        this@LocationDataPojo.mp?.let { mp = it }
        flags = this@LocationDataPojo.flags
    }

    fun saveInto(writer: SerialWriter) {
        latLong.saveInto(writer)
        speedBearing?.saveInto(writer)
        mp?.let { writer.addFloat("mp", it) }
        writer.addInt("flags", flags)
    }

    constructor(reader: SerialReader) : this(
        LatLongDataPojo(reader),
        if (reader.hasName("speedBearing")) SpeedBearingDataPojo(reader) else null,
        if (reader.hasName("mp")) reader.getFloat("mp") else null,
        reader.getInt("flags"),
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    fun toProto(): LatLongData = latLongData {
        this@LatLongDataPojo.timestampMillis?.let { timestampMillis = it }
        this@LatLongDataPojo.latitude?.let { latitude = it }
        this@LatLongDataPojo.longitude?.let { longitude = it }
        this@LatLongDataPojo.accuracy?.let { accuracy = it }
    }

    fun saveInto(writer: SerialWriter) {
        timestampMillis?.let { writer.addLong("timestampMillis", it) }
        latitude?.let { writer.addDouble("latitude", it) }
        longitude?.let { writer.addDouble("longitude", it) }
        accuracy?.let { writer.addFloat("accuracy", it) }
    }

    constructor(reader: SerialReader) : this(
        if (reader.hasName("timestampMillis")) reader.getLong("timestampMillis") else null,
        if (reader.hasName("latitude")) reader.getDouble("latitude") else null,
        if (reader.hasName("longitude")) reader.getDouble("longitude") else null,
        if (reader.hasName("accuracy")) reader.getFloat("accuracy") else null,
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
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

    fun toProto(): SpeedBearingData = speedBearingData {
        this@SpeedBearingDataPojo.speedMps?.let { speedMps = it }
        this@SpeedBearingDataPojo.bearingDeg?.let { bearingDeg = it }
        this@SpeedBearingDataPojo.speedAcc?.let { speedAcc = it }
        this@SpeedBearingDataPojo.bearingAcc?.let { bearingAcc = it }
    }

    fun saveInto(writer: SerialWriter) {
        speedMps?.let { writer.addFloat("speedMps", it) }
        bearingDeg?.let { writer.addFloat("bearingDeg", it) }
        speedAcc?.let { writer.addFloat("speedAcc", it) }
        bearingAcc?.let { writer.addFloat("bearingAcc", it) }
    }

    constructor(reader: SerialReader) : this(
        if (reader.hasName("speedMps")) reader.getFloat("speedMps") else null,
        if (reader.hasName("bearingDeg")) reader.getFloat("bearingDeg") else null,
        if (reader.hasName("speedAcc")) reader.getFloat("speedAcc") else null,
        if (reader.hasName("bearingAcc")) reader.getFloat("bearingAcc") else null,
    )
}
