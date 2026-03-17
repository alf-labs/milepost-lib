package com.alflabs.milepost.fragments.docentTracks

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.TextView
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
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.databinding.DocentTracksFragmentBinding
import com.alflabs.milepost.db.AudioSegmentDao
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackDao
import com.alflabs.milepost.utils.DocentImportValidationData
import com.alflabs.milepost.utils.DocentImportValidationResult
import com.alflabs.milepost.utils.DocentImporterExporter
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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
    @Inject lateinit var docentImporterExporter: DocentImporterExporter
    @Inject lateinit var docentTrackDao: DocentTrackDao
    @Inject lateinit var gpsTrackDao: GpsTrackDao
    @Inject lateinit var audioSegmentDao: AudioSegmentDao

    private var _binding: DocentTracksFragmentBinding? = null
    private val binding get() = _binding!!
    private var docentTrackAdapter: DocentTrackAdapter? = null
    private var docentPlaybackTrackId: Long = -1

    private val viewModel: DocentTracksViewModel by viewModels()

    private val selectJsonFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { importJsonLoadUri(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DocentTracksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "@@ onViewCreated")

        setupMenu()

        lifecycleScope.launch {
            docentPlaybackTrackId = appSettings.docentPlaybackTrackId()

            docentTrackAdapter = DocentTrackAdapter(
                onClick = ::showEditDialog,
                onEdit = ::showEditDialog,
                onDelete = ::showDeleteDialog,
                docentPlaybackTrackId,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.tracksList.adapter = docentTrackAdapter
            binding.tracksList.layoutManager = layoutManager

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.tracksList.addItemDecoration(dividerItemDecoration)


            viewModel.tracks.observe(viewLifecycleOwner) { list ->
                binding.emptyMessage.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
                docentTrackAdapter?.submitList(list)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

                    R.id.action_import -> {
                        importJsonSelectFile()
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
            gpsTrackId = 3,
        )
        viewModel.insert(trackEntity)
        // TODO remove this entirely
        Snackbar.make(requireView(), "TBD Docent track add", Snackbar.LENGTH_LONG).show()
    }

    private fun showEditDialog(item: DocentTrackItem) {
        findNavController().navigate(
            DocentTracksFragmentDirections.actionEditDocentTrack(item.id)
        )
    }

    private fun showDeleteDialog(item: DocentTrackItem) {
        // Display an alert to confirm deletion.
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Docent Track")
            .setMessage("Are you sure you want to delete docent track '${item.userTitle}'? This cannot be reversed.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteById(item.id)
                Snackbar.make(
                    requireView(),
                    "Docent track '${item.userTitle}' deleted",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importJsonSelectFile() {
        logger.d(TAG, "@@ ImportJSON 1 - Select File")
        // 1- Ask user to select a JSON file using the Storage Access Framework (SAF).
        selectJsonFileLauncher.launch("application/json")
    }

    private fun importJsonLoadUri(uri: Uri) {
        // 2- When a file is selected, load it in memory using the Jackson library for GpsTrackEntity.class
        logger.d(TAG, "@@ ImportJSON 2 - Load URI: $uri")

        lifecycleScope.launch {
            val result = docentImporterExporter.loadDocentTrackEntityFromUri(
                requireContext(),
                uri,
                this@DocentTracksFragment::importJsonValidator
            )
            if (result == null) {
                Toast.makeText(requireContext(), "Failed to load JSON", Toast.LENGTH_LONG).show()
                return@launch
            } else {
                Toast.makeText(requireContext(), "Import Result: $result", Toast.LENGTH_LONG).show()
            }
        }

    }

    private suspend fun importJsonValidator(data: DocentImportValidationData): DocentImportValidationResult {
        logger.d(TAG, "@@ importJsonValidator: $data")

        val dialogView = layoutInflater.inflate(R.layout.docent_import_dialog, null)
        val userTitleText = dialogView.findViewById<TextInputEditText>(R.id.user_title)
        val gpsTrackLabel = dialogView.findViewById<TextView>(R.id.gps_track_label)
        val replaceExistingCheckbox = dialogView.findViewById<CheckBox>(R.id.replace_existing)
        val gpsTracksDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.gps_track_dropdown)
        val audioSegmentsDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.audio_segment_dropdown)

        userTitleText.setText(data.userTitle)
        gpsTrackLabel.text = data.gpsTrackName

        val docentId = docentTrackDao.getIdByUuid(data.docentTrackUuid)
        logger.d(TAG, "@@ importJsonValidator: docentId = $docentId")

        replaceExistingCheckbox.isChecked = false
        replaceExistingCheckbox.isEnabled = docentId != null

        val gpsTrackId = gpsTrackDao.getIdByUuid(data.gpsTrackUuid)

        val gpsTracks = gpsTrackDao.getItemsAsync()
        val gpsTracksNames = gpsTracks.map { it.displayTrackLabel() }
        val gpsTracksAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, gpsTracksNames)
        gpsTracksDropdown.setAdapter(gpsTracksAdapter)

        val audioSegments = audioSegmentDao.getItemsAsync()
        val audioSegmentsNames = audioSegments.map { it.displayTitle() }
        val audioSegmentsAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, audioSegmentsNames)
        audioSegmentsDropdown.setAdapter(audioSegmentsAdapter)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Import Docent Track")
            .setView(dialogView)
            .setPositiveButton("Import") { _, _ ->
                // TBD
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()


        return DocentImportValidationResult(
            canContinue = false,
            message = "TBD $docentId",
            gpsTrackId = 0,
            descriptorsToId = emptyMap(),
            descriptorsToUri = emptyMap(),
        )
    }
}
