package com.alflabs.milepost.service

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

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class ExternalGpsProviderTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    companion object {
        private val TAG = ExternalGpsProviderTest::class.java.simpleName
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var provider: ExternalGpsProvider
    private val fakeLogger get() = logger as StringLogger


    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testSomething() {
        // TBD
        assertThat(fakeLogger.string).isEmpty()
    }

}
