package com.alflabs.milepostlib.dagger

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.hardware.usb.UsbManager
import android.media.AudioManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Optional

@Module
@InstallIn(SingletonComponent::class)
object SystemServicesModule {

    @Provides
    fun provideAudioManager(@ApplicationContext context: Context): Optional<AudioManager> {
        return Optional.ofNullable(context.getSystemService(AudioManager::class.java))
    }

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): Optional<NotificationManager> {
        return Optional.ofNullable(context.getSystemService(NotificationManager::class.java))
    }

    @Provides
    fun provideActivityManager(@ApplicationContext context: Context): Optional<ActivityManager> {
        return Optional.ofNullable(context.getSystemService(ActivityManager::class.java))
    }

    @Provides
    fun provideUsbManager(@ApplicationContext context: Context): Optional<UsbManager> {
        return Optional.ofNullable(context.getSystemService(UsbManager::class.java))
    }
}
