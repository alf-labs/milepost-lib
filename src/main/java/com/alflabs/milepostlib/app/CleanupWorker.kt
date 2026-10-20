package com.alflabs.milepostlib.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        val TAG: String = CleanupWorker::class.java.simpleName
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        Log.d(TAG, "Running cleanup worker")

        return try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles { file ->
                file.name.startsWith("track_") && file.name.endsWith(".csv")
            }

            val twoDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)

            files?.forEach { file ->
                if (file.lastModified() < twoDaysAgo) {
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old temp file: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete old temp file: ${file.name}")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            Result.failure()
        }
    }
}
