package com.alflabs.milepostlib.db

import com.alflabs.milepostlib.location.LatLongData
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.location.SpeedData
import com.alflabs.milepostlib.trigger.PlaybackTriggerMetadata
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.PropertyWriter
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.google.protobuf.MessageLite

import com.fasterxml.jackson.module.kotlin.registerKotlinModule

fun ObjectMapper.exportProtobuf() : ObjectMapper {
    registerKotlinModule()
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
            // We also remove the "stringBytes" getters.
            // We also remove the "enumValue" getters.
            if (m.name.contains("OrBuilder")
                or m.name.endsWith("Count")
                or m.name.endsWith("Bytes")
                or m.name.endsWith("Value")) {
                return true
            }
            return super.hasIgnoreMarker(m)
        }
    } )

    return this
}

fun ObjectMapper.importProtobuf() : ObjectMapper {
    registerKotlinModule()
    val protoDeserializerModule = SimpleModule().apply {
        addDeserializer(LocationDataList::class.java, LocationDataListDeserializer())
        addDeserializer(LocationData::class.java, LocationDataDeserializer())
        addDeserializer(SpeedData::class.java, SpeedDataDeserializer())
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
