package com.hifzsniper.app.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

class AudioDownloadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueue(downloadId: String, unmeteredOnly: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(Data.Builder().putString(AudioDownloadWorker.KEY_DOWNLOAD_ID, downloadId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .build()
        workManager.enqueueUniqueWork("audio-download-$downloadId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(downloadId: String) = workManager.cancelUniqueWork("audio-download-$downloadId")
}
