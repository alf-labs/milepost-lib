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

    val tracks = docentTrackDao.getAll().asLiveData()

    suspend fun getIdByUuid(uuid: String): Long? {
        return docentTrackDao.getIdByUuid(uuid)
    }

    /** Insert a DocentTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    fun insert(track: DocentTrackEntity) {
        viewModelScope.launch {
            docentTrackDao.insert(track)
        }
    }

    /** Update a DocentTrackEntity with a matching id. */
    fun update(track: DocentTrackEntity) {
        viewModelScope.launch {
            docentTrackDao.update(track)
        }
    }

    fun delete(trackInfo: DocentTrackEntity) {
        viewModelScope.launch {
            docentTrackDao.deleteById(trackInfo.id)
            println("@@ DB DELETE track = $trackInfo")
        }
    }
}
