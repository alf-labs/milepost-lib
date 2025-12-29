package com.alflabs.milepost.main

import android.icu.util.TimeZone
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceDataStore
import com.alflabs.utils.ILogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    val logger: ILogger
) {
    companion object {
        internal val TAG = AppSettings::class.java.simpleName

        // A warning on expected key types:
        // For numerics, it's typical to use <EditTextPreference> in preferences.xml.
        // This _always_ uses getString / putString in the data store adapter as it knows
        // nothing about actual numeric types. The typical behavior is to store all numbers
        // as stringPreferencesKey, and then provide an app-specific getter that does the
        // type conversion (e.g. toFloat, etc., with a reasonable fallback default value.)

        val USE_IMPERIAL_UNITS = booleanPreferencesKey("use_imperial_units")
        val THRESHOLD_DISTANCE_METERS = stringPreferencesKey("threshold_distance_meters")
        val THRESHOLD_SPEED_MPS = stringPreferencesKey("threshold_speed_mps")
        val PLAYBACK_TRACK_ID = longPreferencesKey("playback_track_id")

        const val THRESHOLD_DISTANCE_METERS_DEFAULT = 100.0f
        const val THRESHOLD_SPEED_MPS_DEFAULT = 0.25f
    }

    @Inject lateinit var appSettingsDataStore: DataStore<Preferences>

    // Preferences access

    fun useImperialUnits(coroutineScope: CoroutineScope): Boolean {
        val adapter = createAdapter(coroutineScope)
        return adapter.getBoolean(USE_IMPERIAL_UNITS.name, false)
    }

    fun thresholdDistanceMeters(coroutineScope: CoroutineScope): Float {
        val adapter = createAdapter(coroutineScope)
        return adapter.getString(THRESHOLD_DISTANCE_METERS.name, "").toFloatOrNull()
            ?: THRESHOLD_DISTANCE_METERS_DEFAULT
    }

    fun thresholdSpeedMps(coroutineScope: CoroutineScope): Float {
        val adapter = createAdapter(coroutineScope)
        return adapter.getString(THRESHOLD_SPEED_MPS.name, "").toFloatOrNull()
            ?: THRESHOLD_SPEED_MPS_DEFAULT
    }

    fun playbackTrackId(coroutineScope: CoroutineScope): Long {
        val adapter = createAdapter(coroutineScope)
        return adapter.getLong(PLAYBACK_TRACK_ID.name, -1)
    }

    fun setPlaybackTrackId(coroutineScope: CoroutineScope, trackId: Long) {
        val adapter = createAdapter(coroutineScope)
        adapter.putLong(PLAYBACK_TRACK_ID.name, trackId)
    }

    // ------

    fun createAdapter(coroutineScope: CoroutineScope): AdapterPreferenceDataStore {
        return AdapterPreferenceDataStore(
            appSettingsDataStore,
            coroutineScope)
    }

    fun initializeDefaults(coroutineScope: CoroutineScope) {
        val adapter = createAdapter(coroutineScope)
        coroutineScope.launch {
            val values: Preferences = appSettingsDataStore.data.first()

            if (!values.contains(USE_IMPERIAL_UNITS)) {
                adapter.putBoolean(USE_IMPERIAL_UNITS.name, isUsTimezone())
            }

            if (!values.contains(THRESHOLD_DISTANCE_METERS)) {
                adapter.putString(THRESHOLD_DISTANCE_METERS.name,
                    THRESHOLD_DISTANCE_METERS_DEFAULT.toString())
            }

            if (!values.contains(THRESHOLD_SPEED_MPS)) {
                adapter.putString(THRESHOLD_SPEED_MPS.name,
                    THRESHOLD_SPEED_MPS_DEFAULT.toString())
            }
        }
    }

    /** Checks if the device's current timezone is a US timezone. */
    fun isUsTimezone(): Boolean {
        // Get the ID of the default timezone for this JVM/device.
        val currentTimezoneId = ZoneId.systemDefault().id

        // Get a list of all IANA timezone IDs associated with the "US" region. [16]
        // This is a robust way to get all US-related timezones, including territories.
        val usTimezoneIds = TimeZone.getAvailableIDs("US")

        return usTimezoneIds.contains(currentTimezoneId)
    }
}

/**
 * A PreferenceDataStore implementation that stores preferences in a Jetpack Preferences DataStore.
 * Inspired by discussions from https://stackoverflow.com/questions/65396998
 *
 * Warning: for completeness this implements all types; however for UI preferences.xml types,
 * we'll ONLY use boolean (on/off toggles) and string (including for NUMBERs or ENUMs!)
 * Do not expect to load/store float and int unless it's for internal usage only.
 */
class AdapterPreferenceDataStore(
    val dataStore: DataStore<Preferences>,
    val coroutineScope: CoroutineScope
) : PreferenceDataStore() {

    private fun <T> putPreference(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.edit {
                Log.d(AppSettings.TAG, "PUT preference $key with value $value")
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
        }
    }

    private fun <T> getBlocking(
        key: Preferences.Key<T>,
        defValue: T
    ): T {
        val value = runBlocking {
            dataStore.data.map { it[key] ?: defValue }.first()
        }
        Log.d(AppSettings.TAG, "GET preference $key --> value $value")
        return value
    }

    override fun putString(key: String, value: String?) {
        putPreference(stringPreferencesKey(key), value)
    }

    override fun getString(key: String, defValue: String?): String =
        getBlocking(stringPreferencesKey(key), defValue ?: "")

    override fun putBoolean(key: String, value: Boolean) {
        putPreference(booleanPreferencesKey(key), value)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        getBlocking(booleanPreferencesKey(key), defValue)

    override fun putInt(key: String, value: Int) {
        putPreference(intPreferencesKey(key), value)
    }

    override fun getInt(key: String, defValue: Int): Int =
        getBlocking(intPreferencesKey(key), defValue)

    override fun putLong(key: String, value: Long) {
        putPreference(longPreferencesKey(key), value)
    }

    override fun getLong(key: String, defValue: Long): Long =
        getBlocking(longPreferencesKey(key), defValue)

    override fun putFloat(key: String, value: Float) {
        putPreference(floatPreferencesKey(key), value)
    }

    override fun getFloat(key: String, defValue: Float): Float =
        getBlocking(floatPreferencesKey(key), defValue)
}
