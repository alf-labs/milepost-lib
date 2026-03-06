package com.alflabs.milepost.utils

import android.content.Context
import android.net.Uri
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import com.alflabs.milepost.trigger.PlaybackTriggerPojo
import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
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

    /**
     * Export a Docent track as JSON.
     */
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
            docentTrackUuid = docentTrackEntity.uuid,
            userTitle = docentTrackEntity.userTitle,
            createdDateISO = docentTrackEntity.creationTime,
            lastUsedDateISO = docentTrackEntity.lastUsedTime,
            gpsTrackUuid = gpsTrackEntity.uuid,
            gpsTrackName = gpsTrackEntity.displayTrackLabel(),
            audioFileNames = uniqueAudioFileNames,
            triggers = exportedTriggers,
        )

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "docent ${exported.docentTrackUuid}${sanitizeTitle(docentTrackEntity.userTitle)}.json")

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

    /**
     * Import a Docent track from JSON.
     *
     * This is a 3-step process:
     * - First, parse the JSON patially. We just want the docent track UUID, the gps track UUID,
     *   and the audio filenames needed.
     * - Second, through a callback, we validate whether we should continue:
     *   - The docent track UUID can be used to know if this track already exists.
     *   - The GPS track UUID + name can be used to find whether the GPS track already exists.
     *     We would abort the import if it doesn't, otherwise we get the gps track ID to use.
     *   - Similarly, we can check whether all the audio files exists, and either get their IDs
     *     or abort if they do not exist.
     * - Third, using the IDs collected from the callback, we can create entities for the
     *   docent track and the triggers.
     *
     * Note that this function makes no database access.
     * That's the responsibility of the caller, and should be done in a transaction.
     *
     * One thing this does NOT do is allow creating a docent track where the GPS track or the
     * audio files are unresolved, as the prototype's UI currently lacks support for that.
     */
    fun loadDocentTrackEntityFromUri(
        context: Context,
        uri: Uri,
        validator: (DocentImportValidationData) -> DocentImportValidationResult,
    ): DocentImportResult? {
        logger.d(TAG, "loadDocentTrackEntityFromUri")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readAllBytes()

                val validated = bytes.inputStream().use { bytesStream ->
                    // This mapper is special and must ignore a bunch of fields not present
                    // in the destination POJO.
                    val objectMapper = ObjectMapper()
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    val validation = objectMapper.readValue(
                        bytesStream,
                        DocentImportValidationData::class.java)

                    validator(validation)
                }

                if (validated.canContinue) {
                    bytes.inputStream().use { bytesStream ->
                        val objectMapper = ObjectMapper()
                        val entity = objectMapper.readValue(
                            bytesStream,
                            DocentTrackEntityPojo::class.java)

                        // TBD convert triggers pojo
                        return DocentImportResult(
                            docentTrackEntity = entity.toProto(validated),
                            triggerItems = entity.triggers.map {
                                it.toItem(
                                    docentTrackId = 0,
                                    audioFileNameToId = validated.audioFileNamesToId)
                            }
                        )
                    }
                } else {
                    return null
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "loadDocentTrackEntityFromUri: error", e)
            null
        }
    }
}


/** A mirror of [DocentTrackEntity] for JSON import/export. */
@JsonInclude(JsonInclude.Include.NON_NULL)
private data class DocentTrackEntityPojo @JsonCreator constructor(
    @param:JsonProperty("docentTrackUuid") val docentTrackUuid: String,
    @param:JsonProperty("userTitle") val userTitle: String? = null,
    @param:JsonProperty("createdDateISO") val createdDateISO: String,
    @param:JsonProperty("lastUsedDateISO") val lastUsedDateISO: String? = null,
    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String,
    @param:JsonProperty("gpsTrackName") val gpsTrackName: String,
    @param:JsonProperty("audioFileNames") val audioFileNames: List<String>,
    @param:JsonProperty("triggers") val triggers: List<PlaybackTriggerPojo>,
) {
    fun toProto(data: DocentImportValidationResult) = DocentTrackEntity(
        id = 0,
        uuid = docentTrackUuid,
        userTitle = userTitle,
        creationTime = createdDateISO,
        lastUsedTime = lastUsedDateISO,
        gpsTrackId = data.gpsTrackId
    )
}

data class DocentImportValidationData @JsonCreator constructor(
    @param:JsonProperty("docentTrackUuid") val docentTrackUuid: String,
    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String,
    @param:JsonProperty("gpsTrackName") val gpsTrackName: String,
    @param:JsonProperty("audioFileNames") val audioFileNames: List<String>,
)

data class DocentImportValidationResult(
    val canContinue: Boolean,
    val message: String,
    val gpsTrackId: Long,
    val audioFileNamesToId: Map<String, Long>,
)

data class DocentImportResult(
    val docentTrackEntity: DocentTrackEntity,
    val triggerItems: List<DocentEditorItem>,
)
