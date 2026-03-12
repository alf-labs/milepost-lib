package com.alflabs.milepost.fragments.audioSegments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.db.AudioSegmentDao
import com.alflabs.milepost.db.AudioSegmentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioSegmentViewModel @Inject constructor(
    private val audioSegmentDao: AudioSegmentDao
) : ViewModel() {

    val items = audioSegmentDao.getItems().asLiveData()

    fun insert(audioFile: AudioSegmentEntity) {
        viewModelScope.launch {
            audioSegmentDao.insert(audioFile)
        }
    }

    fun updatePersistableUri(id: Long, fileName: String, persistableUri: String) {
        viewModelScope.launch {
            audioSegmentDao.updatePersistableUri(id, fileName, persistableUri)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            audioSegmentDao.deleteById(id)
        }
    }
}
