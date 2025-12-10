package com.alflabs.mpgpstrack.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import com.alflabs.mpgpstrack.R
//import com.alflabs.mpgpstrack.databinding.SettingsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = SettingsFragment::class.java.simpleName

//    private var _binding: SettingsFragmentBinding? = null
//    private val binding get() = _binding!!
//
//    private val viewModel: SettingsViewModel by viewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
//        return binding.root
//    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

//// Enable DataStore for the entire preference hierarchy
//        preferenceManager.preferenceDataStore = object : PreferenceDataStore() {
//            override fun putBoolean(key: String, value: Boolean) {
//                lifecycleScope.launch {
//                    requireContext().dataStore.edit { preferences ->
//                        preferences[booleanPreferencesKey(key)] = value
//                    }
//                }
//            }
//
//            override fun getBoolean(key: String, defValue: Boolean): Boolean {
//                var storedValue = defValue
//                val job = lifecycleScope.launch {
//                    requireContext().dataStore.data
//                        .map { preferences -> preferences[booleanPreferencesKey(key)] ?: defValue }
//                        .first() // Get the first emitted value and complete
//                        .collect { value -> storedValue = value }
//                }
//                runBlocking { job.join() } // Block until the value is retrieved (for synchronous access)
//                return storedValue
//            }
//
//            // Implement other put/get methods for different data types (String, Int, etc.)
//            // using the corresponding PreferenceKeys (stringPreferencesKey, intPreferencesKey, etc.)
//        }
    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
////            binding.settingsExampleSwitch.isChecked = settings.exampleSetting
//        }
//
////        binding.settingsExampleSwitch.setOnCheckedChangeListener { _, isChecked ->
////            lifecycleScope.launch {
////                viewModel.setExampleSetting(isChecked)
////            }
////        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
}
