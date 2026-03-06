package com.alflabs.milepost.utils

import android.content.Context
import android.net.Uri
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.db.GpsTrackEntityPojo
import com.alflabs.utils.FileOps
import com.alflabs.utils.ILogger
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Import/Export of Tracks.
 *
 * The export has 2 purposes:
 * 1- Easy to import in Google My Maps.
 * 2- As a format we can latter re-import without losing information as a backup file.
 *
 * For Google My Maps, we mostly care about GPS coordinate, time (optional), and MP value
 * (also optional), and a CSV file makes the most sense here.
 * As a backup purpose, we also want some extra metadata such as track label, uuid, created
 * vs update dates, etc., and a JSON file is better suited.
 *
 * Google My Maps supported formats:
 * - https://support.google.com/mymaps/answer/3024836
 * with a friendly description here including some CSV examples:
 * - https://www.google.com/earth/outreach/learn/visualize-your-data-on-a-custom-map-using-google-my-maps/
 */
@Singleton
class GpsImporterExporter @Inject constructor(
    logger: ILogger,
    val fileOps: FileOps,
) : ImporterExporterCommon(logger) {

    companion object {
        private val TAG = GpsImporterExporter::class.java.simpleName
    }

    private fun sanitizeTitle(track: GpsTrackEntity): String {
        return sanitizeTitle(track.trackLabel)
    }

    /**
     * Export a GPS track as CSV for Google My Maps.
    */
    fun createCsvExportFile(context: Context, gpsTrackEntity: GpsTrackEntity?): File? {
        logger.d(TAG, "createCsvExportFile")
        val csvHeader = "isoDateTime,latitude,longitude,speed_mph,mp\n"

        if (gpsTrackEntity == null) {
            return null
        }
        val locationList = gpsTrackEntity.locationList ?: return null

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "gps ${gpsTrackEntity.uuid}${sanitizeTitle(gpsTrackEntity)}.csv")

            val formatter = DateTimeFormatter.ISO_INSTANT
            fileOps.openFileWriter(file, /*append=*/false).use { writer ->
                writer.write(csvHeader)
                locationList.locationsList.forEach { data ->
                    val ll = data.latLong
                    val sb = if (data.hasSpeed()) data.speed else null
                    val speed = if (sb != null) Units.mps_to_mph(sb.speedMps) else ""
                    val mp = if (data.hasMp()) data.mp.toString() else ""
                    val instant = Instant.ofEpochMilli(ll.timestampMillis)
                    val isoDT = formatter.format(instant)

                    val line = "${isoDT},${ll.latitude},${ll.longitude},${speed},${mp}\n"
                    writer.write(line)
                }
            }
            file // Return the created file
        } catch (e: Exception) {
            // Handle file creation error
            logger.d(TAG, "createCsvExportFile: error", e)
            null
        }
    }

    /**
     * Export a GPS track as JSON.
     */
    fun createJsonExportFile(context: Context, gpsTrackEntity: GpsTrackEntity?): File? {
        logger.d(TAG, "createJsonExportFile")

        if (gpsTrackEntity == null) {
            return null
        }

        val exported = gpsTrackEntity.toPojo()

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "gps ${exported.uuid}${sanitizeTitle(gpsTrackEntity)}.json")

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
     * Import a GPS track from JSON.
     */
    fun loadGpsTrackEntityFromUri(context: Context, uri: Uri): GpsTrackEntity? {
        logger.d(TAG, "loadTrackEntityFromUri")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val objectMapper = ObjectMapper()
                val imported = objectMapper.readValue(inputStream, GpsTrackEntityPojo::class.java)
                return imported.toProto()
            }
        } catch (e: Exception) {
            logger.d(TAG, "loadTrackEntityFromUri: error", e)
            null
        }
    }
}
