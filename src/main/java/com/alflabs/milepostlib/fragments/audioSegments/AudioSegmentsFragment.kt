package com.alflabs.milepostlib.fragments.audioSegments

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.databinding.AudioSegmentsFragmentBinding
import com.alflabs.milepostlib.db.AudioSegmentEntity
import com.alflabs.milepostlib.db.PlayTriggerDao
import com.alflabs.milepostlib.db.audioSegmentMetadata
import com.alflabs.milepostlib.service.MediaPlayerWrapper
import com.alflabs.milepostlib.utils.pluralize
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class AudioSegmentsFragment : Fragment() {

    companion object {
        private val TAG = AudioSegmentsFragment::class.java.simpleName
        private const val KEY_SELECTED_POSITION = "selected-position"
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var playTriggerDao: PlayTriggerDao
    @Inject lateinit var mediaPlayerWrapperFactory: MediaPlayerWrapper.Factory

    private var _binding: AudioSegmentsFragmentBinding? = null
    private val binding get() = _binding!!
    private var audioSegmentAdapter: AudioSegmentAdapter? = null
    private var itemPlaying: AudioSegmentItem? = null
    private var selectAudioSegmentPendingId: Long = -1L
    private lateinit var mediaPlayerWrapper: MediaPlayerWrapper

    private val viewModel: AudioSegmentViewModel by viewModels()

    private val selectAudioFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val contentResolver = requireActivity().applicationContext.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                addAudioFileUri(uri)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AudioSegmentsFragmentBinding.inflate(inflater, container, false)
        mediaPlayerWrapper = mediaPlayerWrapperFactory.create(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "@@ onViewCreated")

        setupMenu()

        lifecycleScope.launch {
            audioSegmentAdapter = AudioSegmentAdapter(
                onPlay = ::onPlayStopItem,
                onUpdate = ::onUpdateAudioDocumentUri,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.audioFileList.adapter = audioSegmentAdapter
            binding.audioFileList.layoutManager = layoutManager

            savedInstanceState?.let {
                val selectedPosition = it.getInt(KEY_SELECTED_POSITION, RecyclerView.NO_POSITION)
                audioSegmentAdapter?.setSelectedPosition(selectedPosition)
            }

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.audioFileList.addItemDecoration(dividerItemDecoration)

            viewModel.items.observe(viewLifecycleOwner) { list ->
                binding.emptyMessage.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
                logger.d(TAG, "@@ AudioSegments observe new list: ${list.size} items")
                validateAllPersistableUris(list)
                audioSegmentAdapter?.submitList(list)
            }

            binding.addAudioFab.setOnClickListener {
                selectNewAudioDocument()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        logger.d(TAG, "@@ onResume")
        lifecycleScope.launch {
            audioSegmentAdapter?.currentList?.let { list ->
                val copy = list.toList()
                val changed = validateAllPersistableUris(copy)
                if (changed) {
                    audioSegmentAdapter?.submitList(copy)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        audioSegmentAdapter?.let { adapter ->
            logger.d(TAG, "@@ onSaveInstanceState: ${adapter.selectedPosition}")
            outState.putInt(KEY_SELECTED_POSITION, adapter.selectedPosition)
        }
    }

    override fun onPause() {
        super.onPause()
        logger.d(TAG, "@@ onPause")
        stopCurrentPlaying()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logger.d(TAG, "@@ onDestroyView")
        stopCurrentPlaying()
        _binding = null
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.audio_files_menu, menu)
                ApiHelper.Menu_setOptionalIconsVisible(menu, true)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.findItem(R.id.action_delete)?.let { menuItem ->
                    val pos = audioSegmentAdapter?.selectedPosition ?: RecyclerView.NO_POSITION
                    menuItem.isEnabled = (pos != RecyclerView.NO_POSITION)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_add -> {
                        selectNewAudioDocument()
                        true
                    }
                    R.id.action_delete -> {
                        askAndMaybeDeleteEntry()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onPlayStopItem(item: AudioSegmentItem) {
        // This toggles play/stop. Only one item can play at a time.
        logger.d(TAG, "@@ onPlayStopItem: CURRENT $itemPlaying")

        itemPlaying?.let { current ->
            logger.d(TAG, "@@ onPlayStopItem: ${current.id} == ${item.id} ?")
            val isSame = current.id == item.id
            stopCurrentPlaying()
            if (isSame) {
                // We stopped the same item, nothing else to do.
                return
            }
        }

        lifecycleScope.launch {
            // Start playing the new item
            mediaPlayerWrapper.startPlaying(
                item.persistableUri.toUri(),
                completionCallback = ::stopCurrentPlaying,
                stopCallback = ::stopCurrentPlaying
            )

            item.isPlaying = true
            itemPlaying = item
            val position = notifyItemIdChanged(item.id)
            logger.d(TAG, "@@ onPlayItem: $position")
        }
    }

    private fun stopCurrentPlaying() {
        logger.d(TAG, "@@ stopCurrentPlaying: with current id ${itemPlaying?.id}")
        mediaPlayerWrapper.stopPlaying()

        itemPlaying?.let { item ->
            itemPlaying = null
            item.isPlaying = false
            val position = notifyItemIdChanged(item.id)
            logger.d(TAG, "@@ stopCurrentPlaying: $position")
        }
    }

    private fun notifyItemIdChanged(id: Long) : Int {
        audioSegmentAdapter?.let { adapter ->
            val list = adapter.currentList
            val first = list.indexOfFirst { it.id == id }
            if (first >= 0) {
                logger.d(TAG, "@@ notifyItemIdChanged: id $id -> pos $first")
                adapter.notifyItemChanged(first)
                return@notifyItemIdChanged first
            }
        }
        return RecyclerView.NO_POSITION
    }

    private fun onUpdateAudioDocumentUri(item: AudioSegmentItem) {
        logger.d(TAG, "@@ onUpdateUriForItem: ${item.id}")
        stopCurrentPlaying()

        // select an audio file and update the URI for this specific item.id
        selectAudioSegmentPendingId = item.id
        selectAudioFileLauncher.launch(arrayOf("audio/*"))
    }

    private fun selectNewAudioDocument() {
        stopCurrentPlaying()

        // 1- Ask user to select an audio file using the Storage Access Framework (SAF).
        // Use a ActivityResultContracts call suitable to return a Persistable URI
        // that can be stored in the database to open the file much later.
        selectAudioSegmentPendingId = -1L
        selectAudioFileLauncher.launch(arrayOf("audio/*"))
    }

    private fun addAudioFileUri(uri: Uri) {
        // 2- Get a persistable uri for the selected file.
        val persistableUri = uri.toString()

        // 3- Get the filename of the selected file.
        // TBD we should really have filenames be unique to visually differentiate entries
        // without having to resort to the db id. This can be done here by adding an index to
        // already existing duplicated filenames.
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        var fileName = "Unknown"
        cursor?.use {
            // Implementation note: cursor.use{} auto-closes the cursor after the scope.
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }

        // 4- Check this is a playable audio file and get its duration.
        var durationMs: Long
        try {
            // If we can successfully retrieve the MediaMetaData duration, it's likely
            // good enough to play with the MediaPlayer.
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), uri)

            // If the file is not a valid media file, the following call might return null or throw
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLong() ?: 0L
            logger.d(TAG, "@@ addAudioFileUri: '$fileName' has duration $durationMs ms")

            retriever.release()

            if (durationMs <= 0) {
                throw Exception("Invalid audio duration")
            }
        } catch (e: Exception) {
            logger.d(TAG, "@@ addAudioFileUri: '$fileName' is not a valid audio file", e)
            AlertDialog.Builder(requireContext())
                .setTitle("Unsupported Format")
                .setMessage("Selected file '$fileName' is not a supported audio format")
                .setPositiveButton("OK", null)
                .show()
            selectAudioSegmentPendingId = -1L
            return // Exit early so we don't save an invalid URI
        }

        val descriptor = "$fileName[0;$durationMs]"

        // One limitation right now is that we use the display filenames to select files,
        // and thus there's no way to differentiate files with the same filename. Thus, we need
        // to reject this new file if it has the same filename as an existing file.
        // However, that's fine if we're updating a file's persistable URI, so we allow it
        // if the ID we're updating has the exact same filename.
        val sameNames =
            audioSegmentAdapter?.currentList
                ?.filter { it.descriptor == descriptor }
                ?.map { it.id }
                ?: emptyList()

        if (sameNames.isNotEmpty()) {
            if (selectAudioSegmentPendingId >= 0
                && sameNames.size == 1
                && sameNames[0] == selectAudioSegmentPendingId) {
                // That's ok because it's the same entry we're updating
                logger.d(TAG, "@@ addAudioFileUri: updated $selectAudioSegmentPendingId -> same descriptor '$descriptor'")
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Duplicate File Reference")
                    // Note: message does not specify that references encode the start/end
                    // timestamps as this cannot be selected in the UI yet. The message should be
                    // change if that feature is later added.
                    .setMessage("There is already an entry for a file named '$fileName'. Segments need to be unique. Please choose a different file or rename it first.")
                    .setPositiveButton("OK", null)
                    .show()
                selectAudioSegmentPendingId = -1L
                return
            }
        }

        // 5- Add to database or Update
        if (selectAudioSegmentPendingId >= 0) {
            viewModel.updatePersistableUri(
                selectAudioSegmentPendingId,
                descriptor,
                persistableUri)
            logger.d(TAG, "@@ addAudioFileUri: updated $selectAudioSegmentPendingId -> $persistableUri")
            notifyItemIdChanged(selectAudioSegmentPendingId)
            selectAudioSegmentPendingId = -1L
        } else {
            val newAudioSegment = AudioSegmentEntity(
                descriptor = descriptor,
                lastAccessTime = Instant.now().toString(),
                persistableUri = persistableUri,
                metadata = audioSegmentMetadata {
                    this.durationMs = durationMs
                }
            )
            viewModel.insert(newAudioSegment)
            logger.d(TAG, "@@ addAudioFileUri: added $newAudioSegment")
        }
    }

    private fun askAndMaybeDeleteEntry() {
        stopCurrentPlaying()
        val selected = audioSegmentAdapter?.getSelected() ?: return

        lifecycleScope.launch {
            val count = playTriggerDao.countTriggersForAudioSegment(selected.id)

            val msg = when {
                count > 0 -> "Are you sure you want to delete the reference to file '${selected.displayTitle()}'?\n" +
                        "WARNING: This will also delete $count ${count.pluralize("trigger", "triggers")} using this segment.\n" +
                        "This does not delete the actual audio file."
                else -> "Are you sure you want to delete the reference to file '${selected.displayTitle()}'?\n" +
                        "This does not delete the actual audio file."
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Delete Audio File Reference")
                .setMessage(msg)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteById(selected.id)
                    Snackbar.make(
                        requireView(),
                        "Reference to file '${selected.displayTitle()}' deleted",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /** Check all persistable Uri. Updates the list in-place, returns whether it has changed. */
    private fun validateAllPersistableUris(list: List<AudioSegmentItem>) : Boolean {
        var changed = false
        for (item in list) {
            val oldValid = item.uriValid
            item.uriValid = isPersistableUriValid(requireContext(), item.persistableUri.toUri())
            changed = changed || (item.uriValid != oldValid)
        }
        logger.d(TAG, "@@ validateAllPersistableUris: ${list.size} items --> changed $changed")
        return changed
    }

    /**
     * Checks if a persistable URI is still valid and readable.
     *
     * @param context The application context.
     * @param uri The persistable URI to check.
     * @return `true` if the URI is valid and readable, `false` otherwise.
     */
    private fun isPersistableUriValid(context: Context, uri: Uri): Boolean {
        val contentResolver = context.contentResolver

        // 1. Check if the app has persisted read permission for this URI.
        val persistedUris = contentResolver.persistedUriPermissions
        val hasPermission = persistedUris.any { it.uri == uri && it.isReadPermission }

        if (!hasPermission) {
            // No permission grant found for this URI.
            return false
        }

        // 2. Try to access the file's content to ensure it still exists.
        return try {
            // Attempt to open the file. If the file doesn't exist or is not accessible,
            // this will throw a FileNotFoundException.
            contentResolver.openInputStream(uri)?.use {
                // We don't need to read the stream, just successfully open it.
                // The 'use' block ensures it's closed immediately.
            }
            true // URI is valid and accessible.
        } catch (e: FileNotFoundException) {
            // The file is not found, which means the URI is stale.
            false
        } catch (e: SecurityException) {
            // This can happen in rare edge cases, e.g., if permissions were
            // just revoked in a strange way.
            false
        }
    }

}
