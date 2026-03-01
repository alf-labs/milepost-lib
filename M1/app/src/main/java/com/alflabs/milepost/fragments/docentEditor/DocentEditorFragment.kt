package com.alflabs.milepost.fragments.docentEditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.databinding.DocentEditorFragmentBinding
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class DocentEditorFragment : Fragment() {

    companion object {
        private val TAG = DocentEditorFragment::class.java.simpleName
        private const val KEY_SELECTED_POSITION = "selected-position"
        private const val ARG_DOCENT_TRACK_ID = "docentTrackId"
    }

    @Inject lateinit var logger: ILogger

    private var _binding: DocentEditorFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: DocentEditorFragmentArgs by navArgs()
    private var docentEditAdapter: DocentEditAdapter? = null
    private val viewModel: DocentEditorViewModel by viewModels()

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
            docentEditAdapter = DocentEditAdapter(
//                onPlay = ::onPlayStopItem,
//                onUpdate = ::onUpdateAudioDocumentUri,
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.playTriggerList.adapter = docentEditAdapter
            binding.playTriggerList.layoutManager = layoutManager

            savedInstanceState?.let {
                val selectedPosition = it.getInt(KEY_SELECTED_POSITION, RecyclerView.NO_POSITION)
                docentEditAdapter?.setSelectedPosition(selectedPosition)
            }

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.playTriggerList.addItemDecoration(dividerItemDecoration)

            viewModel.track.observe(viewLifecycleOwner) { track ->
                if (track == null) {
                    binding.label.text = "No track found"
                } else {
                    binding.label.text = track.userTitle?.ifBlank { "(Tap to Add Title)" }
                }
            }

            viewModel.items.observe(viewLifecycleOwner) { list ->

                binding.emptyMessage.visibility =
                    if (list?.isEmpty() ?: false) View.VISIBLE else View.GONE
                logger.d(TAG, "@@ DocentEdit observe new list: ${list?.size ?: -1} items")
//                validateAllPersistableUris(list)
                docentEditAdapter?.submitList(list)
            }

            viewModel.loadTrack(fragmentArgs.docentTrackId)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        docentEditAdapter?.let { adapter ->
            logger.d(TAG, "@@ onSaveInstanceState: ${adapter.selectedPosition}")
            outState.putInt(KEY_SELECTED_POSITION, adapter.selectedPosition)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
