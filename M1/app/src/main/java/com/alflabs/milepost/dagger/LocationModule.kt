package com.alflabs.milepost.dagger

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

//RM TEMP moved to Lib
//@Module
//@InstallIn(SingletonComponent::class)
object LocationModule {

//    @Provides
//    fun provideFusedLocationProviderClient(
//        @ApplicationContext context: Context
//    ): FusedLocationProviderClient {
//        return LocationServices.getFusedLocationProviderClient(context)
//    }
//
//    @Provides
//    fun provideLocationRequest(): LocationRequest {
//        return LocationRequest.Builder(
//            /*priority=*/ Priority.PRIORITY_HIGH_ACCURACY,
//            /*intervalMillis=*/ 1000).apply {
//            setIntervalMillis(1000)
//            setMinUpdateIntervalMillis(500)
//        }.build()
//    }
}
