package com.alflabs.milepostlib.db

import androidx.room.TypeConverter
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.trigger.PlaybackTrigger
import com.google.protobuf.AbstractMessageLite
import com.google.protobuf.GeneratedMessageLite
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Parser
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Converters {
    @TypeConverter
    fun fromLocationDataList(value: LocationDataList?): String? = fromProto(value)

    @TypeConverter
    fun toLocationDataList(value: String?): LocationDataList? = toProto<LocationDataList>(value)

    @TypeConverter
    fun fromGpsTrackSummary(value: GpsTrackSummary?): String? = fromProto(value)

    @TypeConverter
    fun toGpsTrackSummary(value: String?): GpsTrackSummary?  = toProto<GpsTrackSummary>(value)

    @TypeConverter
    fun fromAudioSegmentMetadata(value: AudioSegmentMetadata?): String? = fromProto(value)

    @TypeConverter
    fun toAudioSegmentMetadata(value: String?): AudioSegmentMetadata?  = toProto<AudioSegmentMetadata>(value)

    @TypeConverter
    fun fromPlaybackTrigger(value: PlaybackTrigger?): String? = fromProto(value)

    @TypeConverter
    fun toPlaybackTrigger(value: String?): PlaybackTrigger?  = toProto<PlaybackTrigger>(value)

    companion object {
        /** Serializes a generic Protobuf lite to a Base64 string. */
        private inline fun
                <reified T : AbstractMessageLite<T, *>>
                fromProto(value: T?): String? {
            return value?.let {
                Base64.encode(it.toByteArray())
            }
        }

        /**
         * Deserializes a Base64 string into a generic Protobuf lite.
         *
         * This relies on the fact that each generated [GeneratedMessageLite] contains a Parser()
         * method returning the deserializer for that protobuf message.
         *
         * When used a Room Column, the proto should always be used as an optional with a null
         * default value as we cannot guarantee that the base64 encoded string in the database
         * actually represents a valid proto. However, we do not hide proto parser exceptions here.
         * A failure to parse a proto will be reflected in the Room DAO implementation.
         *
         * The Base64 decoder throws IllegalArgumentException on failure.
         * The parser throws InvalidProtocolBufferException on failure.
         * A null input string returns a null output.
         * An empty non-null string should parse to an empty (all defaults) proto.
         */
        @Throws(
            IllegalArgumentException::class,
            InvalidProtocolBufferException::class)
        private inline fun
                <reified T : GeneratedMessageLite<T, *>>
                toProto(value: String?): T? {
            return value?.let {
                val decoded = Base64.decode(it)
                // Reified type allows accessing T's static methods like GeneratedMessageLite::parser()
                @Suppress("UNCHECKED_CAST")
                val parser = T::class.java.getMethod("parser").invoke(null) as Parser<T>
                parser.parseFrom(decoded)
            }
        }
    }
}
