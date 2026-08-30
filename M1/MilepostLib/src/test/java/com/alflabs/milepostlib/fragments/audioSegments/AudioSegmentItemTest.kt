package com.alflabs.milepostlib.fragments.audioSegments

import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test


class AudioSegmentItemTest {
    @get:Rule
    val expect: Expect = Expect.create()

    private fun mkItem(descriptor: String) = AudioSegmentItem(
        id = 1L,
        descriptor = descriptor,
        lastAccessTime = "2034-56-78",
        persistableUri = "uri",
        metadata = null
    )

    @Test
    fun displayTitle() {
        expect.that(mkItem("filename.ext[0;136000]").displayTitle()).isEqualTo("filename (2m 16s)")
        expect.that(mkItem("filename_no_ext[0;136000]").displayTitle()).isEqualTo("filename_no_ext (2m 16s)")
        expect.that(mkItem("filename_no_timestamp.wav").displayTitle()).isEqualTo("filename_no_timestamp")
        expect.that(mkItem("filename_plain").displayTitle()).isEqualTo("filename_plain")
    }

    @Test
    fun toDisplayTitle() {
        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext[68000;136000]")).isEqualTo("filename (1m 08s - 2m 16s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext[68000]")).isEqualTo("filename (1m 08s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext[68000;0]")).isEqualTo("filename (1m 08s - 0m 00s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext[;136000]")).isEqualTo("filename (2m 16s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext[0;136000]")).isEqualTo("filename (2m 16s)")

        expect.that(AudioSegmentItem.toDisplayTitle("filename[0;136000]")).isEqualTo("filename (2m 16s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename[68000]")).isEqualTo("filename (1m 08s)")
        expect.that(AudioSegmentItem.toDisplayTitle("filename[;136000]")).isEqualTo("filename (2m 16s)")

        expect.that(AudioSegmentItem.toDisplayTitle("filename.ext")).isEqualTo("filename")
        expect.that(AudioSegmentItem.toDisplayTitle("filename")).isEqualTo("filename")
    }
}
