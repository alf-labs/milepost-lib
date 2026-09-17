package com.alflabs.milepostlib.utils

import android.content.Context
import android.net.Uri
import com.alflabs.milepostlib.db.DocentTrackEntity
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.db.exportProtobuf
import com.alflabs.milepostlib.fragments.docentTriggers.DocentTriggerItem
import com.alflabs.milepostlib.trigger.PlaybackTriggerPojo
import com.alflabs.milepostlib.db.importProtobuf
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

    /**
     * Export a Docent track as JSON.
     */
    fun createJsonExportFile(
        context: Context,
        docentTrackEntity: DocentTrackEntity?,
        gpsTrackEntity: GpsTrackEntity?,
        triggersList: List<DocentTriggerItem>,
    ): File? {
        logger.d(TAG, "createJsonExportFile")

        if (docentTrackEntity == null || gpsTrackEntity == null) {
            return null
        }

        // Creates a sorted list of unique audio file names used by all triggers.
        val uniqueDescriptors = triggersList.map { it.descriptor }.distinct().sorted()

        val exportedTriggers = triggersList.map { PlaybackTriggerPojo(it) }

        val exported = DocentImportExportData(
            docentTrack = docentTrackEntity,
            gpsTrackUuid = gpsTrackEntity.uuid,
            gpsTrackName = gpsTrackEntity.displayTrackLabel(),
            audioDescriptors = uniqueDescriptors,
            triggers = exportedTriggers,
        )

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "docent ${docentTrackEntity.uuid}${sanitizeTitle(docentTrackEntity.userTitle)}.json")
            logger.d(TAG, "@@ createJsonExportFile: file = ${file.path}")

            val objectMapper = ObjectMapper().exportProtobuf()
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
     * The validator can read from the database but not write to it.
     * The output of this function is a list of entities that need to be added.
     * Updating the database is the responsibility of the caller, and should be done
     * in a transaction.
     *
     * One thing this does NOT do is allow creating a docent track where the GPS track or the
     * audio files are unresolved, as the prototype's UI currently lacks support for that.
     */
    suspend fun loadDocentTrackEntityFromUri(
        context: Context,
        uri: Uri,
        validator: suspend (DocentImportExportData) -> DocentImportValidationResult,
    ): DocentImportResult? {
        logger.d(TAG, "loadDocentTrackEntityFromUri")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()

                val (data, validated) = bytes.inputStream().use { bytesStream ->
                    val objectMapper = ObjectMapper().importProtobuf()
                    val data = objectMapper.readValue(
                        bytesStream,
                        DocentImportExportData::class.java)

                    Pair(data, validator(data))
                }

                return if (validated.canContinue) {
                    DocentImportResult(
                        validatorResult = validated,
                        docentTrackEntity = data.toEntity(validated),
                        triggerItems = data.triggers.map {
                            it.toItem(
                                docentTrackId = 0,
                                descriptorsToId = validated.descriptorsToId,
                                descriptorsToUri = validated.descriptorsToUri,
                            )
                        }
                    )
                } else {
                    null
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
data class DocentImportExportData @JsonCreator constructor(
    @param:JsonProperty("docentTrack") val docentTrack: DocentTrackEntity,
    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String,
    @param:JsonProperty("gpsTrackName") val gpsTrackName: String,
    @param:JsonProperty("audioDescriptors") val audioDescriptors: List<String>,
    @param:JsonProperty("triggers") val triggers: List<PlaybackTriggerPojo>,
) {
    fun toEntity(data: DocentImportValidationResult) = docentTrack.copy(
        id = 0,
        userTitle = data.docentUserTitle.ifBlank { docentTrack.userTitle ?: "" },
        gpsTrackId = data.gpsTrackId
    )
}

//// TBD we may not need this
//private class DocentTrackExportInfoDeserializer : StdDeserializer<DocentTrackExportInfo>(DocentTrackExportInfo::class.java) {
//    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DocentTrackExportInfo {
//        val node: JsonNode = p.codec.readTree(p)
//        return DocentTrackExportInfo(
//            docentTrack = ctxt.readTreeAsValue(node.get("docentTrack"), DocentTrackEntity::class.java),
//            gpsTrackUuid = node.get("gpsTrackUuid").asText(),
//            gpsTrackName = node.get("gpsTrackName").asText(),
//            audioDescriptors = ctxt.readTreeAsValue(node.get("audioDescriptors"), ctxt.typeFactory.constructCollectionType(List::class.java, String::class.java)),
//            triggers = ctxt.readTreeAsValue(node.get("triggers"), ctxt.typeFactory.constructCollectionType(List::class.java, PlaybackTriggerPojo::class.java))
//        )
//    }
//}

//data class DocentImportValidationData @JsonCreator constructor(
//    @param:JsonProperty("docentTrack") val docentTrack: DocentTrackEntity,
////    @param:JsonProperty("docentTrackUuid") val docentTrackUuid: String,
////    @param:JsonProperty("userTitle") val userTitle: String,
//    @param:JsonProperty("gpsTrackUuid") val gpsTrackUuid: String,
//    @param:JsonProperty("gpsTrackName") val gpsTrackName: String,
//    @param:JsonProperty("audioDescriptors") val audioDescriptors: List<String>,
//)

//// TBD we may not need this
//private class DocentImportValidationDataDeserializer : StdDeserializer<DocentImportValidationData>(DocentImportValidationData::class.java) {
//    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DocentImportValidationData {
//        val node: JsonNode = p.codec.readTree(p)
//        val docentTrack = node.get("docentTrack")
//        return DocentImportValidationData(
//            docentTrackUuid = docentTrack.get("uuid").asText(),
//            userTitle = if (docentTrack.hasNonNull("userTitle")) docentTrack.get("userTitle").asText() else "",
//            gpsTrackUuid = node.get("gpsTrackUuid").asText(),
//            gpsTrackName = node.get("gpsTrackName").asText(),
//            audioDescriptors = ctxt.readTreeAsValue(node.get("audioDescriptors"), ctxt.typeFactory.constructCollectionType(List::class.java, String::class.java))
//        )
//    }
//}

data class DocentImportValidationResult(
    val canContinue: Boolean,
    val message: String,
    val replace: Boolean = false,
    /** Existing track to be replaced */
    val docentTrackId: Long = 0,
    /** Override imported title, or empty to use existing. */
    val docentUserTitle: String = "",
    /** Existing gps track to use. */
    val gpsTrackId: Long = 0,
    val descriptorsToId: Map<String, Long> = emptyMap(),
    val descriptorsToUri: Map<String, String> = emptyMap(),
)

data class DocentImportResult(
    val validatorResult: DocentImportValidationResult,
    val docentTrackEntity: DocentTrackEntity,
    val triggerItems: List<DocentTriggerItem>,
)
