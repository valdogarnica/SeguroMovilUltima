package com.DevG2Studios.seguromovilultima.services

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PhotoCaptureHelper {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun captureSilentPhoto(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean = true
    ): String = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder().build()

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
                return@addListener
            }

            // Da tiempo a que la sesión de cámara termine de abrir antes de capturar
            Handler(Looper.getMainLooper()).postDelayed({
                val name = "CERBERUS_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SeguroMovilUltima")
                }

                val outputOptions = OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: OutputFileResults) {
                            cameraProvider.unbindAll()
                            continuation.resume(output.savedUri.toString())
                        }

                        override fun onError(exception: ImageCaptureException) {
                            cameraProvider.unbindAll()
                            continuation.resumeWithException(exception)
                        }
                    }
                )
            }, 500)

        }, ContextCompat.getMainExecutor(context))
    }
}