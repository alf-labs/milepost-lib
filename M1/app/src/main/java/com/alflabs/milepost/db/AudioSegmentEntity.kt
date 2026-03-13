package com.alflabs.milepost.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_segments")
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // TBD: consider adding a "reference" or "descriptor" column that is "reasonably unique",
    // consisting of the filename combined with a time range selection in the audio file.
    // That would then be used to differentiate segments using the same file, but with different
    // timings. Such an identifier would be mostly useful for import/export and is not the canonical
    // "unique key" for a segment entity.

    // Note: we want this field here (and not in metadata) so that we can sort the queries directly.
    @ColumnInfo
    val fileName: String,

    @ColumnInfo
    val lastAccessTime: String,

    @ColumnInfo
    val persistableUri: String,

    @ColumnInfo
    val metadata: AudioSegmentMetadata? = null,
)
