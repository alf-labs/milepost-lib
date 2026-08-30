package com.alflabs.milepostlib.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GpsTrackEntity::class, AudioSegmentEntity::class, PlayTriggerEntity::class, DocentTrackEntity::class],
    version = 1,
    autoMigrations = [
    ]
)
@TypeConverters(Converters::class)
abstract class LibDatabase : RoomDatabase() {
    abstract fun gpsTrackDao(): GpsTrackDao

    abstract fun audioSegmentDao(): AudioSegmentDao

    abstract fun playTriggerDao(): PlayTriggerDao

    abstract fun docentTrackDao(): DocentTrackDao
}
