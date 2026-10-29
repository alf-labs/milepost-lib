package com.alflabs.milepostlib.fragments.docentTriggers

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.alflabs.milepostlib.db.AudioSegmentDao
import com.alflabs.milepostlib.db.DocentTrackDao
import com.alflabs.milepostlib.db.DocentTrackEntity
import com.alflabs.milepostlib.db.GpsTrackDao
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.db.PlayTriggerDao
import com.alflabs.milepostlib.db.PlayTriggerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocentTriggersViewModel @Inject constructor(
    private val docentTrackDao: DocentTrackDao,
    private val playTriggerDao: PlayTriggerDao,
    private val audioSegmentDao: AudioSegmentDao,
    private val gpsTrackDao: GpsTrackDao,
) : ViewModel() {

    companion object {
        private val TAG = DocentTriggersViewModel::class.java.simpleName
    }

    private val _docentTrackId = MutableLiveData<Long>()

    val docentTrack: LiveData<DocentTrackEntity?> = _docentTrackId.switchMap { id ->
        docentTrackDao.getById(id).asLiveData()
    }

    val triggers: LiveData<List<DocentTriggerItem>?> = _docentTrackId.switchMap { id ->
        playTriggerDao.getTriggersForDocentTrack(id).asLiveData()
    }

    val gpsTrack: LiveData<GpsTrackEntity?> = _docentTrackId.switchMap { id ->
        gpsTrackDao.getGpsTrackForDocentTrackId(id).asLiveData()
    }

    val audioSegments = audioSegmentDao.getItems().asLiveData()

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
