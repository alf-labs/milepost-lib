package com.alflabs.milepostlib.db

import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.locationDataList
import com.alflabs.milepostlib.location.speedData
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
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

        val objectMapper = ObjectMapper().configureForProtobuf()

        val writer = StringWriter()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, originalList)

        val exported = writer.toString().replace("\r\n", "\n")
        val expected = """
            {
              "locationsCount" : 3,
              "locationsList" : [ {
                "flags" : 0,
                "latLong" : {
                  "accuracy" : 0.0,
                  "latitude" : 1.0,
                  "longitude" : 1.0,
                  "timestampMillis" : 0
                },
                "mp" : 0.0,
                "poiLabel" : "",
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                },
                "speed" : {
                  "speedAcc" : 0.0,
                  "speedMps" : 0.0
                }
              }, {
                "flags" : 0,
                "latLong" : {
                  "accuracy" : 0.0,
                  "latitude" : 2.0,
                  "longitude" : 2.0,
                  "timestampMillis" : 0
                },
                "mp" : 0.0,
                "poiLabel" : "",
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                },
                "speed" : {
                  "speedAcc" : 0.0,
                  "speedMps" : 0.0
                }
              }, {
                "flags" : 0,
                "latLong" : {
                  "accuracy" : 0.0,
                  "latitude" : 3.0,
                  "longitude" : 3.0,
                  "timestampMillis" : 0
                },
                "mp" : 0.0,
                "poiLabel" : "",
                "poiLabelBytes" : {
                  "empty" : true,
                  "validUtf8" : true
                },
                "speed" : {
                  "speedAcc" : 0.0,
                  "speedMps" : 1.0
                }
              } ]
            }
            """.trimIndent().replace("\r\n", "\n")
        assertThat(exported).isEqualTo(expected)

//        val imported = objectMapper.readValue(exported, LocationDataList::class.java)
//        assertThat(imported).isEqualTo(originalList)
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

        val objectMapper = ObjectMapper().configureForProtobuf()

        val writer = StringWriter()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, gpsTrackEntity)

        val exported = writer.toString().replace("\r\n", "\n")
        val expected = """
            {
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "endLabel" : "End There",
              "id" : 123,
              "locationList" : {
                "locationsCount" : 3,
                "locationsList" : [ {
                  "flags" : 0,
                  "latLong" : {
                    "accuracy" : 0.0,
                    "latitude" : 1.0,
                    "longitude" : 1.0,
                    "timestampMillis" : 0
                  },
                  "mp" : 0.0,
                  "poiLabel" : "",
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  },
                  "speed" : {
                    "speedAcc" : 0.0,
                    "speedMps" : 0.0
                  }
                }, {
                  "flags" : 0,
                  "latLong" : {
                    "accuracy" : 0.0,
                    "latitude" : 2.0,
                    "longitude" : 2.0,
                    "timestampMillis" : 0
                  },
                  "mp" : 0.0,
                  "poiLabel" : "",
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  },
                  "speed" : {
                    "speedAcc" : 0.0,
                    "speedMps" : 0.0
                  }
                }, {
                  "flags" : 0,
                  "latLong" : {
                    "accuracy" : 0.0,
                    "latitude" : 3.0,
                    "longitude" : 3.0,
                    "timestampMillis" : 0
                  },
                  "mp" : 0.0,
                  "poiLabel" : "",
                  "poiLabelBytes" : {
                    "empty" : true,
                    "validUtf8" : true
                  },
                  "speed" : {
                    "speedAcc" : 0.0,
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
            """.trimIndent().replace("\r\n", "\n")
        assertThat(exported).isEqualTo(expected)

//        val imported = objectMapper.readValue(exported, GpsTrackEntity::class.java)
//        assertThat(imported).isEqualTo(gpsTrackEntity)
    }

}

fun ObjectMapper.configureForProtobuf() : ObjectMapper {
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

    // Ignore generated *OrBuilderList getters to prevent duplicate JSON keys
    setAnnotationIntrospector( object : JacksonAnnotationIntrospector() {
        override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
            // We avoid *OrBuilder() and *OrBuilderList() getters.
            if (m.name.contains("OrBuilder")) {
                return true
            }
            return super.hasIgnoreMarker(m)
        }
    } )

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

