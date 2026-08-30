package com.alflabs.milepostlib.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

    companion object {
        /**
         * Special downgrade from test-#10 to final-#1.
         * TBD: Eventually remove this once all test clients have been upgraded
         * and definitely remove this if this db reaches version 10!
         * Used in [com.alflabs.milepostlib.dagger.DatabaseModule].
         */
        val TEMP_MIGRATION_10_1 = object : Migration(10, 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Truly the same schema, so no-op.
            }
        }
    }
}
