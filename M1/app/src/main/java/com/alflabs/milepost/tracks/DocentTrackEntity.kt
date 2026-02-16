package com.alflabs.milepost.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "docent_tracks",
    foreignKeys = [
        ForeignKey(
            entity = GpsTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["gps_track_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["uuid"], unique = true)]
)
data class DocentTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_title")
    val userTitle: String? = null,

    @ColumnInfo(name = "creation_time")
    val creationTime: String,

    @ColumnInfo(name = "last_used_time")
    val lastUsedTime: String,

    @ColumnInfo(name = "gps_track_id", index = true)
    val gpsTrackId: Long?
)
