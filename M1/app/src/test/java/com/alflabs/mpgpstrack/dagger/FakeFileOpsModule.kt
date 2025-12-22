package com.alflabs.mpgpstrack.dagger

import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.FileOps
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FileOpsModule::class]
)
object FakeFileOpsModule {

    @Singleton
    @Provides
    fun providesFileOps(): FileOps {
        return FakeFileOps()
    }
}
