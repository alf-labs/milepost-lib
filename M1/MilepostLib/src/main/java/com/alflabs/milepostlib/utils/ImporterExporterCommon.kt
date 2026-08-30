package com.alflabs.milepostlib.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.alflabs.utils.ILogger
import java.io.File

open class ImporterExporterCommon(
    open val logger: ILogger,
) {

    companion object {
        private val TAG = ImporterExporterCommon::class.java.simpleName
    }

    fun sanitizeTitle(rawTitle: String?): String {
        var title = rawTitle?.trim() ?: ""
        title = title
            .replace(Regex("[^A-Za-z0-9_+-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return " ${title.take(64)}"
    }

    fun shareFile(context: Context, file: File) {
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
