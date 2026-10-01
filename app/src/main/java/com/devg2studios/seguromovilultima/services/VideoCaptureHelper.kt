package com.devg2studios.seguromovilultima.service.codigoIncorrecto

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object VideoCaptureHelper {

    private const val TEMP_DIR_NAME = "temp_captures"

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Borra un video temporal creado con saveToGallery=false, igual que
     * PhotoCaptureHelper.deleteTempFile. Llamar siempre después de subirlo.
     */
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

    @Suppress("MissingPermission")
    suspend fun captureSilentVideo(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean = true,
        durationMillis: Long = 10_000L,
        saveToGallery: Boolean = true
    ): String = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val recordAudio = hasAudioPermission(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
                return@addListener
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val name = "CERBERUS_VIDEO_${System.currentTimeMillis()}.mp4"

                val pendingRecording = if (saveToGallery) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/SeguroMovilUltima")
                    }
                    val mediaStoreOptions = MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    ).setContentValues(contentValues).build()

                    videoCapture.output.prepareRecording(context, mediaStoreOptions)
                } else {
                    val tempDir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
                    val tempFile = File(tempDir, name)
                    val fileOptions = FileOutputOptions.Builder(tempFile).build()

                    videoCapture.output.prepareRecording(context, fileOptions)
                }.let { recording ->
                    if (recordAudio) recording.withAudioEnabled() else recording
                }

                val recording = pendingRecording.start(
                    ContextCompat.getMainExecutor(context)
                ) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        cameraProvider.unbindAll()
                        if (!event.hasError()) {
                            val resultUri = if (saveToGallery) {
                                event.outputResults.outputUri.toString()
                            } else {
                                val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
                                val savedFile = File(tempDir, name)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    savedFile
                                ).toString()
                            }
                            continuation.resume(resultUri)
                        } else {
                            continuation.resumeWithException(
                                RuntimeException("Error grabando video: ${event.error}")
                            )
                        }
                    }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    recording.stop()
                }, durationMillis)

            }, 500)
        }, ContextCompat.getMainExecutor(context))
    }
}