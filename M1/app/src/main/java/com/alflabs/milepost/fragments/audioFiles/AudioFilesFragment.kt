package com.alflabs.milepost.fragments.audioFiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.AudioFilesFragmentBinding
import com.alflabs.milepost.tracks.AudioFileEntity
import com.alflabs.utils.ApiHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant

@AndroidEntryPoint
class AudioFilesFragment : Fragment() {

    private var _binding: AudioFilesFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AudioFilesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AudioFilesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        lifecycleScope.launch {
            val adapter = AudioFileAdapter()

            val layoutManager = LinearLayoutManager(requireContext())
            binding.audioFileList.adapter = adapter
            binding.audioFileList.layoutManager = layoutManager

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.audioFileList.addItemDecoration(dividerItemDecoration)


            viewModel.audioFiles.observe(viewLifecycleOwner) { list ->
                binding.emptyMessage.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(list)
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.audio_files_menu, menu)
                ApiHelper.Menu_setOptionalIconsVisible(menu, true)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        doAddFile()
                        true
                    }
                    R.id.action_delete -> {
                        doRemoveEntry()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun doAddFile() {
        // TBD TESTING ONLY
            val newAudioFile = AudioFileEntity(
                userTitle = "New Recording",
                fileName = "recording_${System.currentTimeMillis()}.mp3",
                lastAccessTime = Instant.now().toString(),
                persistableUri = ""
            )
            viewModel.addAudioFile(newAudioFile)
    }

    private fun doRemoveEntry() {
        // TBD
//        viewModel.deleteAudioFile(audioFile) // TBD
        Snackbar.make(requireView(), "TBD Audio file deleted", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
