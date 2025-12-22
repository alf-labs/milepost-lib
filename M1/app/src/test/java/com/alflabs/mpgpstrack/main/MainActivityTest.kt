package com.alflabs.mpgpstrack.main

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.utils.ILogger
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class MainActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var logger: ILogger
    private val fakeLogger get() = logger as StringLogger

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun activity_launches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertThat(activity).isNotNull()
            assertThat(fakeLogger.string).contains("MainActivity: onCreate")
            assertThat(fakeLogger.string).contains("MainActivity: Start page: STOPPED")
        }
    }
}
