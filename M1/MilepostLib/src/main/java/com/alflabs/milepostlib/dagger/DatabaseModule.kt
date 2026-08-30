package com.alflabs.milepostlib.dagger

import android.content.Context
import androidx.room.Room
import com.alflabs.milepostlib.db.LibDatabase
import com.alflabs.milepostlib.db.AudioSegmentDao
import com.alflabs.milepostlib.db.DocentTrackDao
import com.alflabs.milepostlib.db.GpsTrackDao
import com.alflabs.milepostlib.db.PlayTriggerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideLibDatabase(@ApplicationContext appContext: Context): LibDatabase {
        return Room.databaseBuilder(
            appContext,
            LibDatabase::class.java,
            "tracks"
        ).build()
    }

    @Provides
    fun provideGpsTrackDao(libDatabase: LibDatabase): GpsTrackDao {
        return libDatabase.gpsTrackDao()
    }

    @Provides
    fun provideAudioSegmentDao(libDatabase: LibDatabase): AudioSegmentDao {
        return libDatabase.audioSegmentDao()
    }

    @Provides
    fun provideDocentTrackDao(libDatabase: LibDatabase): DocentTrackDao {
        return libDatabase.docentTrackDao()
    }

    @Provides
    fun providePlayTriggerDao(libDatabase: LibDatabase): PlayTriggerDao {
        return libDatabase.playTriggerDao()
    }
}
