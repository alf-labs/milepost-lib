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

    suspend fun toPojo(gpsTrackDao: GpsTrackDao) = DocentTrackEntityPojo(
        uuid = uuid,
        userTitle = userTitle,
        createdISO = creationTime,
        lastUsedISO = lastUsedTime,
        gpsTrackUuid = gpsTrackDao.getUuid(gpsTrackId)!!
    )
}

/** A mirror of [DocentTrackEntity] for JSON import/export. */
data class DocentTrackEntityPojo @JsonCreator constructor(
    @param:JsonProperty("uuid") val uuid: String,
    @param:JsonProperty("userTitle") val userTitle: String? = null,
    @param:JsonProperty("createdISO") val createdISO: String,
    @param:JsonProperty("lastUsedISO") val lastUsedISO: String? = null,
    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String
) {
    suspend fun toProto(gpsTrackDao: GpsTrackDao) = DocentTrackEntity(
        id = 0,
        uuid = uuid,
        userTitle = userTitle,
        creationTime = createdISO,
        lastUsedTime = lastUsedISO,
        gpsTrackId = gpsTrackDao.getIdByUuid(gpsTrackUuid)!!
    )

}
