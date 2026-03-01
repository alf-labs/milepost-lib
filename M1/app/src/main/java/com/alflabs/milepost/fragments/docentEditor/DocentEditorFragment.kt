package com.alflabs.milepost.fragments.docentEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.DocentEditorFragmentBinding
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackDao
import com.alflabs.utils.ILogger
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class DocentEditorFragment : Fragment() {

    companion object {
        private val TAG = DocentEditorFragment::class.java.simpleName
        private const val KEY_SELECTED_POSITION = "selected-position"
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var gpsTrackDao: GpsTrackDao
    @Inject lateinit var docentTrackDao: DocentTrackDao

    private var _binding: DocentEditorFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: DocentEditorFragmentArgs by navArgs()
    private var docentEditorAdapter: DocentEditorAdapter? = null
    private val viewModel: DocentEditorViewModel by viewModels()
    private var docentTrackEntity: DocentTrackEntity? = null
    private var firstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DocentEditorFragmentBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "@@ onViewCreated")

        lifecycleScope.launch {
            docentEditorAdapter = DocentEditorAdapter(
//                onPlay = ::onPlayStopItem,
//                onUpdate = ::onUpdateAudioDocumentUri,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.playTriggerList.adapter = docentEditorAdapter
            binding.playTriggerList.layoutManager = layoutManager

            savedInstanceState?.let {
                val selectedPosition = it.getInt(KEY_SELECTED_POSITION, RecyclerView.NO_POSITION)
                docentEditorAdapter?.setSelectedPosition(selectedPosition)
            }

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.playTriggerList.addItemDecoration(dividerItemDecoration)

            viewModel.docentTrack.observe(viewLifecycleOwner) { docentTrack ->
                logger.d(TAG, "@@ DocentEdit observe new DOCENT docentTrack: ${docentTrack?.userTitle}")
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
                logger.d(TAG, "@@ DocentEdit observe new GPS track: ${gpsTrack?.trackLabel}")
                val label = gpsTrack?.displayTrackLabel()
                binding.gpsTrackLabel.text = if (label != null) "GPS Track: $label" else "GPS Track not found"
            }

            viewModel.triggers.observe(viewLifecycleOwner) { list ->
                logger.d(TAG, "@@ DocentEdit observe new list: ${list?.size ?: -1} items")

                binding.emptyMessage.visibility =
                    if (list?.isEmpty() ?: false) View.VISIBLE else View.GONE

//                validateAllPersistableUris(list)
                docentEditorAdapter?.submitList(list)
            }

            binding.userTitle.setOnClickListener {
                showReviewTitleDialog(docentTrackEntity)
            }

            viewModel.loadTrack(fragmentArgs.docentTrackId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        docentEditorAdapter?.let { adapter ->
            logger.d(TAG, "@@ onSaveInstanceState: ${adapter.selectedPosition}")
            outState.putInt(KEY_SELECTED_POSITION, adapter.selectedPosition)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun showReviewTitleDialog(track: DocentTrackEntity?) {
        if (track == null) return

        val dialogView = layoutInflater.inflate(R.layout.docent_editor_title_dialog, null)
        val trackTitleEditText = dialogView.findViewById<TextInputEditText>(R.id.track_title_edittext)

        trackTitleEditText.setText(track.displayUserTitle())

        AlertDialog.Builder(requireContext())
            .setTitle("Review Options")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val newTitle = trackTitleEditText.text.toString().trim()

                logger.d(TAG, "@@Apply TITLE clicked:")
                logger.d(TAG, "@@  Title: $newTitle")


                if (newTitle != track.userTitle) {
                    val newGpsTrackEntity = track.copy(userTitle = newTitle)
                    lifecycleScope.launch {
                        docentTrackDao.update(newGpsTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
