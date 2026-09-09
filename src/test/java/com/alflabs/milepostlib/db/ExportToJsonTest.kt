package com.alflabs.milepostlib.db

import com.alflabs.milepostlib.location.LatLongData
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.location.SpeedData
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.locationDataList
import com.alflabs.milepostlib.location.speedData
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.MessageLite
import org.junit.Test
import java.io.StringWriter


class ExportToJsonTest {

    @Test
    fun testProtobufToJson() {
        val originalList = locationDataList {
            locations.addAll(listOf(
                locationData { latLong = latLongData { latitude = 1.0; longitude = 1.0 } },
                locationData { latLong = latLongData { latitude = 2.0; longitude = 2.0 } },
                locationData { latLong = latLongData { latitude = 3.0; longitude = 3.0 };
                    speed = speedData { speedMps = 1.0f } }
            ))
        }

        val exportMapper = ObjectMapper().exportProtobuf()

        val writer = StringWriter()
        exportMapper.writerWithDefaultPrettyPrinter().writeValue(writer, originalList)

        val exported = writer.toTestString()
        val expected = """
            {
              "locationsList" : [ {
                "flags" : 0,
                "latLong" : {
                  "latitude" : 1.0,
                  "longitude" : 1.0,
                  "timestampMillis" : 0
                },
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                }
              }, {
                "flags" : 0,
                "latLong" : {
                  "latitude" : 2.0,
                  "longitude" : 2.0,
                  "timestampMillis" : 0
                },
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                }
              }, {
                "flags" : 0,
                "latLong" : {
                  "latitude" : 3.0,
                  "longitude" : 3.0,
                  "timestampMillis" : 0
                },
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                },
                "speed" : {
                  "speedMps" : 1.0
                }
              } ]
            }
            """.trimIndent().toTestString()
        assertThat(exported).isEqualTo(expected)

        val importMapper = ObjectMapper().importProtobuf()
        val imported = importMapper.readValue(exported, LocationDataList::class.java)
        assertThat(imported.toTestString()).isEqualTo(originalList.toTestString())
    }

    @Test
    fun testRoomEntityToJson() {
        val locationList = locationDataList {
            locations.addAll(listOf(
                locationData { latLong = latLongData { latitude = 1.0; longitude = 1.0 } },
                locationData { latLong = latLongData { latitude = 2.0; longitude = 2.0 } },
                locationData { latLong = latLongData { latitude = 3.0; longitude = 3.0 };
                    speed = speedData { speedMps = 1.0f } }
            ))
        }
        val gpsTrackEntity = GpsTrackEntity(
            id = 123,
            uuid = "test-uuid-1234",
            trackLabel = "Track name",
            startLabel = "Start Here",
            endLabel = "End There",
            locationList = locationList,
            // This summary should not be exported
            summary = gpsTrackSummary {
                mpMin = 10f
                mpMax = 42f
                lengthM = 2400f
                durationS = 1800f
            },
            createdDateISO = "2021-02-03T04:05:06Z",
            updatedDateISO = "2022-03-04T05:06:07Z"
        )

        val exportMapper = ObjectMapper().exportProtobuf()

        val writer = StringWriter()
        exportMapper.writerWithDefaultPrettyPrinter().writeValue(writer, gpsTrackEntity)

        val exported = writer.toTestString()
        val expected = """
            {
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "endLabel" : "End There",
              "id" : 123,
              "locationList" : {
                "locationsList" : [ {
                  "flags" : 0,
                  "latLong" : {
                    "latitude" : 1.0,
                    "longitude" : 1.0,
                    "timestampMillis" : 0
                  },
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  }
                }, {
                  "flags" : 0,
                  "latLong" : {
                    "latitude" : 2.0,
                    "longitude" : 2.0,
                    "timestampMillis" : 0
                  },
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  }
                }, {
                  "flags" : 0,
                  "latLong" : {
                    "latitude" : 3.0,
                    "longitude" : 3.0,
                    "timestampMillis" : 0
                  },
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  },
                  "speed" : {
                    "speedMps" : 1.0
                  }
                } ]
              },
              "startLabel" : "Start Here",
              "summary" : {
                "durationS" : 1800.0,
                "lengthM" : 2400.0,
                "mpMax" : 42.0,
                "mpMin" : 10.0
              },
              "trackLabel" : "Track name",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "uuid" : "test-uuid-1234"
            }
            """.trimIndent().toTestString()
        assertThat(exported).isEqualTo(expected)

        val importMapper = ObjectMapper().importProtobuf()
        val imported = importMapper.readValue(exported, GpsTrackEntity::class.java)
        assertThat(imported.toTestString()).isEqualTo(gpsTrackEntity.toTestString())
    }
}

private fun Any.toTestString() =
    this.toString().replace("\r\n", "\n")

fun ObjectMapper.exportProtobuf() : ObjectMapper {
    setSerializationInclusion(JsonInclude.Include.NON_NULL)

    // For unit tests, we need to enforce sorting of keys and properties.
    // Sort standard Java POJO properties alphabetically
    configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    // Sort java.util.Map entries by their keys
    configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    // Sort JsonNode / ObjectNode properties
    configure(JsonNodeFeature.WRITE_PROPERTIES_SORTED, true)

    // Tells Jackson to ignore defaultInstanceForType on all Protobuf-Lite objects
    addMixIn(MessageLite::class.java, ProtobufLiteMixin::class.java)

    val protobufHasFilter = object : SimpleBeanPropertyFilter() {
        override fun serializeAsField(
            pojo: Any,
            jgen: JsonGenerator,
            provider: SerializerProvider,
            writer: PropertyWriter
        ) {
            val propName = writer.name // e.g., "speed"
            val hasMethodName = "has" + propName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            try {
                val hasMethod = pojo.javaClass.getMethod(hasMethodName)
                val isPresent = hasMethod.invoke(pojo) as? Boolean
                if (isPresent == false) {
                    return // Omit field from JSON if hasX() returns false
                }
            } catch (_: NoSuchMethodException) {
                // Field has no hasX() method (e.g. primitives/collections), proceed normally
            }

            super.serializeAsField(pojo, jgen, provider, writer)
        }
    }
    setFilterProvider(
        SimpleFilterProvider().addFilter("protobufHasFilter", protobufHasFilter)
    )

    // Ignore generated *OrBuilderList getters to prevent duplicate JSON keys
    setAnnotationIntrospector( object : JacksonAnnotationIntrospector() {
        override fun findFilterId(a: Annotated): Any? {
            // Attach the filter to all Protobuf-Lite classes
            if (MessageLite::class.java.isAssignableFrom(a.rawType)) {
                return "protobufHasFilter"
            }
            return super.findFilterId(a)
        }

        override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
            // We avoid *OrBuilder() and *OrBuilderList() getters.
            // We also remove the "listCount" getters.
            if (m.name.contains("OrBuilder") or m.name.endsWith("Count")) {
                return true
            }
            return super.hasIgnoreMarker(m)
        }
    } )

    return this
}

fun ObjectMapper.importProtobuf() : ObjectMapper {
    val protoDeserializerModule = SimpleModule().apply {
        addDeserializer(LocationDataList::class.java, LocationDataListDeserializer())
        addDeserializer(LocationData::class.java, LocationDataDeserializer())
        addDeserializer(LatLongData::class.java, LatLongDataDeserializer())
        addDeserializer(SpeedData::class.java, SpeedDataDeserializer())
        addDeserializer(GpsTrackEntity::class.java, GpsTrackEntityDeserializer())
        addDeserializer(GpsTrackSummary::class.java, GpsTrackSummaryDeserializer())
    }
    registerModule(protoDeserializerModule)

    return this
}

// Mixin definition that allows Jackson to ignore Protobuf-Lite getters during export.
abstract class ProtobufLiteMixin {
    @get:JsonIgnore
    abstract val defaultInstanceForType: MessageLite

    @get:JsonIgnore
    abstract val initializationErrorString: String

    @get:JsonIgnore
    abstract val isInitialized: Boolean

    @get:JsonIgnore
    abstract val parserForType: Any

    @get:JsonIgnore
    abstract val serializedSize: Int
}

class LocationDataListDeserializer : StdDeserializer<LocationDataList>(LocationDataList::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocationDataList {
        val node: JsonNode = p.codec.readTree(p)
        return locationDataList {
            node.get("locationsList")?.forEach { elem ->
                locations.add(ctxt.readTreeAsValue(elem, LocationData::class.java))
            }
        }
    }
}

class LocationDataDeserializer : StdDeserializer<LocationData>(LocationData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocationData {
        val node: JsonNode = p.codec.readTree(p)
        return locationData {
            if (node.hasNonNull("latLong")) {
                latLong = ctxt.readTreeAsValue(node.get("latLong"), LatLongData::class.java)
            }
            if (node.hasNonNull("speed")) {
                speed = ctxt.readTreeAsValue(node.get("speed"), SpeedData::class.java)
            }
            if (node.hasNonNull("flags")) flags = node.get("flags").intValue()
            if (node.hasNonNull("mp")) {
                val value = node.get("mp").floatValue()
                if (value != 0f) mp = value
            }
            if (node.hasNonNull("poiLabel")) {
                val value = node.get("poiLabel").asText()
                if (value.isNotEmpty()) poiLabel = value
            }
        }
    }
}

class LatLongDataDeserializer : StdDeserializer<LatLongData>(LatLongData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LatLongData {
        val node: JsonNode = p.codec.readTree(p)
        return latLongData {
            if (node.hasNonNull("latitude")) latitude = node.get("latitude").asDouble()
            if (node.hasNonNull("longitude")) longitude = node.get("longitude").asDouble()
            if (node.hasNonNull("timestampMillis")) timestampMillis = node.get("timestampMillis").asLong()
            if (node.hasNonNull("accuracy")) {
                val value = node.get("accuracy").floatValue()
                if (value != 0f) accuracy = value
            }
        }
    }
}

class SpeedDataDeserializer : StdDeserializer<SpeedData>(SpeedData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SpeedData {
        val node: JsonNode = p.codec.readTree(p)
        return speedData {
            if (node.hasNonNull("speedMps")) speedMps = node.get("speedMps").floatValue()
            if (node.hasNonNull("speedAcc")) {
                val value = node.get("speedAcc").floatValue()
                if (value != 0f) speedAcc = value
            }
        }
    }
}

class GpsTrackSummaryDeserializer : StdDeserializer<GpsTrackSummary>(GpsTrackSummary::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GpsTrackSummary {
        val node: JsonNode = p.codec.readTree(p)
        return gpsTrackSummary {
            if (node.hasNonNull("mpMin")) {
                val value = node.get("mpMin").floatValue()
                if (value != 0f) mpMin = value
            }
            if (node.hasNonNull("mpMax")) {
                val value = node.get("mpMax").floatValue()
                if (value != 0f) mpMax = value
            }
            if (node.hasNonNull("lengthM")) {
                val value = node.get("lengthM").floatValue()
                if (value != 0f) lengthM = value
            }
            if (node.hasNonNull("durationS")) {
                val value = node.get("durationS").floatValue()
                if (value != 0f) durationS = value
            }
        }
    }
}

class GpsTrackEntityDeserializer : StdDeserializer<GpsTrackEntity>(GpsTrackEntity::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GpsTrackEntity {
        val node: JsonNode = p.codec.readTree(p)
        return GpsTrackEntity(
            id = if (node.hasNonNull("id")) node.get("id").asLong() else 0L,
            uuid = if (node.hasNonNull("uuid")) node.get("uuid").asText() else "",
            createdDateISO = if (node.hasNonNull("createdDateISO")) node.get("createdDateISO").asText() else "",
            updatedDateISO = if (node.hasNonNull("updatedDateISO")) node.get("updatedDateISO").asText() else null,
            trackLabel = if (node.hasNonNull("trackLabel")) node.get("trackLabel").asText() else null,
            startLabel = if (node.hasNonNull("startLabel")) node.get("startLabel").asText() else null,
            endLabel = if (node.hasNonNull("endLabel")) node.get("endLabel").asText() else null,
            summary = if (node.hasNonNull("summary")) ctxt.readTreeAsValue(node.get("summary"), GpsTrackSummary::class.java) else null,
            locationList = if (node.hasNonNull("locationList")) ctxt.readTreeAsValue(node.get("locationList"), LocationDataList::class.java) else null
        )
    }
}
