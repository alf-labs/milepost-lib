package com.alflabs.mpgpstrack.fragments.settings

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.settings.AppSettings
import com.alflabs.mpgpstrack.settings.copy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = SettingsFragment::class.java.simpleName

    @Inject lateinit var appSettingsDataStore: DataStore<AppSettings>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        preferenceManager.preferenceDataStore = AdapterPreferenceDataStore(
            appSettingsDataStore,
            lifecycleScope
        )
    }
}

/**
 * A PreferenceDataStore implementation that stores preferences in a Jetpack Proto DataStore.
 */
class AdapterPreferenceDataStore(
    val appSettings: DataStore<AppSettings>,
    val lifecycleScope: LifecycleCoroutineScope
) : PreferenceDataStore() {

    override fun putBoolean(key: String, value: Boolean) {
        lifecycleScope.launch {
            appSettings.updateData { prefs ->
                when (key) {
                    "use_imperial_units" -> prefs.copy {
                        locationSettings = locationSettings.copy {
                            useImperialUnits = value
                        }
                    }
                    else -> prefs
                }
            }
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        var storedValue = defValue
        runBlocking {
            lifecycleScope.launch {
                val prefs = appSettings.data.first()
                storedValue = when (key) {
                    "use_imperial_units" -> if (prefs.locationSettings.hasUseImperialUnits()) {
                        prefs.locationSettings.useImperialUnits
                    } else {
                        defValue
                    }
                    else -> defValue
                }
            }.join()
        }
        return storedValue
    }

    override fun putFloat(key: String?, value: Float) {
        lifecycleScope.launch {
            appSettings.updateData { prefs ->
                when (key) {
                    "threshold_distance_meters" -> prefs.copy {
                        locationSettings = locationSettings.copy {
                            thresholdDistanceMeters = value
                        }
                    }
                    "threshold_speed_mps" -> prefs.copy {
                        locationSettings = locationSettings.copy {
                            thresholdSpeedMps = value
                        }
                    }
                    else -> prefs
                }
            }
        }
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        var storedValue = defValue
        runBlocking {
            lifecycleScope.launch {
                val prefs = appSettings.data.first()
                storedValue = when (key) {
                    "threshold_distance_meters" -> if (prefs.locationSettings.hasThresholdDistanceMeters()) {
                        prefs.locationSettings.thresholdDistanceMeters
                    } else {
                        defValue
                    }
                    "threshold_speed_mps" -> if (prefs.locationSettings.hasThresholdSpeedMps()) {
                        prefs.locationSettings.thresholdSpeedMps
                    } else {
                        defValue
                    }
                    else -> defValue
                }
            }.join()
        }
        return storedValue
    }

    override fun putString(key: String, value: String?) {
        lifecycleScope.launch {
            appSettings.updateData { prefs ->
                // Map the key to the correct field in your proto
                when (key) {
                    // "placeholder" -> prefs.toBuilder().setPlaceholder(value).build()
                    else -> prefs
                }
            }
        }
    }

    override fun getString(key: String, defValue: String?): String? {
        var storedValue = defValue
        runBlocking {
            lifecycleScope.launch {
                val prefs = appSettings.data.first()
                storedValue = when (key) {
                    // "placeholder" -> prefs.placeholder.ifEmpty { defValue }
                    else -> defValue
                }
            }.join()
        }
        return storedValue
    }

    override fun putInt(key: String, value: Int) {
        lifecycleScope.launch {
            appSettings.updateData { prefs ->
                when (key) {
                    // "placeholder" -> prefs.toBuilder().setPlaceholder(value).build()
                    else -> prefs
                }
            }
        }
    }

    override fun getInt(key: String, defValue: Int): Int {
        var storedValue = defValue
        runBlocking {
            lifecycleScope.launch {
                val prefs = appSettings.data.first()
                storedValue = when (key) {
                    // "placeholder" -> if (prefs.hasPlaceholder()) prefs.placeholder else defValue
                    else -> defValue
                }
            }.join()
        }
        return storedValue
    }
}
