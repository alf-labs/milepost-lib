package com.alflabs.milepost.fragments.docentTracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.DocentTrackDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocentTracksViewModel @Inject constructor(
    private val docentTrackDao: DocentTrackDao
) : ViewModel() {

    val tracks = docentTrackDao.getItems().asLiveData()

    suspend fun getIdByUuid(uuid: String): Long? {
        return docentTrackDao.getIdByUuid(uuid)
    }

    /** Insert a DocentTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    fun insert(track: DocentTrackEntity) {
        viewModelScope.launch {
            docentTrackDao.insert(track)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            docentTrackDao.deleteById(id)
            println("@@ DB DELETE track = $id")
        }
    }
}
