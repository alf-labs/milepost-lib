package com.alflabs.milepost.dagger

import com.alflabs.utils.FileOps
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FileOpsModule {

    @Singleton
    @Provides
    fun providesFileOps(): FileOps {
        return FileOps()
    }
}
