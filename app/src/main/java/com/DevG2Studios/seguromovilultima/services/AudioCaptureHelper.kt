

package com.DevG2Studios.seguromovilultima.services.codigoIncorrecto

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object AudioCaptureHelper {

    suspend fun captureSilentAudio(
        context: Context,
        durationMillis: Long = 10_000L
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            val name = "SEGUROMOVIL_AUDIO_${System.currentTimeMillis()}.m4a"
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

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

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

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}