package com.alflabs.milepost.fragments.audioFiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.tracks.AudioFileDao
import com.alflabs.milepost.tracks.AudioFileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioFilesViewModel @Inject constructor(
    private val audioFileDao: AudioFileDao
) : ViewModel() {

    val items = audioFileDao.getItems().asLiveData()

    fun insert(audioFile: AudioFileEntity) {
        viewModelScope.launch {
            audioFileDao.insert(audioFile)
        }
    }

    fun updatePersistableUri(id: Long, fileName: String, persistableUri: String) {
        viewModelScope.launch {
            audioFileDao.updatePersistableUri(id, fileName, persistableUri)
        }
    }

    fun delete(item: AudioFileItem) {
        viewModelScope.launch {
            audioFileDao.deleteById(item.id)
        }
    }
}
