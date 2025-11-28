package com.alflabs.mpgpstrack.dagger

import com.alflabs.utils.AndroidLogger
import com.alflabs.utils.ILogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {

    @Singleton
    @Provides
    fun providesLogger(): ILogger {
        return AndroidLogger()
    }
}
