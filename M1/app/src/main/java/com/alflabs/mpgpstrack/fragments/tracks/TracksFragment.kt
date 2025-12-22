package com.alflabs.mpgpstrack.fragments.tracks

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.mpgpstrack.databinding.TracksFragmentBinding
import com.alflabs.mpgpstrack.main.AppSettings
import com.alflabs.mpgpstrack.tracks.TrackDisplayInfo
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TracksFragment : Fragment() {

    companion object {
        private val TAG = TracksFragment::class.java.simpleName
    }

    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var logger: ILogger

    private var _binding: TracksFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TracksViewModel by viewModels()
    private var useImperialUnits = false

    private val selectJsonFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { importJsonLoadUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TracksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        useImperialUnits = appSettings.useImperialUnits(lifecycleScope)

        val adapter = TrackAdapter(
            onClick = { track ->
                showEditDialog(track)
                /*
                AlertDialog.Builder(requireContext())
                    .setTitle("Track Selected")
                    .setMessage(track.label)
                    .setPositiveButton("OK", null)
                    .show()
                 */
            },
            onEdit = { track ->
                showEditDialog(track)
            },
            onDelete = { track ->
                viewModel.delete(track)
            },
            useImperialUnits,
        )
        val layoutManager = LinearLayoutManager(requireContext())
        binding.tracksList.adapter = adapter
        binding.tracksList.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            layoutManager.orientation
        )
        binding.tracksList.addItemDecoration(dividerItemDecoration)

        viewModel.tracks.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.importButton.setOnClickListener {
            importJsonSelectFile()
        }
    }

    private fun importJsonSelectFile() {
        logger.d(TAG, "@@ ImportJSON 1 - Select File")
        // 1- Ask user to select a JSON file using the Storage Access Framework (SAF).
        selectJsonFileLauncher.launch("application/json")
    }

    private fun importJsonLoadUri(uri: Uri) {
        // 2- When a file is selected, load it in memory using the Jackson library for TrackEntity.class
        // 3- If the TrackEntity.id already exists in the database, ask the user if they want to override it or create a new record.
        // 4- Either update the database (same id) or insert a new record (new id).
        logger.d(TAG, "@@ ImportJSON 2 - Load URI: $uri")
    }

    private fun showEditDialog(track: TrackDisplayInfo) {
        findNavController().navigate(
            TracksFragmentDirections.actionListToReview(track.id)
        )

        // Quick kludge to show an inline edit dialog for one property
        /*
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val nameInput = EditText(context).apply {
                setText(track.label)
                hint = "Label"
            }
            addView(nameInput)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Track")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newlabel = (layout.getChildAt(0) as EditText).text.toString()
                viewModel.updateTrack(track.copy(label = newlabel))
            }
            .setNegativeButton("Cancel", null)
            .show()
         */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
