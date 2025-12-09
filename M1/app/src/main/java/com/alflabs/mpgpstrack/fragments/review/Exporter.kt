package com.alflabs.mpgpstrack.fragments.review

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.utils.Units
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter

/*
    Export a GPS track.

    Google My Maps supported formats:
    https://support.google.com/mymaps/answer/3024836
    with a friendly description here including some CSV examples:
    https://www.google.com/earth/outreach/learn/visualize-your-data-on-a-custom-map-using-google-my-maps/

*/

object Exporter {
    private val TAG = Exporter::class.java.simpleName

    fun createCsvExportFile(context: Context, trackEntity: TrackEntity?): File? {
        Log.d(TAG, "createCsvExportFile")
        val csvHeader = "isoDateTime,latitude,longitude,speed_mph\n"

        if (trackEntity == null) {
            return null
        }
        val locationList = trackEntity.locationList ?: return null

        return try {
            // Use the cache directory for the export file
            val cacheDir = context.cacheDir
            val file = File(cacheDir, "track_${trackEntity.uuid}.csv")

            val formatter = DateTimeFormatter.ISO_INSTANT

            FileOutputStream(file).use { fos ->
                fos.write(csvHeader.toByteArray())
                locationList.locationsList.forEach { data ->
                        val ll = data.latLong
                        val sb = data.speedBearing
                        val speed = if (sb != null) Units.mps_to_mph(sb.speedMps) else ""

                        val instant = Instant.ofEpochMilli(ll.timestampMillis)
                        val isoDT = formatter.format(instant)

                        val line = "${isoDT},${ll.latitude},${ll.longitude},${speed}\n"
                        fos.write(line.toByteArray())
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

        // TBD Questions:
        // - Can I know when the chooser finished?
        // - How do I cleanup the temp files?
    }
}

