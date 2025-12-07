package com.alflabs.mpgpstrack.fragments.tracks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.mpgpstrack.databinding.TracksFragmentBinding
import com.alflabs.mpgpstrack.tracks.TrackEntity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TracksFragment : Fragment() {

    private var _binding: TracksFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TracksViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TracksFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TrackAdapter(
            onClick = { track ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Track Selected")
                    .setMessage(track.label)
                    .setPositiveButton("OK", null)
                    .show()
            },
            onEdit = { track ->
                showEditDialog(track)
            },
            onDelete = { track ->
                viewModel.delete(track)
            }
        )
        binding.tracksView.adapter = adapter
        binding.tracksView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.tracks.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        binding.addButton.setOnClickListener {
            viewModel.createRandomTrack()
        }
    }

    private fun showEditDialog(track: TrackEntity) {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
