package com.alflabs.milepost.fragments.audioFiles

import android.content.Context
import android.content.Intent
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
import java.io.FileNotFoundException
import java.time.Instant

@AndroidEntryPoint
class AudioFilesFragment : Fragment() {

    private var _binding: AudioFilesFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AudioFilesViewModel by viewModels()

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
                        selectAudioFile()
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

    private fun selectAudioFile() {
        // 1- Ask user to select an audio file using the Storage Access Framework (SAF).
        // Use a ActivityResultContracts call suitable to return a Persistable URI
        // that can be stored in the database to open the file much later.
        selectAudioFileLauncher.launch(arrayOf("audio/*"))
    }

    private fun addAudioFileUri(uri: Uri) {
        // 2- Get a persistable uri for the selected file.
        val persistableUri = uri.toString()

        // 3- Get the filename of the selected file.
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        val fileName = cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    it.getString(displayNameIndex)
                } else {
                    "Unknown"
                }
            } else {
                "Unknown"
            }
        } ?: "Unknown"

        // 4- Add to database
        val newAudioFile = AudioFileEntity(
            fileName = fileName,
            lastAccessTime = Instant.now().toString(),
            persistableUri = persistableUri
        )
        viewModel.insert(newAudioFile)
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

    /**
     * Checks if a persistable URI is still valid and readable.
     *
     * @param context The application context.
     * @param uri The persistable URI to check.
     * @return `true` if the URI is valid and readable, `false` otherwise.
     */
    fun isPersistableUriValid(context: Context, uri: Uri): Boolean {
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
