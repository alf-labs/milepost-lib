package com.alflabs.milepost.trigger

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackTriggerDirectionPojoTest {

    @Test
    fun playbackTriggerDirectionPojo_protoConversion() {
        // Test all directions
        val directions = listOf(
            PlaybackTriggerDirection.DIRECTION_UNSPECIFIED to "Both",
            PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP to "ToLowMP",
            PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP to "ToHighMP"
        )

        for ((proto, string) in directions) {
            val pojoFromProto = PlaybackTriggerDirectionPojo(proto)
            assertThat(pojoFromProto.direction).isEqualTo(string)
            assertThat(pojoFromProto.toProto()).isEqualTo(proto)

            val pojoFromString = PlaybackTriggerDirectionPojo(string)
            assertThat(pojoFromString.toProto()).isEqualTo(proto)
        }

        // Test unrecognized
        val unknownPojo = PlaybackTriggerDirectionPojo("Unknown")
        assertThat(unknownPojo.toProto()).isEqualTo(PlaybackTriggerDirection.UNRECOGNIZED)

        val unrecognizedPojo = PlaybackTriggerDirectionPojo(PlaybackTriggerDirection.UNRECOGNIZED)
        assertThat(unrecognizedPojo.direction).isEqualTo("Unknown")
    }
}
