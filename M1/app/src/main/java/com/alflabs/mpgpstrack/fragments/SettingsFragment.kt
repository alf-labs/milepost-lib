package com.alflabs.mpgpstrack.fragments

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.main.AppSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        internal val TAG = SettingsFragment::class.java.simpleName
    }

    @Inject lateinit var appSettings: AppSettings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = appSettings.createAdapter(lifecycleScope)
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}

