package com.alflabs.milepostlib.dagger

import com.alflabs.utils.ILogger
import com.alflabs.utils.StringLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LoggerModule::class]
)
object FakeLoggerModule {

    @Singleton
    @Provides
    fun providesLogger(): ILogger {
        return StringLogger()
    }
}
