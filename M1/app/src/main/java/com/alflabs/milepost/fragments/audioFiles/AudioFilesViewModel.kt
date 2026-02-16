package com.alflabs.milepost.fragments.audioFiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.tracks.AudioFileDao
import com.alflabs.milepost.tracks.AudioFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioFilesViewModel @Inject constructor(
    private val audioFileDao: AudioFileDao
) : ViewModel() {

    val audioFiles = audioFileDao.getAll().asLiveData()

    fun addAudioFile(audioFile: AudioFileEntity) {
        viewModelScope.launch {
            audioFileDao.insert(audioFile)
        }
    }

    fun deleteAudioFile(audioFile: AudioFileEntity) {
        viewModelScope.launch {
            audioFileDao.deleteById(audioFile.id)
        }
    }
}
