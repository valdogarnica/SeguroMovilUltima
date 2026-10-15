package com.devg2studios.seguromovilultima.services.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import com.devg2studios.seguromovilultima.data.local.backup.QueuedMediaEntity
import com.devg2studios.seguromovilultima.data.local.backup.SyncedMediaDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaBackupScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: SyncedMediaDao
) {
    companion object {
        // Muy por debajo del límite de 100 jobs simultáneos que impone Android.
        private const val MAX_CONCURRENT_JOBS = 20
    }

    suspend fun scanAndEnqueuePending(wifiOnly: Boolean) {
        val allMedia = MediaItemScanner.scanAll(context)
        val syncedIds = dao.getAllSyncedIds().toHashSet()
        val queuedIds = dao.getQueuedIds().toHashSet()

        val pending = allMedia.filter {
            it.mediaStoreId !in syncedIds && it.mediaStoreId !in queuedIds
        }

        Log.d("MEDIA_BACKUP", "MediaStore=${allMedia.size} sincronizados=${syncedIds.size} en-cola=${queuedIds.size} pendientes-nuevos=${pending.size}")

        val huecosLibres = MAX_CONCURRENT_JOBS - queuedIds.size
        if (huecosLibres <= 0) {
            Log.d("MEDIA_BACKUP", "Cola llena ($MAX_CONCURRENT_JOBS), no se agrega nada por ahora")
            return
        }

        val batch = pending.take(huecosLibres)
        if (batch.isEmpty()) {
            Log.d("MEDIA_BACKUP", "No hay pendientes nuevos para encolar")
            return
        }

        dao.enqueueBatch(batch.map { QueuedMediaEntity(it.mediaStoreId) })

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        batch.forEach { item ->
            Log.d("MEDIA_BACKUP", "Encolando: id=${item.mediaStoreId}, folder=${item.sourceFolder}, isVideo=${item.isVideo}")
            val inputData = workDataOf(
                MediaUploadWorker.KEY_MEDIA_STORE_ID to item.mediaStoreId,
                MediaUploadWorker.KEY_IS_VIDEO to item.isVideo,
                MediaUploadWorker.KEY_SOURCE_FOLDER to item.sourceFolder,
                MediaUploadWorker.KEY_MIME_TYPE to item.mimeType,
                MediaUploadWorker.KEY_TAKEN_AT to item.takenAtMillis,
                "wifiOnly" to wifiOnly
            )

            val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "media_upload_${item.mediaStoreId}",
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        Log.d("MEDIA_BACKUP", "Lote encolado: ${batch.size} (quedan ${pending.size - batch.size} pendientes-nuevos en espera)")
    }
    suspend fun resetQueueAndScan(wifiOnly: Boolean) {
        dao.clearAllQueued()
        scanAndEnqueuePending(wifiOnly)
    }
}