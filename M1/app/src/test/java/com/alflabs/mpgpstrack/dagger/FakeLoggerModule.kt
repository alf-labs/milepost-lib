package com.alflabs.mpgpstrack.dagger

import com.alflabs.utils.ILogger
import com.alflabs.utils.StringLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FakeLoggerModule {

    @Singleton
    @Provides
    fun providesLogger(): ILogger {
        return StringLogger()
    }
}
