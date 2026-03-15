package com.alflabs.milepost.fragments.audioSegments

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioSegmentItemTest {

    private fun mkItem(descriptor: String) = AudioSegmentItem(
        id = 1L,
        descriptor = descriptor,
        lastAccessTime = "2034-56-78",
        persistableUri = "uri",
        metadata = null
    )

    @Test
    fun displayTitle() {
        assertThat(mkItem("filename.ext[0;1234]").displayTitle()).isEqualTo("filename")
        assertThat(mkItem("filename_no_ext[0;1234]").displayTitle()).isEqualTo("filename_no_ext")
        assertThat(mkItem("filename_no_timestamp.wav").displayTitle()).isEqualTo("filename_no_timestamp")
        assertThat(mkItem("filename_plain").displayTitle()).isEqualTo("filename_plain")
    }
}
