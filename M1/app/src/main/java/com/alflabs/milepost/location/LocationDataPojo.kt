package com.alflabs.milepost.location

import com.alflabs.serial.SerialReader
import com.alflabs.serial.SerialWriter
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class LocationDataListPojo @JsonCreator constructor(
    @param:JsonProperty("locations") val locations: List<LocationDataPojo>,
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
data class LocationDataPojo @JsonCreator constructor(
    @param:JsonProperty("latLong")      val latLong: LatLongDataPojo,
    @param:JsonAlias("speedBearing")
    @param:JsonProperty("speed")        val speed: SpeedDataPojo? = null,
    @param:JsonProperty("mp")           val mp: Float? = null,
    @param:JsonProperty("flags")        val flags: Int = 0,
) {
    constructor(proto: LocationData) : this(
        LatLongDataPojo(proto.latLong),
        if (proto.hasSpeed()) SpeedDataPojo(proto.speed) else null,
        if (proto.hasMp()) proto.mp else null,
        proto.flags,
    )

    fun toProto(): LocationData = locationData {
        latLong = this@LocationDataPojo.latLong.toProto()
        this@LocationDataPojo.speed?.let { this.speed = it.toProto() }
        this@LocationDataPojo.mp?.let { mp = it }
        flags = this@LocationDataPojo.flags
    }

    fun saveInto(writer: SerialWriter) {
        latLong.saveInto(writer)
        this@LocationDataPojo.speed?.saveInto(writer)
        mp?.let { writer.addFloat("mp", it) }
        writer.addInt("flags", flags)
    }

    constructor(reader: SerialReader) : this(
        LatLongDataPojo(reader),
        if (reader.hasName("speed")) SpeedDataPojo(reader) else null,
        if (reader.hasName("mp")) reader.getFloat("mp") else null,
        reader.getInt("flags"),
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LatLongDataPojo @JsonCreator constructor(
    @param:JsonProperty("timestampMillis")  val timestampMillis: Long? = null,
    @param:JsonProperty("latitude")         val latitude: Double? = null,
    @param:JsonProperty("longitude")        val longitude: Double? = null,
    @param:JsonProperty("accuracy")         val accuracy: Float? = null,
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
@JsonIgnoreProperties(value = ["bearingDeg", "bearingAcc"])
data class SpeedDataPojo @JsonCreator constructor(
    @param:JsonProperty("speedMps")     val speedMps: Float? = null,
    @param:JsonProperty("speedAcc")     val speedAcc: Float? = null,
) {
    constructor(proto: SpeedData) : this(
        proto.speedMps,
        if (proto.hasSpeedAcc()) proto.speedAcc else null,
    )

    fun toProto(): SpeedData = speedData {
        this@SpeedDataPojo.speedMps?.let { speedMps = it }
        this@SpeedDataPojo.speedAcc?.let { speedAcc = it }
    }

    fun saveInto(writer: SerialWriter) {
        speedMps?.let { writer.addFloat("speedMps", it) }
        speedAcc?.let { writer.addFloat("speedAcc", it) }
    }

    constructor(reader: SerialReader) : this(
        if (reader.hasName("speedMps")) reader.getFloat("speedMps") else null,
        if (reader.hasName("speedAcc")) reader.getFloat("speedAcc") else null,
    )
}
