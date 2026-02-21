package com.alflabs.milepost.fragments.gpsTracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.milepost.tracks.GpsTrackDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GpsTracksViewModel @Inject constructor(
    private val gpsTrackDao: GpsTrackDao
) : ViewModel() {

    val tracks = gpsTrackDao.getAllDisplayItems().asLiveData()

    suspend fun getIdByUuid(uuid: String): Long? {
        return gpsTrackDao.getIdByUuid(uuid)
    }

    /** Insert a GpsTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    fun insert(track: GpsTrackEntity) {
        viewModelScope.launch {
            gpsTrackDao.insert(track)
        }
    }

    /** Update a GpsTrackEntity with a matching id. */
    fun update(track: GpsTrackEntity) {
        viewModelScope.launch {
            gpsTrackDao.update(track)
        }
    }

    fun delete(trackItem: GpsTrackDisplayItem) {
        viewModelScope.launch {
            gpsTrackDao.deleteById(trackItem.id)
            println("@@ DB DELETE track = $trackItem")
        }
    }
}
