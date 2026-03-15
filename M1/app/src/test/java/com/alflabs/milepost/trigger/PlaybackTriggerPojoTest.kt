package com.alflabs.milepost.trigger

import com.alflabs.milepost.fragments.docentTriggers.DocentTriggerItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackTriggerPojoTest {

    @Test
    fun playbackTriggerPojo_fromDocentEditorItem() {
        val metadata = playbackTriggerMetadata {
            includeInColdStart = true
            mpMin = 10.5f
            mpMax = 20.0f
            mpDirection = PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
        }

        val item = DocentTriggerItem(
            id = 123L,
            audioSegmentId = 456L,
            persistableUri = "Uri-456",
            docentTrackId = 789L,
            playbackTrigger = metadata,
            descriptor = "test_audio.mp3[0:42]",
        )

        val pojo = PlaybackTriggerPojo(item)

        assertThat(pojo.audioDescriptor).isEqualTo("test_audio.mp3[0:42]")
        assertThat(pojo.includeInColdStart).isTrue()
        assertThat(pojo.mpMin).isEqualTo(10.5f)
        assertThat(pojo.mpMax).isEqualTo(20.0f)
        assertThat(pojo.mpDirection.direction).isEqualTo("ToHighMP")
    }

    @Test
    fun playbackTriggerPojo_toItem() {
        val pojo = PlaybackTriggerPojo(
            audioDescriptor = "test_audio.mp3[0:42]",
            includeInColdStart = false,
            mpMin = 5.0f,
            mpMax = 15.0f,
            mpDirection = PlaybackTriggerDirectionPojo("ToLowMP")
        )

        val descriptorsToId = mapOf("test_audio.mp3[0:42]" to 456L)
        val descriptorsToUri = mapOf("test_audio.mp3[0:42]" to "Uri-456")
        val docentTrackId = 789L

        val item = pojo.toItem(docentTrackId, descriptorsToId, descriptorsToUri)

        assertThat(item.id).isEqualTo(0L)
        assertThat(item.descriptor).isEqualTo("test_audio.mp3")
        assertThat(item.audioSegmentId).isEqualTo(456L)
        assertThat(item.docentTrackId).isEqualTo(docentTrackId)

        val metadata = item.playbackTrigger!!
        assertThat(metadata.includeInColdStart).isFalse()
        assertThat(metadata.mpMin).isEqualTo(5.0f)
        assertThat(metadata.mpMax).isEqualTo(15.0f)
        assertThat(metadata.mpDirection).isEqualTo(PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP)
    }
}
