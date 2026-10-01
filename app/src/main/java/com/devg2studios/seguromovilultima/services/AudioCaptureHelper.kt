package com.devg2studios.seguromovilultima.services.codigoIncorrecto

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AudioCaptureHelper {

    private const val TEMP_DIR_NAME = "temp_captures"

    fun deleteTempFile(context: Context, uriString: String) {
        if (!uriString.contains(TEMP_DIR_NAME)) return
        try {
            val fileName = uriString.substringAfterLast("/")
            val file = File(File(context.cacheDir, TEMP_DIR_NAME), fileName)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // no crítico
        }
    }

    suspend fun captureSilentAudio(
        context: Context,
        durationMillis: Long = 10_000L,
        saveToGallery: Boolean = true
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val name = "SEGUROMOVIL_AUDIO_${System.currentTimeMillis()}.m4a"

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            if (saveToGallery) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/SeguroMovilUltima")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw IllegalStateException("No se pudo crear el archivo de audio")

                val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                    ?: throw IllegalStateException("No se pudo abrir el descriptor de archivo")

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    setOutputFile(pfd.fileDescriptor)
                    prepare()
                    start()
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        recorder.stop()
                        recorder.release()
                        pfd.close()
                        continuation.resume(uri.toString())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }, durationMillis)

            } else {
                // Modo privado: graba directo a un File en el caché de la app,
                // sin pasar por MediaStore, así nunca aparece en la galería/
                // reproductor de música del usuario.
                val tempDir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
                val tempFile = File(tempDir, name)

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128_000)
                    setAudioSamplingRate(44_100)
                    setOutputFile(tempFile.absolutePath)
                    prepare()
                    start()
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        recorder.stop()
                        recorder.release()
                        val resultUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        ).toString()
                        continuation.resume(resultUri)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }, durationMillis)
            }

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}