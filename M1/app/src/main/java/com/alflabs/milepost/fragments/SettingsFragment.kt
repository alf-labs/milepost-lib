package com.alflabs.milepost.fragments

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.milepost.R
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.utils.Units
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
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
            ?.withSummaryProvider { preference ->
                val text = preference.text
                val value = text?.toFloatOrNull()
                if (value == null) {
                    "Invalid number: $text"
                } else if (useImperialUnits) {
                    "${prettyFloat(value, 3)} meters (${prettyFloat(Units.m_to_miles(value), 3)} miles)"
                } else if (value >= 1000) {
                    "${prettyFloat(value, 3)} meters (${prettyFloat(Units.m_to_km(value), 3)} km)"
                } else {
                    "${prettyFloat(value, 3)} meters"
                }
            }

        thresholdSpeedPref =
            findPreference<ImperialEditTextPreference>(AppSettings.THRESHOLD_SPEED_MPS.name)
            ?.withSummaryProvider { preference ->
                val text = preference.text
                val value = text?.toFloatOrNull()
                if (value == null) {
                    "Invalid number: $text"
                } else {
                    "${prettyFloat(value, 3)} m/s (${prettyFloat(Units.mpsToUnit(value, useImperialUnits), 3)} ${Units.mpsUnitName(useImperialUnits)})"
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        lifecycleScope.launch {
            useImperialUnits = appSettings.useImperialUnits()

            thresholdDistancePref?.setUseImperialUnits(useImperialUnits)
            thresholdSpeedPref?.setUseImperialUnits(useImperialUnits)

            appSettings.appSettingsDataStore.data.asLiveData().observe(viewLifecycleOwner) { pref ->
                pref[AppSettings.USE_IMPERIAL_UNITS]?.let { unit ->
                    useImperialUnits = unit
                    thresholdDistancePref?.setUseImperialUnits(useImperialUnits)
                    thresholdSpeedPref?.setUseImperialUnits(useImperialUnits)
                }
            }
        }

        return view
    }

    /** Formats the float to the given precision and removes all trailing zeroes. */
    fun prettyFloat(value: Float, precision: Int): String {
        val s = String.format("%.${precision}f", value)

        // First implementation:
        // val suffix = ".${"0".repeat(precision)}"
        // if (s.endsWith(suffix)) {
        //     s = s.dropLast(suffix.length)
        // }

        // Second implementation:
        return BigDecimal(s).stripTrailingZeros().toPlainString()
    }
}

class ImperialEditTextPreference : EditTextPreference {
    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int,
                defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    constructor(context: Context,
                attrs: AttributeSet?,
    ) : super(context, attrs)

    constructor(context: Context,
    ) : super(context)

    private var useImperialUnits: Boolean = false

    fun setUseImperialUnits(useImperialUnits: Boolean) {
        this.useImperialUnits = useImperialUnits
        notifyChanged()
    }

    fun withSummaryProvider(summaryProvider: SummaryProvider<EditTextPreference>): ImperialEditTextPreference {
        this.summaryProvider = summaryProvider
        return this
    }

}
