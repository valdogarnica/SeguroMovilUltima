// version 1.1 modificacion para encolar los comandos de foto simultaneos, la version original esta en git


package com.devg2studios.seguromovilultima.services

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PhotoCaptureHelper {

    private val captureMutex = Mutex()

    suspend fun captureSilentPhoto(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean = true
    ): String = captureMutex.withLock {
        captureInternal(context, lifecycleOwner, useFrontCamera)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun captureInternal(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean
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

            Handler(Looper.getMainLooper()).postDelayed({
                val name = "SEGUROMOVILULTIMA_${System.currentTimeMillis()}.jpg"
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
            }, 800) // aumenté el margen, ver punto 2 abajo
        }, ContextCompat.getMainExecutor(context))
    }
}