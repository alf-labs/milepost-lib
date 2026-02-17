package com.alflabs.milepost.fragments.docentTracks

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
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.databinding.DocentTracksFragmentBinding
import com.alflabs.milepost.tracks.DocentTrackEntity
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class DocentTracksFragment : Fragment() {

    companion object {
        private val TAG = DocentTracksFragment::class.java.simpleName
    }

    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var logger: ILogger

    private var _binding: DocentTracksFragmentBinding? = null
    private val binding get() = _binding!!
    private var gpsPlaybackTrackId: Long = -1

    private val viewModel: DocentTracksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DocentTracksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        lifecycleScope.launch {
            gpsPlaybackTrackId = appSettings.playbackTrackId()

            val adapter = DocentTrackAdapter(
                onClick = { track ->
                    // TBD
                },
                onEdit = { track ->
                    // TBD
                },
                onDelete = { track ->
                    // TBD
                },
                useImperialUnits = false,
                gpsPlaybackTrackId,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.tracksList.adapter = adapter
            binding.tracksList.layoutManager = layoutManager

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.tracksList.addItemDecoration(dividerItemDecoration)


            viewModel.tracks.observe(viewLifecycleOwner) { list ->
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
                menuInflater.inflate(R.menu.docent_tracks_menu, menu)
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
            val trackEntity = DocentTrackEntity(
                userTitle = "New Docent Track",
                creationTime = Instant.now().toString(),
                lastUsedTime = Instant.now().toString(),
                gpsTrackId = gpsPlaybackTrackId,
            )
            viewModel.insert(trackEntity)
    }

    private fun doRemoveEntry() {
        // TBD
//        viewModel.delete(audioFile) // TBD
        Snackbar.make(requireView(), "TBD Docent track deleted", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
