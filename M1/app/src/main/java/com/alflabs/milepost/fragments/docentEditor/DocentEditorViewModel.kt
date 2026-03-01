package com.alflabs.milepost.fragments.docentEditor

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.PlayTriggerDao
import com.alflabs.milepost.db.PlayTriggerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocentEditorViewModel @Inject constructor(
    private val docentTrackDao: DocentTrackDao,
    private val playTriggerDao: PlayTriggerDao,
) : ViewModel() {

    companion object {
        private val TAG = DocentEditorViewModel::class.java.simpleName
    }

    private val _docentTrackId = MutableLiveData<Long>()

    val track: LiveData<DocentTrackEntity?> = _docentTrackId.switchMap { id ->
        docentTrackDao.getById(id).asLiveData()
    }

    val items: LiveData<List<DocentEditorItem>?> = _docentTrackId.switchMap { id ->
        playTriggerDao.getTriggersForDocentTrack(id).asLiveData()
    }

    fun loadTrack(docentTrackId: Long) {
        Log.d(TAG, "Edit load track by id #$docentTrackId")
        _docentTrackId.value = docentTrackId
    }

    fun insert(playTrigger: PlayTriggerEntity) {
        viewModelScope.launch {
            playTriggerDao.insert(playTrigger)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            playTriggerDao.deleteById(id)
        }
    }
}
