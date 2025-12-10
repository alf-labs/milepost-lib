package com.alflabs.mpgpstrack.dagger

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.alflabs.mpgpstrack.main.AppSettingsSerializer
import com.alflabs.mpgpstrack.settings.AppSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    private const val APP_SETTINGS_FILE_NAME = "app_settings.pb"

    // Note: this creates the DataStore wrapper around the pb file.
    // All accesses should be done via this singleton instance to ensure file consistency.
    // (the weird Context. syntax is because this is really a Kotlin extension function
    // that takes a Context as argument).
    private val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
        fileName = APP_SETTINGS_FILE_NAME,
        serializer = AppSettingsSerializer
    )

    @Provides
    @Singleton
    fun provideAppSettingsDataStore(@ApplicationContext context: Context): DataStore<AppSettings> {
        return context.appSettingsDataStore
    }
}
