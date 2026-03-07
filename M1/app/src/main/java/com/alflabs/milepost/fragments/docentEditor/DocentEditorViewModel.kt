package com.alflabs.milepost.fragments.docentEditor

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.db.AudioFileDao
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackDao
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.db.PlayTriggerDao
import com.alflabs.milepost.db.PlayTriggerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocentEditorViewModel @Inject constructor(
    private val docentTrackDao: DocentTrackDao,
    private val playTriggerDao: PlayTriggerDao,
    private val audioFileDao: AudioFileDao,
    private val gpsTrackDao: GpsTrackDao,
) : ViewModel() {

    companion object {
        private val TAG = DocentEditorViewModel::class.java.simpleName
    }

    private val _docentTrackId = MutableLiveData<Long>()

    val docentTrack: LiveData<DocentTrackEntity?> = _docentTrackId.switchMap { id ->
        docentTrackDao.getById(id).asLiveData()
    }

    val triggers: LiveData<List<DocentEditorItem>?> = _docentTrackId.switchMap { id ->
        playTriggerDao.getTriggersForDocentTrack(id).asLiveData()
    }

    val gpsTrack: LiveData<GpsTrackEntity?> = _docentTrackId.switchMap { id ->
        gpsTrackDao.getGpsTrackForDocentTrackId(id).asLiveData()
    }

    val audioFiles = audioFileDao.getItems().asLiveData()

    fun loadDocentTrack(docentTrackId: Long) {
        Log.d(TAG, "Edit load track by id #$docentTrackId")
        _docentTrackId.value = docentTrackId
    }

    fun insertPlayTrigger(playTrigger: PlayTriggerEntity) {
        viewModelScope.launch {
            playTriggerDao.insert(playTrigger)
        }
    }

    fun deletePlayTriggerById(id: Long) {
        viewModelScope.launch {
            playTriggerDao.deleteById(id)
        }
    }

    fun updatePlayTrigger(playTrigger: PlayTriggerEntity) {
        viewModelScope.launch {
            playTriggerDao.update(playTrigger)
        }
    }

    fun updateDocentTrack(docentTrack: DocentTrackEntity) {
        viewModelScope.launch {
            docentTrackDao.update(docentTrack)
        }
    }
}
