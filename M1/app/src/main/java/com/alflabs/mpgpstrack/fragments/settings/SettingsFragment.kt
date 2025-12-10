package com.alflabs.mpgpstrack.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.alflabs.mpgpstrack.databinding.SettingsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val TAG = SettingsFragment::class.java.simpleName

    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.appSettings.observe(viewLifecycleOwner) { settings ->
//            binding.settingsExampleSwitch.isChecked = settings.exampleSetting
        }

//        binding.settingsExampleSwitch.setOnCheckedChangeListener { _, isChecked ->
//            lifecycleScope.launch {
//                viewModel.setExampleSetting(isChecked)
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
