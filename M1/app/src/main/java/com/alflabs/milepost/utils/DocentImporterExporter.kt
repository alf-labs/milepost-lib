package com.alflabs.milepost.utils

import android.content.Context
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackDao
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import com.alflabs.milepost.trigger.PlaybackTriggerPojo
import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocentImporterExporter @Inject constructor(
    override val logger: ILogger,
    private val fileOps: FileOps,
) : ImporterExporterCommon(logger) {

    companion object {
        private val TAG = DocentImporterExporter::class.java.simpleName
    }

    fun createJsonExportFile(
        context: Context,
        docentTrackEntity: DocentTrackEntity?,
        gpsTrackEntity: GpsTrackEntity?,
        triggersList: List<DocentEditorItem>,
    ): File? {
        logger.d(TAG, "createJsonExportFile")

        if (docentTrackEntity == null || gpsTrackEntity == null) {
            return null
        }

        // Creates a sorted list of unique audio file names used by all triggers.
        val uniqueAudioFileNames = triggersList.map { it.fileName }.distinct().sorted()

        val exportedTriggers = triggersList.map { PlaybackTriggerPojo(it) }

        val exported = DocentTrackEntityPojo(
            uuid = docentTrackEntity.uuid,
            userTitle = docentTrackEntity.userTitle,
            createdISO = docentTrackEntity.creationTime,
            lastUsedISO = docentTrackEntity.lastUsedTime,
            gpsTrackUuid = gpsTrackEntity.uuid,
            gpsTrackName = gpsTrackEntity.displayTrackLabel(),
            audioFileNames = uniqueAudioFileNames,
            triggers = exportedTriggers,
        )

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "docent ${exported.uuid}${sanitizeTitle(docentTrackEntity.userTitle)}.json")

            val objectMapper = ObjectMapper()
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            fileOps.openFileWriter(file, /*append=*/false).use { writer ->
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, exported)
            }

            file // Return the created file
        } catch (e: Exception) {
            // Handle file creation error
            logger.d(TAG, "createJsonExportFile: error", e)
            null
        }
    }
}


/** A mirror of [DocentTrackEntity] for JSON import/export. */
private data class DocentTrackEntityPojo @JsonCreator constructor(
    @param:JsonProperty("uuid") val uuid: String,
    @param:JsonProperty("userTitle") val userTitle: String? = null,
    @param:JsonProperty("createdISO") val createdISO: String,
    @param:JsonProperty("lastUsedISO") val lastUsedISO: String? = null,
    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String,
    @param:JsonProperty("gpsTrackName") val gpsTrackName: String,
    @param:JsonProperty("audioFileNames") val audioFileNames: List<String>,
    @param:JsonProperty("triggers") val triggers: List<PlaybackTriggerPojo>,
) {
    suspend fun toProto(gpsTrackDao: GpsTrackDao) = DocentTrackEntity(
        id = 0,
        uuid = uuid,
        userTitle = userTitle,
        creationTime = createdISO,
        lastUsedTime = lastUsedISO,
        gpsTrackId = gpsTrackDao.getIdByUuid(gpsTrackUuid)!!
    )
}

