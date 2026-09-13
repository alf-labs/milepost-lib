package com.alflabs.milepostlib.db

import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.locationDataList
import com.alflabs.milepostlib.location.speedData
import com.alflabs.milepostlib.utils.diffIsEqualTo
import com.alflabs.milepostlib.utils.toTestString
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
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
        assertThat(exported).diffIsEqualTo(expected)

        val importMapper = ObjectMapper().importProtobuf()
        val imported = importMapper.readValue(exported, LocationDataList::class.java)
        assertThat(imported.toTestString()).diffIsEqualTo(originalList.toTestString())
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
        assertThat(exported).diffIsEqualTo(expected)

        val importMapper = ObjectMapper().importProtobuf()
        val imported = importMapper.readValue(exported, GpsTrackEntity::class.java)
        assertThat(imported.toTestString()).diffIsEqualTo(gpsTrackEntity.toTestString())
    }
}
