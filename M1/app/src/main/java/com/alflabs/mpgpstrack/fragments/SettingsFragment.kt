package com.alflabs.mpgpstrack.fragments

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.main.AppSettings
import com.alflabs.mpgpstrack.utils.Units
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    companion object {
        internal val TAG = SettingsFragment::class.java.simpleName
    }

    @Inject lateinit var appSettings: AppSettings

    private var thresholdDistancePref: ImperialEditTextPreference? = null
    private var thresholdSpeedPref: ImperialEditTextPreference? = null
    private var useImperialUnits = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = appSettings.createAdapter(lifecycleScope)
        setPreferencesFromResource(R.xml.preferences, rootKey)

        thresholdDistancePref =
            findPreference<ImperialEditTextPreference>(AppSettings.THRESHOLD_DISTANCE_METERS.name)

        thresholdSpeedPref =
            findPreference<ImperialEditTextPreference>(AppSettings.THRESHOLD_SPEED_MPS.name)

        thresholdDistancePref?.summaryProvider =
            Preference.SummaryProvider<EditTextPreference> { preference ->
                val text = preference.text
                val value = text?.toFloatOrNull()
                if (value == null) {
                    "Invalid number: $text"
                } else if (useImperialUnits) {
                    String.format("%,.1f meters (%,.3f miles)", value, Units.m_to_miles(value))
                } else {
                    String.format("%,.1f meters (%,.3f km)", value, Units.m_to_km(value))
                }
            }

        thresholdSpeedPref?.summaryProvider =
            Preference.SummaryProvider<EditTextPreference> { preference ->
                val text = preference.text
                val value = text?.toFloatOrNull()
                if (value == null) {
                    "Invalid number: $text"
                } else if (useImperialUnits) {
                    String.format("%,.1f m/s (%,.3f mph)", value, Units.mps_to_mph(value))
                } else {
                    String.format("%,.1f m/s (%,.3f km/h)", value, Units.mps_to_mph(value))
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        useImperialUnits = appSettings.useImperialUnits(lifecycleScope)

        thresholdDistancePref?.setUseImperialUnits(useImperialUnits)
        thresholdSpeedPref?.setUseImperialUnits(useImperialUnits)

        appSettings.appSettingsDataStore.data.asLiveData().observe(viewLifecycleOwner) { pref ->
            pref[AppSettings.USE_IMPERIAL_UNITS]?.let { unit ->
                useImperialUnits = unit
                thresholdDistancePref?.setUseImperialUnits(useImperialUnits)
                thresholdSpeedPref?.setUseImperialUnits(useImperialUnits)
            }
        }

        return view
    }
}

class ImperialEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {
    private var useImperialUnits: Boolean = false

    fun setUseImperialUnits(useImperialUnits: Boolean) {
        this.useImperialUnits = useImperialUnits
        notifyChanged()
    }

}
