package com.alflabs.milepost.app

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

        // A warning on expected key types used in visual preference fragment editor:
        // For numerics, it's typical to use <EditTextPreference> in preferences.xml.
        // This _always_ uses getString / putString in the data store adapter as it knows
        // nothing about actual numeric types. The typical behavior is to store all numbers
        // as stringPreferencesKey, and then provide an app-specific getter that does the
        // type conversion (e.g. toFloat, etc., with a reasonable fallback default value.)
        //
        // This does not apply to values used only in-app and not visible in the settings fragment.

        // Values visible in the settings fragment, they must be boolean (checkbox) or string.
        val USE_IMPERIAL_UNITS = booleanPreferencesKey("use_imperial_units")
        val THRESHOLD_DISTANCE_METERS = stringPreferencesKey("threshold_distance_meters")
        val THRESHOLD_SPEED_MPS = stringPreferencesKey("threshold_speed_mps")

        val SIMULATE_GPS = stringPreferencesKey("simulate_gps")

        val USE_LOCK_TASK = booleanPreferencesKey("use_lock_task")
        val HIDE_SYSTEM_BARS = booleanPreferencesKey("hide_system_bars")

        val USE_USB_AUDIO = booleanPreferencesKey("use_usb_audio")
        val USE_LOUDNESS_ENHANCER = booleanPreferencesKey("use_loudness_enhancer")
        val USE_SOUND_LIMITER = booleanPreferencesKey("use_sound_limiter")

        // The following values are only used internally, they can be of any type.
        val GPS_PLAYBACK_TRACK_ID = longPreferencesKey("gps_playback_track_id")
        val DOCENT_PLAYBACK_TRACK_ID = longPreferencesKey("docent_playback_track_id")
        val MAP_ZOOM_LEVEL = floatPreferencesKey("map_zoom_level")

        const val THRESHOLD_DISTANCE_METERS_DEFAULT = 100.0f
        const val THRESHOLD_SPEED_MPS_DEFAULT = 0.25f
        const val MAP_ZOOM_LEVEL_DEFAULT = 15f
    }

    @Inject lateinit var appSettingsDataStore: DataStore<Preferences>

    // Preferences access

    suspend fun useImperialUnits(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(USE_IMPERIAL_UNITS.name, false)
    }

    suspend fun useLockTask(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(USE_LOCK_TASK.name, false)
    }

    suspend fun hideSystemBars(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(HIDE_SYSTEM_BARS.name, false)
    }

    suspend fun thresholdDistanceMeters(): Float {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getString(THRESHOLD_DISTANCE_METERS.name, "").toFloatOrNull()
            ?: THRESHOLD_DISTANCE_METERS_DEFAULT
    }

    suspend fun thresholdSpeedMps(): Float {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getString(THRESHOLD_SPEED_MPS.name, "").toFloatOrNull()
            ?: THRESHOLD_SPEED_MPS_DEFAULT
    }

    suspend fun gpsPlaybackTrackId(): Long {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getLong(GPS_PLAYBACK_TRACK_ID.name, -1)
    }

    suspend fun setGpsPlaybackTrackId(trackId: Long) {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        adapter.putLong(GPS_PLAYBACK_TRACK_ID.name, trackId)
    }

    suspend fun docentPlaybackTrackId(): Long {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getLong(DOCENT_PLAYBACK_TRACK_ID.name, -1)
    }

    suspend fun setDocentPlaybackTrackId(trackId: Long) {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        adapter.putLong(DOCENT_PLAYBACK_TRACK_ID.name, trackId)
    }

    suspend fun mapZoomLevel(): Float {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getFloat(MAP_ZOOM_LEVEL.name, MAP_ZOOM_LEVEL_DEFAULT)
    }

    suspend fun setMapZoomLevel(level: Float) {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        adapter.putFloat(MAP_ZOOM_LEVEL.name, level)
    }

    suspend fun simulateGps(): String {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getString(SIMULATE_GPS.name, "none")
    }

    suspend fun useUsbAudio(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(USE_USB_AUDIO.name, false)
    }

    suspend fun useLoundnessEnhancer(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(USE_LOUDNESS_ENHANCER.name, false)
    }

    suspend fun useSoundLimiter(): Boolean {
        val adapter = AsyncPreferenceDataStore(appSettingsDataStore)
        return adapter.getBoolean(USE_SOUND_LIMITER.name, false)
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
 *
 * TBD: deprecate this and re-implement using suspend methods in AsyncPreferenceDataStore.
 */
@Deprecated("Use AsyncPreferenceDataStore")
class AdapterPreferenceDataStore(
    val dataStore: DataStore<Preferences>,
    val coroutineScope: CoroutineScope
) : PreferenceDataStore() {

    private fun <T> putPreference(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
            Log.d(AppSettings.TAG, "PUT preference $key with value $value")
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

class AsyncPreferenceDataStore(val dataStore: DataStore<Preferences>) {

    private suspend fun <T> putAsync(key: Preferences.Key<T>, value: T?) {
        dataStore.edit {
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
        Log.d(AppSettings.TAG, "PUT preference $key with value $value")
    }

    private suspend fun <T> getAsync(
        key: Preferences.Key<T>,
        defValue: T
    ): T {
        return dataStore.data.map { it[key] ?: defValue }.first()
    }

    suspend fun putString(key: String, value: String?) {
        putAsync(stringPreferencesKey(key), value)
    }

    suspend fun getString(key: String, defValue: String?): String =
        getAsync(stringPreferencesKey(key), defValue ?: "")

    suspend fun putBoolean(key: String, value: Boolean) {
        putAsync(booleanPreferencesKey(key), value)
    }

    suspend fun getBoolean(key: String, defValue: Boolean): Boolean =
        getAsync(booleanPreferencesKey(key), defValue)

    suspend fun putInt(key: String, value: Int) {
        putAsync(intPreferencesKey(key), value)
    }

    suspend fun getInt(key: String, defValue: Int): Int =
        getAsync(intPreferencesKey(key), defValue)

    suspend fun putLong(key: String, value: Long) {
        putAsync(longPreferencesKey(key), value)
    }

    suspend fun getLong(key: String, defValue: Long): Long =
        getAsync(longPreferencesKey(key), defValue)

    suspend fun putFloat(key: String, value: Float) {
        putAsync(floatPreferencesKey(key), value)
    }

    suspend fun getFloat(key: String, defValue: Float): Float =
        getAsync(floatPreferencesKey(key), defValue)
}
