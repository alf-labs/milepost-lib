package com.alflabs.milepost.fragments.tracks

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.GpsTracksFragmentBinding
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.tracks.GpsTrackDisplayInfo
import com.alflabs.milepost.utils.ImporterExporter
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GpsTracksFragment : Fragment() {

    companion object {
        private val TAG = GpsTracksFragment::class.java.simpleName
    }

    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var logger: ILogger
    @Inject lateinit var importerExporter: ImporterExporter

    private var _binding: GpsTracksFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GpsTracksViewModel by viewModels()
    private var useImperialUnits = false

    private val selectJsonFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { importJsonLoadUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GpsTracksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()

        lifecycleScope.launch {
            useImperialUnits = appSettings.useImperialUnits()

            viewLifecycleOwner.lifecycleScope.launch {
                val playbackTrackId = appSettings.playbackTrackId()

                val adapter = GpsTrackAdapter(
                    onClick = { track ->
                        showEditDialog(track)
                    },
                    onEdit = { track ->
                        showEditDialog(track)
                    },
                    onDelete = { track ->
                        viewModel.delete(track)
                    },
                    useImperialUnits,
                    playbackTrackId,
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
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.tracks_menu, menu)
                ApiHelper.Menu_setOptionalIconsVisible(menu, true)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_import -> {
                        importJsonSelectFile()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun importJsonSelectFile() {
        logger.d(TAG, "@@ ImportJSON 1 - Select File")
        // 1- Ask user to select a JSON file using the Storage Access Framework (SAF).
        selectJsonFileLauncher.launch("application/json")
    }

    private fun importJsonLoadUri(uri: Uri) {
        // 2- When a file is selected, load it in memory using the Jackson library for GpsTrackEntity.class
        logger.d(TAG, "@@ ImportJSON 2 - Load URI: $uri")
        val entity = importerExporter.loadTrackEntityFromUri(requireContext(), uri)
        if (entity == null) {
            Toast.makeText(requireContext(), "Failed to load JSON", Toast.LENGTH_LONG).show()
            return
        }

        val uuid = entity.uuid
        // 3- If the uuid already exists in the database, ask the user if they want to override it or create a new record.
        lifecycleScope.launch {
            val existingId = viewModel.getIdByUuid(uuid)
            logger.d(TAG, "@@ ImportJSON 3 - Existing ID: $uuid --> $existingId")
            if (existingId != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Track Exists")
                    .setMessage("A track with the same UUID already exists. What would you like to do?")
                    .setPositiveButton("Override") { _, _ ->
                        // 4a- Update (override) the existing track entity with the new.
                        logger.d(TAG, "@@ ImportJSON 4a - Update existing ID: $existingId")
                        viewModel.update(entity.copy(id = existingId))
                        Toast.makeText(requireContext(), "Track updated", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Create New") { _, _ ->
                        // 4b- Create a new track entity. It must have a new UUID to not conflict.
                        val id = viewModel.insert(entity.copy(id = 0, uuid = UUID.randomUUID().toString()))
                        logger.d(TAG, "@@ ImportJSON 4b - Create New ID: $id")
                        Toast.makeText(requireContext(), "New track created", Toast.LENGTH_SHORT).show()
                    }
                    .setNeutralButton("Cancel", null)
                    .show()
            } else {
                // 4c- This UUID is unknown. Simply insert the new track entity, let the id be auto-generated.
                val id = viewModel.insert(entity.copy(id = 0))
                logger.d(TAG, "@@ ImportJSON 4c - Insert New ID: $id")
                Toast.makeText(requireContext(), "Track imported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(track: GpsTrackDisplayInfo) {
        findNavController().navigate(
            GpsTracksFragmentDirections.actionListToReview(track.id)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
