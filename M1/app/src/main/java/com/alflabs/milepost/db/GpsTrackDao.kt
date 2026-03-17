package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.gpsTracks.GpsTrackDisplayItem
import com.alflabs.milepost.location.LocationDataList
import kotlinx.coroutines.flow.Flow

/** All queries for the gps_tracks table */
@Dao
interface GpsTrackDao {
    /** Insert a GpsTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    @Insert
    suspend fun insert(track: GpsTrackEntity): Long

    /** Update a GpsTrackEntity with a matching id. */
    @Update
    suspend fun update(track: GpsTrackEntity)

    @Query("SELECT id, createdDateISO, trackLabel, summary FROM gps_tracks ORDER BY id DESC")
    fun getAllDisplayItems(): Flow<List<GpsTrackDisplayItem>>

    @Query("SELECT * FROM gps_tracks WHERE id = :id")
    fun getById(id: Long): Flow<GpsTrackEntity?>

    @Query("SELECT * FROM gps_tracks WHERE id = :id")
    suspend fun getByIdAsync(id: Long): GpsTrackEntity?

    @Query("SELECT uuid FROM gps_tracks WHERE id = :id")
    suspend fun getUuid(id: Long): String?

    @Query("SELECT id FROM gps_tracks WHERE uuid = :uuid")
    suspend fun getIdByUuid(uuid: String): Long?

    @Query("SELECT * FROM gps_tracks")
    suspend fun getItemsAsync(): List<GpsTrackEntity>

    @Query("""
        SELECT G.* FROM gps_tracks AS G
        INNER JOIN docent_tracks AS D ON G.id = D.gpsTrackId
        WHERE D.id = :docentId
        """)
    fun getGpsTrackForDocentTrackId(docentId: Long): Flow<GpsTrackEntity?>

    @Query("UPDATE gps_tracks SET locationList = :locationList WHERE id = :id")
    suspend fun updateLocationList(id: Long, locationList: LocationDataList)

    @Query("UPDATE gps_tracks SET summary = :summary WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: GpsTrackSummary)

    @Query("DELETE FROM gps_tracks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
