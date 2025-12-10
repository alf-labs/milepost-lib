package com.alflabs.mpgpstrack.fragments

import android.os.Bundle
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.mpgpstrack.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        internal val TAG = SettingsFragment::class.java.simpleName
    }

    @Inject lateinit var appSettingsDataStore: DataStore<Preferences>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        preferenceManager.preferenceDataStore = AdapterPreferenceDataStore(
            appSettingsDataStore,
            lifecycleScope
        )
    }
}

/**
 * A PreferenceDataStore implementation that stores preferences in a Jetpack Preferences DataStore.
 * Inspired by discussions from https://stackoverflow.com/questions/65396998
 */
class AdapterPreferenceDataStore(
    val dataStore: DataStore<Preferences>,
    val lifecycleScope: LifecycleCoroutineScope
) : PreferenceDataStore() {

    private fun <T> putPreference(key: Preferences.Key<T>, value: T?) {
        lifecycleScope.launch {
            dataStore.edit {
                Log.d(SettingsFragment.TAG, "PUT preference $key with value $value")
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
        Log.d(SettingsFragment.TAG, "GET preference $key")
        val value = runBlocking {
            dataStore.data.map { it[key] ?: defValue }.first()
        }
        Log.d(SettingsFragment.TAG, "GET preference $key --> value $value")
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

    override fun putFloat(key: String, value: Float) {
        putPreference(floatPreferencesKey(key), value)
    }

    override fun getFloat(key: String, defValue: Float): Float =
        getBlocking(floatPreferencesKey(key), defValue)
}
