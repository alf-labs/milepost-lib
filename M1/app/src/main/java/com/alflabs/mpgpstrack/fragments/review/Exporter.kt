package com.alflabs.mpgpstrack.fragments.review

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.alflabs.mpgpstrack.tracks.TrackEntity
import java.io.File
import java.io.FileOutputStream


// ... inside your data handling class

object Exporter {
    private val TAG = Exporter::class.java.simpleName

    fun createCsvExportFile(context: Context, trackEntity: TrackEntity?): File? {
        Log.d(TAG, "createCsvExportFile")
        val csvHeader = "timestamp_millis,latitude,longitude,speed_mps\n"

        if (trackEntity == null) {
            return null
        }
        val locationList = trackEntity.locationList ?: return null

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "track_${trackEntity.uuid}.csv")

            FileOutputStream(file).use {
                it.write(csvHeader.toByteArray())
                locationList.locationsList.forEach { data ->
                        val ll = data.latLong
                        val sb = data.speedBearing
                        val line = "${ll.timestampMillis},${ll.latitude},${ll.longitude},${sb?.speedMps ?: ""}\n"
                        it.write(line.toByteArray())
                    }
            }
            file // Return the created file
        } catch (e: Exception) {
            // Handle file creation error
            Log.d(TAG, "createCsvExportFile: error", e)
            null
        }
    }

    internal fun shareFile(context: Context, file: File) {
        Log.d(TAG, "shareFile: $file")
        // Get the content URI using the FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match the authorities in AndroidManifest
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/csv" // Set the MIME type for the file
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the activity to show the share sheet
        context.startActivity(Intent.createChooser(shareIntent, "Export Location Track"))
    }
}

