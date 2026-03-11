package com.alflabs.milepost.fragments.docentTriggers

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.databinding.DocentTriggersFragmentBinding
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.fragments.audioFiles.AudioFileItem
import com.alflabs.milepost.trigger.PlaybackTriggerDirection
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata
import com.alflabs.milepost.utils.DocentImporterExporter
import com.alflabs.milepost.utils.Time
import com.alflabs.milepost.utils.Units
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.toString

@AndroidEntryPoint
class DocentTriggersFragment : Fragment() {

    companion object {
        private val TAG = DocentTriggersFragment::class.java.simpleName
        private const val KEY_SELECTED_POSITION = "selected-position"
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var docentTrackDao: DocentTrackDao
    @Inject lateinit var docentImporterExporter: DocentImporterExporter

    private var _binding: DocentTriggersFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: DocentTriggersFragmentArgs by navArgs()
    private var docentTriggersAdapter: DocentTriggersAdapter? = null
    private val viewModel: DocentTriggersViewModel by viewModels()
    private var docentTrackEntity: DocentTrackEntity? = null
    private var gpsTrackEntity: GpsTrackEntity? = null
    private var allAudioFiles: List<AudioFileItem>? = null
    private var firstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DocentTriggersFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "@@ onViewCreated")

        setupMenu()

        lifecycleScope.launch {
            docentTriggersAdapter = DocentTriggersAdapter(
                onUpdate = ::showAddOrEditTriggerDialog,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.playTriggerList.adapter = docentTriggersAdapter
            binding.playTriggerList.layoutManager = layoutManager

            savedInstanceState?.let {
                val selectedPosition = it.getInt(KEY_SELECTED_POSITION, RecyclerView.NO_POSITION)
                docentTriggersAdapter?.setSelectedPosition(selectedPosition)
            }

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.playTriggerList.addItemDecoration(dividerItemDecoration)

            viewModel.docentTrack.observe(viewLifecycleOwner) { docentTrack ->
                logger.d(TAG, "@@ DocentTriggers observe new docentTrack: ${docentTrack?.userTitle}")
                docentTrackEntity = docentTrack

                if (docentTrack == null) {
                    binding.userTitle.text = "No docent track found"
                } else {
                    binding.userTitle.text = docentTrack.userTitle?.ifBlank { "(Tap to Add Title)" }
                }

                logger.d(TAG, "@@ is first load: $firstLoad")
                if (docentTrack != null && firstLoad) {
                    firstLoad = false
                    if (docentTrack.userTitle.isNullOrEmpty()) {
                        showReviewTitleDialog(docentTrack)
                    }
                }
            }

            viewModel.gpsTrack.observe(viewLifecycleOwner) { gpsTrack ->
                logger.d(TAG, "@@ DocentTriggers observe new GPS track: ${gpsTrack?.trackLabel}")
                gpsTrackEntity = gpsTrack
                val label = gpsTrack?.displayTrackLabel()
                binding.gpsTrackLabel.text = if (label != null) "GPS Track: $label" else "GPS Track not found"
                updateDurations(docentTriggersAdapter?.currentList, gpsTrackEntity)
            }

            viewModel.triggers.observe(viewLifecycleOwner) { list ->
                logger.d(TAG, "@@ DocentTriggers observe new triggers: ${list?.size ?: -1} items")

                binding.emptyMessage.visibility =
                    if (list?.isEmpty() ?: false) View.VISIBLE else View.GONE

                docentTriggersAdapter?.submitList(list)
                updateDurations(list, gpsTrackEntity)
            }

            viewModel.audioFiles.observe(viewLifecycleOwner) { list ->
                logger.d(TAG, "@@ DocentTriggers observe new audio files: ${list?.size ?: -1} items")
                allAudioFiles = list
            }

            binding.userTitle.setOnClickListener {
                showReviewTitleDialog(docentTrackEntity)
            }

            binding.addTriggerFab.setOnClickListener {
                showAddOrEditTriggerDialog(null)
            }

            viewModel.loadDocentTrack(fragmentArgs.docentTrackId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        docentTriggersAdapter?.let { adapter ->
            logger.d(TAG, "@@ onSaveInstanceState: ${adapter.selectedPosition}")
            outState.putInt(KEY_SELECTED_POSITION, adapter.selectedPosition)
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
                menuInflater.inflate(R.menu.docent_editor_menu, menu)
                ApiHelper.Menu_setOptionalIconsVisible(menu, true)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                logger.d(TAG, "@@ onPrepareMenu -- track entity enabled = ${docentTrackEntity != null}")
                menu.findItem(R.id.action_playback).isEnabled = (docentTrackEntity != null)
                menu.findItem(R.id.action_export_json).isEnabled = (docentTrackEntity != null)
                menu.findItem(R.id.action_delete).isEnabled = (docentTrackEntity != null)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                logger.d(TAG, "@@ onMenuItemSelected: ${menuItem.itemId}")
                return when (menuItem.itemId) {
                    R.id.action_playback -> {
                        playbackTrack(docentTrackEntity)
                        true
                    }

                    R.id.action_export_json -> {
                        val triggers = docentTriggersAdapter?.currentList
                        val exportFile =
                            docentImporterExporter.createJsonExportFile(
                                requireContext(),
                                docentTrackEntity,
                                gpsTrackEntity,
                                triggers ?: emptyList()
                            )
                        exportFile?.let {
                            docentImporterExporter.shareFile(requireContext(), it)
                        }
                        true
                    }

                    R.id.action_delete -> {
                        askAndMaybeDeleteTrack(docentTrackEntity)
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun playbackTrack(docentTrack: DocentTrackEntity?) {
        if (docentTrack == null) return
        lifecycleScope.launch {
            logger.d(TAG, "@@ Set prefs Playback track: ${docentTrack.id}")
            appSettings.setDocentPlaybackTrackId(docentTrack.id)
            val action = DocentTriggersFragmentDirections.actionReviewToPlay()
            findNavController().navigate(action)
        }
    }

    private fun askAndMaybeDeleteTrack(docentTrack: DocentTrackEntity?) {
        if (docentTrack == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Track")
            .setMessage("Are you sure you want to delete this docent track?\n" +
                    "This action cannot be reversed.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    if (appSettings.docentPlaybackTrackId() == docentTrack.id) {
                        logger.d(TAG, "@@ Clear prefs Docent Playback track: -1")
                        appSettings.setDocentPlaybackTrackId(-1)
                    }
                    docentTrackDao.deleteById(docentTrack.id)
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddOrEditTriggerDialog(item: DocentTriggerItem?) {
        val isAdd = item == null
        val docentTrack = docentTrackEntity ?: return
        val currentAudioFiles = allAudioFiles ?: emptyList()
        val currTrigger = item?.playbackTrigger

        val dialogView = layoutInflater.inflate(R.layout.docent_editor_trigger_dialog, null)
        val audioFileDropdown =
            dialogView.findViewById<AutoCompleteTextView>(R.id.audio_file_dropdown)
        val audioPlayButton = dialogView.findViewById<ImageButton>(R.id.audio_play_btn)
        val mpMinEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_min_edittext)
        val mpMaxEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_max_edittext)
        val mpDirectionDropdown =
            dialogView.findViewById<AutoCompleteTextView>(R.id.mp_direction_dropdown)
        val inColdStartCheckbox = dialogView.findViewById<CheckBox>(R.id.in_cold_start_checkbox)

        val mpDirItems = listOf("Both", "Towards Lower MP", "Towards Higher MP")
        val mpDirAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mpDirItems)
        mpDirectionDropdown.setAdapter(mpDirAdapter)
        val currMpDirIndex = currTrigger?.mpDirection?.ordinal ?: -1
        val initialMpDirText = mpDirItems.getOrElse(currMpDirIndex) { mpDirItems[0] }
        mpDirectionDropdown.setText(initialMpDirText, /*filter=*/false)

        // Note: this works because we currently impose audio files to have unique filenames.
        val audioFileNames = currentAudioFiles.map { it.fileName }.toList()
        val audioFileAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, audioFileNames)
        audioFileDropdown.setAdapter(audioFileAdapter)
        val currAudioFileIndex = currentAudioFiles.indexOfFirst { it.id == (item?.audioFileId ?: -1) }
        if (currAudioFileIndex >= 0) {
            audioFileDropdown.setText(audioFileNames[currAudioFileIndex], /*filter=*/false)
        }
        logger.d(TAG, "@@ audio files:: #$currAudioFileIndex in ... $audioFileNames")

        audioPlayButton.setOnClickListener {
            logger.d(TAG, "TBD On play button click")
        }

        var currMpMin = currTrigger?.mpMin
        var currMpMax = currTrigger?.mpMax
        if (currMpMin != null && currMpMax != null && currMpMin > currMpMax) {
            val tmp = currMpMin
            currMpMin = currMpMax
            currMpMax = tmp
        }
        mpMinEditText.setText(currMpMin?.toString())
        mpMaxEditText.setText(currMpMax?.toString())

        val currInColdStart = currTrigger?.includeInColdStart
        inColdStartCheckbox.isChecked = currInColdStart ?: true // default is true

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isAdd) "Add Trigger" else "Edit Trigger")
            .setView(dialogView)
            .setPositiveButton(if (isAdd) "Add" else "Update") { _, _ ->
                // Since we have a validator, we know the values here are valid strings
                // or valid floats.
                val newAudio = audioFileDropdown.text.toString().trim()
                val newMpDir = mpDirectionDropdown.text.toString().trim()
                var newMpMin = mpMinEditText.text.toString().toFloat()
                var newMpMax = mpMaxEditText.text.toString().toFloat()
                val newInColdStart = inColdStartCheckbox.isChecked

                if (newMpMin > newMpMax) {
                    val tmp = newMpMin
                    newMpMin = newMpMax
                    newMpMax = tmp
                }

                logger.d(TAG, "@@Apply TRIGGER clicked:")
                logger.d(TAG, "@@  Audio File: $newAudio")
                logger.d(TAG, "@@  MP Direction: $newMpDir")
                logger.d(TAG, "@@  MP Min: $newMpMin")
                logger.d(TAG, "@@  MP Max: $newMpMax")
                logger.d(TAG, "@@  In Cold Start: $newInColdStart")

                var changed = isAdd
                val newTrigger = currTrigger?.toBuilder() ?: PlaybackTriggerMetadata.newBuilder()
                var newEntity = item?.toEntity() ?: PlayTriggerEntity(
                    audioFileId = 0,
                    docentTrackId = docentTrack.id,
                )

                if (newInColdStart != currInColdStart) {
                    changed = true
                    newTrigger.setIncludeInColdStart(newInColdStart)
                }

                if (newMpMin != currMpMin) {
                    changed = true
                    newTrigger.setMpMin(newMpMin)
                }

                if (newMpMax != currMpMax) {
                    changed = true
                    newTrigger.setMpMax(newMpMax)
                }

                val newMpDirIndex = mpDirItems.indexOf(newMpDir)
                if (newMpDirIndex >= 0 && newMpDirIndex != currMpDirIndex) {
                    changed = true
                    newTrigger.setMpDirection(PlaybackTriggerDirection.forNumber(newMpDirIndex))
                }

                val newAudioFileIndex = currentAudioFiles.indexOfFirst { it.fileName == newAudio }
                if (newAudioFileIndex >= 0 && newAudioFileIndex != currAudioFileIndex) {
                    changed = true
                    newEntity = newEntity.copy(audioFileId = currentAudioFiles[newAudioFileIndex].id)
                }

                newEntity = newEntity.copy(playbackTrigger = newTrigger.build())

                if (changed) {
                    lifecycleScope.launch {
                        if (isAdd) {
                            logger.d(TAG, "@@ ADD new trigger: $newEntity")
                            viewModel.insertPlayTrigger(newEntity)
                        } else {
                            logger.d(TAG, "@@ UPDATE trigger: $newEntity")
                            viewModel.updatePlayTrigger(newEntity)
                            notifyItemIdChanged(newEntity.id)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false

        val validationFunction = {
            val hasAudio = audioFileDropdown.text.isNotBlank()
            val hasMpDir = mpDirectionDropdown.text.isNotBlank()
            val newMpMin = mpMinEditText.text.toString().toFloatOrNull()
            val hasMpMin = newMpMin != null
            val newMpMax = mpMaxEditText.text.toString().toFloatOrNull()
            val hasMpMax = newMpMax != null

            positiveButton.isEnabled = hasAudio && hasMpDir
                    && hasMpMin && hasMpMax
                    && newMpMin != newMpMax
        }

        audioFileDropdown.setOnItemClickListener { _, _, _, _ -> validationFunction() }
        mpDirectionDropdown.setOnItemClickListener { _, _, _, _ -> validationFunction() }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validationFunction()
            }
        }

        mpMinEditText.addTextChangedListener(textWatcher)
        mpMaxEditText.addTextChangedListener(textWatcher)

        validationFunction()
    }

    private fun showReviewTitleDialog(docentTrack: DocentTrackEntity?) {
        if (docentTrack == null) return

        val dialogView = layoutInflater.inflate(R.layout.docent_triggers_title_dialog, null)
        val trackTitleEditText = dialogView.findViewById<TextInputEditText>(R.id.track_title_edittext)

        trackTitleEditText.setText(docentTrack.displayUserTitle())

        AlertDialog.Builder(requireContext())
            .setTitle("Docent Track Title")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val newTitle = trackTitleEditText.text.toString().trim()

                logger.d(TAG, "@@Apply TITLE clicked:")
                logger.d(TAG, "@@  Title: $newTitle")


                if (newTitle != docentTrack.userTitle) {
                    val newDocentTrackEntity = docentTrack.copy(userTitle = newTitle)
                    lifecycleScope.launch {
                        viewModel.updateDocentTrack(newDocentTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun notifyItemIdChanged(id: Long) : Int {
        docentTriggersAdapter?.let { adapter ->
            val list = adapter.currentList
            val first = list.indexOfFirst { it.id == id }
            if (first >= 0) {
                adapter.notifyItemChanged(first)
                return@notifyItemIdChanged first
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun updateDurations(
        list: List<DocentTriggerItem>?,
        gpsTrackEntity: GpsTrackEntity?
    ) {
        val locations = gpsTrackEntity?.locationList ?: return
        if (list.isNullOrEmpty()) return
        list.forEach { item ->
            val oldDisplay = item.durationDisplay
            val oldDurMp = item.durationMp
            val durationMs = item.metadata?.durationMs ?: 0L
            item.durationDisplay = Time.formatMillis(durationMs)

            // Find the speed of all the locations matching the trigger interval
            item.playbackTrigger?.let { trigger ->
                val mpMin = trigger.mpMin
                val mpMax = trigger.mpMax

                val speedsMPH = locations
                    .locationsList
                    .filter {
                        it.hasSpeed() &&
                        it.speed.speedMps > 0 &&
                        it.hasMp() && it.mp >= mpMin && it.mp <= mpMax
                    }
                    .map { Units.mps_to_mph(it.speed.speedMps) }

                val medianMPH = medianSpeed(speedsMPH)
                val averageMPG = averageSpeed(speedsMPH)

                logger.d(TAG, "@@ updateDurations: trigger ${item.id} -> avg $averageMPG / median $medianMPH / speeds $speedsMPH")
                val newDurMp = medianMPH * durationMs.toFloat() / 1000f / 3600f
                item.durationMp = newDurMp
            }

            if (oldDisplay != item.durationDisplay || oldDurMp != item.durationMp) {
                logger.d(TAG, "@@ updateDurations: trigger ${item.id} -> duration ${item.durationDisplay} (${item.durationMp} MP)")
                notifyItemIdChanged(item.id)
            }
        }
    }

    private fun medianSpeed(speeds: List<Float>) : Float {
        if (speeds.isEmpty()) return 0.0f
        val sorted = speeds.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    private fun averageSpeed(speeds: List<Float>) : Float {
        if (speeds.isEmpty()) return 0.0f
        return speeds.average().toFloat()
    }
}
