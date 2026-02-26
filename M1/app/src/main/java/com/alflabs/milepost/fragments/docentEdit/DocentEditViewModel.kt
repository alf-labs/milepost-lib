package com.alflabs.milepost.fragments.docentEdit

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.db.PlayTriggerDao
import com.alflabs.milepost.db.PlayTriggerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocentEditViewModel @Inject constructor(
    private val playTriggerDao: PlayTriggerDao
) : ViewModel() {

    var itemsDocentTrackId: Long = -1
    var items: LiveData<List<DocentEditItem>>? = null

    fun getItems(docentTrackId: Long): LiveData<List<DocentEditItem>> {
        if (itemsDocentTrackId != docentTrackId) {
            itemsDocentTrackId = docentTrackId
            items = playTriggerDao.getTriggersForDocentTrack(docentTrackId).asLiveData()
        }
        return items!!
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
