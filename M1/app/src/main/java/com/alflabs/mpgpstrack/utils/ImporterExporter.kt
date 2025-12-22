package com.alflabs.mpgpstrack.utils

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.core.content.FileProvider
import com.alflabs.mpgpstrack.location.LatLongData
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.TrackEntityPojo
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
class ImporterExporter @Inject constructor(
    val logger: ILogger,
    val fileOps: FileOps,
) {

    companion object {
        private val TAG = ImporterExporter::class.java.simpleName
    }

    /**
     * Export a GPS track as CSV for Google My Maps.
    */
    fun createCsvExportFile(context: Context, trackEntity: TrackEntity?): File? {
        logger.d(TAG, "createCsvExportFile")
        val csvHeader = "isoDateTime,latitude,longitude,speed_mph,mp\n"

        if (trackEntity == null) {
            return null
        }
        val locationList = trackEntity.locationList ?: return null

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "track_${trackEntity.uuid}.csv")

            val formatter = DateTimeFormatter.ISO_INSTANT
            val distResults = FloatArray(1)

            fileOps.openFileWriter(file, /*append=*/false).use { writer ->
                writer.write(csvHeader)
                var lastLatLong: LatLongData? = null
                var lastMp = 0f
                locationList.locationsList.forEach { data ->
                    val ll = data.latLong
                    val sb = if (data.hasSpeedBearing()) data.speedBearing else null
                    val speed = if (sb != null) Units.mps_to_mph(sb.speedMps) else ""
                    val mp = if (data.hasMp()) {
                        lastMp = data.mp
                        data.mp.toString()
                    } else {
                        lastLatLong?.let {
                            Location.distanceBetween(
                                it.latitude, it.longitude,
                                ll.latitude, ll.longitude,
                                distResults)
                            lastMp += Units.m_to_miles(distResults[0])
                        }
                        lastMp.toString()
                    }

                    val instant = Instant.ofEpochMilli(ll.timestampMillis)
                    val isoDT = formatter.format(instant)

                    val line = "${isoDT},${ll.latitude},${ll.longitude},${speed},${mp}\n"
                    writer.write(line)

                    lastLatLong = ll
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
    fun createJsonExportFile(context: Context, trackEntity: TrackEntity?): File? {
        logger.d(TAG, "createJsonExportFile")

        if (trackEntity == null) {
            return null
        }

        val exported = trackEntity.toPojo()

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "track_${exported.uuid}.json")

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
    fun loadTrackEntityFromUri(context: Context, uri: Uri): TrackEntity? {
        logger.d(TAG, "loadTrackEntityFromUri")

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val objectMapper = ObjectMapper()
                val imported = objectMapper.readValue(inputStream, TrackEntityPojo::class.java)
                return imported.toProto()
            }
        } catch (e: Exception) {
            logger.d(TAG, "loadTrackEntityFromUri: error", e)
            null
        }
    }

    internal fun shareFile(context: Context, file: File) {
        val userType = if (file.extension == "json") "JSON" else "CSV"
        logger.d(TAG, "shareFile: $file as $userType")

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match the authorities in AndroidManifest
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = if (file.extension == "json") "application/json" else "text/csv"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Export $userType Location Track"))
    }
}
