package com.devg2studios.seguromovilultima.services.backup

import android.content.ContentUris
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devg2studios.seguromovilultima.data.local.DeviceIdProvider
import com.devg2studios.seguromovilultima.data.local.backup.SyncedMediaDao
import com.devg2studios.seguromovilultima.data.local.backup.SyncedMediaEntity
import com.devg2studios.seguromovilultima.data.local.backup.UploadProgressDao
import com.devg2studios.seguromovilultima.data.local.backup.UploadProgressEntity
import com.devg2studios.seguromovilultima.data.remote.api.backup.MediaBackupApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.source
import retrofit2.Response
import java.io.FileInputStream
import kotlin.math.ceil
import kotlin.math.min

@HiltWorker
class MediaUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: MediaBackupApi,
    private val dao: SyncedMediaDao,
    private val progressDao: UploadProgressDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val scanner: MediaBackupScanner
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MEDIA_STORE_ID = "media_store_id"
        const val KEY_IS_VIDEO = "is_video"
        const val KEY_SOURCE_FOLDER = "source_folder"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_TAKEN_AT = "taken_at"

        // 45MB por parte: margen cómodo bajo el límite de 500MB del backend.
        private const val CHUNK_SIZE = 45L * 1024 * 1024
        // Videos por debajo de esto se suben de una sola vez, como las fotos.
        private const val VIDEO_CHUNK_THRESHOLD = 90L * 1024 * 1024

        // Códigos HTTP que NUNCA se arreglan con un reintento (el archivo seguirá
        // siendo "demasiado grande" o "mal formado" la próxima vez).
        private val PERMANENT_ERROR_CODES = setOf(400, 413, 415, 422)
    }

    override suspend fun doWork(): Result {
        val mediaStoreId = inputData.getLong(KEY_MEDIA_STORE_ID, -1L)
        val wifiOnly = inputData.getBoolean("wifiOnly", true)
        Log.d("MEDIA_BACKUP", "Worker iniciado para mediaStoreId=$mediaStoreId")

        if (mediaStoreId == -1L) {
            Log.e("MEDIA_BACKUP", "mediaStoreId inválido, fallando worker")
            return finishAndReplenish(mediaStoreId, wifiOnly, Result.failure())
        }

        if (wifiOnly && !isConnectedToWifi(applicationContext)) {
            Log.d("MEDIA_BACKUP", "wifiOnly=true pero no hay Wi-Fi real activa, reintentando más tarde")
            return Result.retry() // sigue en la cola, no libera el hueco
        }

        val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
        val sourceFolder = inputData.getString(KEY_SOURCE_FOLDER) ?: "Otros"
        val mimeType = inputData.getString(KEY_MIME_TYPE) ?: "application/octet-stream"
        val takenAtMillis = inputData.getLong(KEY_TAKEN_AT, System.currentTimeMillis())

        val contentUri: Uri = if (isVideo) {
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        } else {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
        }


        val result = try {
            val contentResolver = applicationContext.contentResolver
            val fileSize = contentResolver.openAssetFileDescriptor(contentUri, "r")?.use { it.length } ?: -1L
            val deviceId = deviceIdProvider.getDeviceId()

            if (isVideo && fileSize > VIDEO_CHUNK_THRESHOLD) {
                Log.d("MEDIA_BACKUP", "Video grande ($fileSize bytes) para mediaStoreId=$mediaStoreId, subiendo por partes")
                uploadEnPartes(mediaStoreId, contentUri, fileSize, deviceId, sourceFolder, mimeType, takenAtMillis)
            } else {
                Log.d("MEDIA_BACKUP", "Preparando subida directa para $contentUri (tamaño: $fileSize bytes)")
                uploadDirecto(mediaStoreId, contentUri, fileSize, isVideo, deviceId, sourceFolder, mimeType, takenAtMillis)
            }
        } catch (e: Exception) {
            Log.e("MEDIA_BACKUP", "EXCEPCIÓN subiendo mediaStoreId=$mediaStoreId: ${e.message}", e)
            Result.retry()
        }

        // isRetry lo marcan las funciones de subida cuando corresponde reintentar
        // en vez de liberar el hueco; lo leemos de vuelta aquí:
        val isRetry = result == Result.retry()

        return if (isRetry) result else finishAndReplenish(mediaStoreId, wifiOnly, result)
    }

    /** Subida de un solo archivo (fotos, o videos chicos). Comportamiento original. */
    private suspend fun uploadDirecto(
        mediaStoreId: Long,
        contentUri: Uri,
        fileSize: Long,
        isVideo: Boolean,
        deviceId: String,
        sourceFolder: String,
        mimeType: String,
        takenAtMillis: Long
    ): Result {
        val contentResolver = applicationContext.contentResolver
        val inputStream = contentResolver.openInputStream(contentUri)
        if (inputStream == null) {
            Log.e("MEDIA_BACKUP", "No se pudo abrir el archivo $contentUri para mediaStoreId=$mediaStoreId")
            return Result.failure()
        }

        val fileName = if (isVideo) "backup.mp4" else "backup.jpg"
        val requestBody = object : okhttp3.RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()
            override fun contentLength() = fileSize
            override fun writeTo(sink: okio.BufferedSink) {
                inputStream.use { stream -> sink.writeAll(stream.source()) }
            }
        }

        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val response = api.uploadBackup(
            deviceId = deviceId,
            file = filePart,
            sourceFolder = sourceFolder.toRequestBody("text/plain".toMediaTypeOrNull()),
            mediaType = (if (isVideo) "VIDEO" else "IMAGE").toRequestBody("text/plain".toMediaTypeOrNull()),
            takenAtEpochMillis = takenAtMillis.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        )

        return manejarRespuesta(response, mediaStoreId)
    }

    /**
     * Divide el video en partes de CHUNK_SIZE bytes y las sube secuencialmente.
     * Nombra cada parte "<mediaStoreId>_parte<N>_de_<total>" para que el backend
     * las guarde en R2 con ese esquema de nombres.
     */
    private suspend fun uploadEnPartes(
        mediaStoreId: Long,
        contentUri: Uri,
        fileSize: Long,
        deviceId: String,
        sourceFolder: String,
        mimeType: String,
        takenAtMillis: Long
    ): Result {
        val totalPartes = ceil(fileSize.toDouble() / CHUNK_SIZE).toInt().coerceAtLeast(1)
        val parteInicial = (progressDao.getUltimaParte(mediaStoreId) ?: 0) + 1
        if(parteInicial > 1){
            Log.d("MEDIA_BACKUP", "Reanudando mediaStoreId=$mediaStoreId desde parte $parteInicial/$totalPartes")
        }
        val pfd: ParcelFileDescriptor = applicationContext.contentResolver.openFileDescriptor(contentUri, "r")
            ?: run {
                Log.e("MEDIA_BACKUP", "No se pudo abrir ParcelFileDescriptor para mediaStoreId=$mediaStoreId")
                return Result.failure()
            }

        pfd.use {
            for (parte in parteInicial..totalPartes) {
                val offset = (parte - 1).toLong() * CHUNK_SIZE
                val tamanoParte = min(CHUNK_SIZE, fileSize - offset)

                val chunkRequestBody = object : okhttp3.RequestBody() {
                    override fun contentType() = mimeType.toMediaTypeOrNull()
                    override fun contentLength() = tamanoParte
                    override fun writeTo(sink: okio.BufferedSink) {
                        FileInputStream(pfd.fileDescriptor).use { fis ->
                            fis.channel.position(offset)
                            var restante = tamanoParte
                            val buffer = ByteArray(64 * 1024)
                            while (restante > 0) {
                                val leido = fis.read(buffer, 0, min(buffer.size.toLong(), restante).toInt())
                                if (leido == -1) break
                                sink.write(buffer, 0, leido)
                                restante -= leido
                            }
                        }
                    }
                }

                val nombreParte = "video_${mediaStoreId}_parte${parte}_de_${totalPartes}.mp4"
                val filePart = MultipartBody.Part.createFormData("file", nombreParte, chunkRequestBody)

                Log.d("MEDIA_BACKUP", "Subiendo parte $parte/$totalPartes de mediaStoreId=$mediaStoreId ($tamanoParte bytes)")

                val response = api.uploadBackupChunk(
                    deviceId = deviceId,
                    file = filePart,
                    mediaStoreId = mediaStoreId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    parte = parte.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    totalPartes = totalPartes.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    sourceFolder = sourceFolder.toRequestBody("text/plain".toMediaTypeOrNull()),
                    takenAtEpochMillis = takenAtMillis.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                )

                val resultadoParte = manejarRespuesta(response, mediaStoreId, parte, totalPartes)
                if (resultadoParte == Result.failure()) {
                    // Falla permanente (413, 400, etc.): nunca va a funcionar reanudar, descarta el rastro.
                    progressDao.limpiarProgreso(mediaStoreId)
                    return resultadoParte
                }
                if (resultadoParte == Result.retry()) {
                    // Falla transitoria (red, 5xx): NO tocar el progreso — así la próxima vez
                    // retoma exactamente donde se quedó, en vez de re-subir todo desde cero.
                    return resultadoParte
                }
                progressDao.guardarProgreso(UploadProgressEntity(mediaStoreId, parte))
            }
        }

        // Todas las partes subieron: registramos UNA sola fila para el video completo.
        val completeResponse = api.completeBackupChunks(
            deviceId = deviceId,
            mediaStoreId = mediaStoreId,
            totalPartes = totalPartes,
            sourceFolder = sourceFolder,
            mediaType = "VIDEO",
            takenAtEpochMillis = takenAtMillis,
            fileSizeBytes = fileSize
        )

        if (!completeResponse.isSuccessful) {
            Log.e("MEDIA_BACKUP", "Falló el registro final de mediaStoreId=$mediaStoreId: ${completeResponse.code()}")
            return Result.retry() // las partes ya están en R2; solo falta registrar, vale la pena reintentar
        }

        Log.d("MEDIA_BACKUP", "Video completo registrado para mediaStoreId=$mediaStoreId ($totalPartes partes)")
        progressDao.limpiarProgreso(mediaStoreId)
        dao.markSynced(SyncedMediaEntity(mediaStoreId))
        return Result.success()
    }

    private suspend fun manejarRespuesta(
        response: Response<*>,
        mediaStoreId: Long,
        parte: Int? = null,
        totalPartes: Int? = null
    ): Result {
        if (response.isSuccessful) {
            if (parte == null) {
                Log.d("MEDIA_BACKUP", "Subida exitosa para mediaStoreId=$mediaStoreId")
                dao.markSynced(SyncedMediaEntity(mediaStoreId))
            } else {
                Log.d("MEDIA_BACKUP", "Parte $parte/$totalPartes exitosa para mediaStoreId=$mediaStoreId")
            }
            return Result.success()
        }

        val code = response.code()
        val etiquetaParte = if (parte != null) " (parte $parte/$totalPartes)" else ""
        Log.e(
            "MEDIA_BACKUP",
            "Backend respondió error $code para mediaStoreId=$mediaStoreId$etiquetaParte: ${response.errorBody()?.string()}"
        )

        return if (code in PERMANENT_ERROR_CODES) {
            Result.failure() // no tiene sentido reintentar, nunca va a cambiar
        } else {
            Result.retry() // 5xx, 429, etc: puede resolverse solo
        }
    }

    private suspend fun finishAndReplenish(mediaStoreId: Long, wifiOnly: Boolean, result: Result): Result {
        dao.dequeue(mediaStoreId)
        scanner.scanAndEnqueuePending(wifiOnly)
        return result
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val cm = context.getSystemService<ConnectivityManager>() ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}