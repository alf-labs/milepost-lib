package com.alflabs.milepost.dagger

import com.alflabs.utils.AndroidClock
import com.alflabs.utils.IClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//RM TEMP moved to Lib
//@Module
//@InstallIn(SingletonComponent::class)
object ClockModule {

//    @Singleton
//    @Provides
//    fun providesClock(): IClock {
//        return AndroidClock()
//    }
}
