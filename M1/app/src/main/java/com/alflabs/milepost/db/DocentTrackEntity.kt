package com.alflabs.milepost.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

@Entity(
    tableName = "docent_tracks",
    foreignKeys = [
        ForeignKey(
            entity = GpsTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["gpsTrackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocentTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo
    val userTitle: String? = null,

    @ColumnInfo
    val creationTime: String,

    @ColumnInfo
    val lastUsedTime: String? = null,

    @ColumnInfo(index = true)
    val gpsTrackId: Long
) {
    /** A version of [userTitle] for display that is never null or empty. */
    fun displayUserTitle(): String {
        if (userTitle.isNullOrBlank()) {
            return "Docent Track #$id"
        }
        return userTitle
    }
}
