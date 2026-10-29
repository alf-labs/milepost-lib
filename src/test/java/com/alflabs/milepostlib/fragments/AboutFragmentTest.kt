package com.alflabs.milepostlib.fragments

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.dagger.launchFragmentInHiltContainer
import com.alflabs.utils.ILogger
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class AboutFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var logger: ILogger
    private val fakeLogger get() = logger as StringLogger

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun aboutFragment_isDisplayed() {
        // Important: launchFragmentInContainer() does NOT work with Robolectric + Hilt as the
        // default empty app is not a hilt entry point. This uses a workaround that is
        // described by the https://github.com/android/architecture-samples project.
        launchFragmentInHiltContainer<AboutFragment>()

        onView(withId(R.id.webview)).check(matches(not(isDisplayed())))
        assertThat(fakeLogger.string).contains("AboutFragment: WebView loaded")
    }
}
