package com.alflabs.milepost.dagger

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {
    private const val APP_SETTINGS_FILE_NAME = "app_settings.xml"

    // Note: this creates the DataStore wrapper around the pb file.
    // All accesses should be done via this singleton instance to ensure file consistency.
    // (the weird Context. syntax is because this is really a Kotlin extension function
    // that takes a Context as argument).
    private val Context.appSettingsDataStore by preferencesDataStore(APP_SETTINGS_FILE_NAME)

    @Provides
    @Singleton
    fun provideAppSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.appSettingsDataStore
    }
}
