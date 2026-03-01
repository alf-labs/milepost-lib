package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.docentTracks.DocentTrackItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DocentTrackDao {
    /** Insert a DocentTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    @Insert
    suspend fun insert(docentTrack: DocentTrackEntity): Long

    @Update
    suspend fun update(docentTrack: DocentTrackEntity)

    @Query("SELECT * FROM docent_tracks WHERE id = :id")
    fun getById(id: Long): Flow<DocentTrackEntity?>

    @Query("""
        SELECT G.* FROM gps_tracks AS G
        INNER JOIN docent_tracks AS D ON G.id = D.gpsTrackId
        WHERE D.id = :docentId
        """)
    fun getGpsTrackForDocentTrackId(docentId: Long): Flow<GpsTrackEntity?>

    @Query("SELECT id FROM docent_tracks WHERE uuid = :uuid")
    suspend fun getIdByUuid(uuid: String): Long?

    @Query("""
        SELECT
            D.id, D.userTitle, D.creationTime, D.gpsTrackId, G.trackLabel AS gpsTrackLabel
        FROM docent_tracks AS D
        INNER JOIN gps_tracks AS G ON D.gpsTrackId = G.id
        ORDER BY D.creationTime DESC
        """)
    fun getItems(): Flow<List<DocentTrackItem>>

    @Query("DELETE FROM docent_tracks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
