package com.alflabs.milepost.tracks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocentTrackDao {
    @Insert
    suspend fun insert(docentTrack: DocentTrackEntity)

    @Update
    suspend fun update(docentTrack: DocentTrackEntity)

    @Query("SELECT * FROM docent_tracks WHERE id = :id")
    fun getById(id: Long): Flow<DocentTrackEntity?>

    @Query("SELECT id FROM docent_tracks WHERE uuid = :uuid")
    suspend fun getIdByUuid(uuid: String): Long?

    @Query("SELECT * FROM docent_tracks ORDER BY last_used_time DESC")
    suspend fun getAll(): List<DocentTrackEntity>

    @Query("DELETE FROM docent_tracks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
