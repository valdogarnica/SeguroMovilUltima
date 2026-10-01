// version 1.1 modificación para encolar los comandos de foto simultáneos, la version original está en git


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
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PhotoCaptureHelper {

    private val captureMutex = Mutex()
    private const val TEMP_DIR_NAME = "temp_captures"

    /**
     * @param saveToGallery si es false, la foto se guarda en un archivo
     * temporal privado de la app (no visible en la galería del usuario)
     * y debe borrarse manualmente con [deleteTempFile] una vez subida.
     */
    suspend fun captureSilentPhoto(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean = true,
        saveToGallery: Boolean = true
    ): String = captureMutex.withLock {
        captureInternal(context, lifecycleOwner, useFrontCamera, saveToGallery)
    }

    /**
     * Borra un archivo temporal creado con saveToGallery=false. Llamar
     * SIEMPRE después de terminar de subir la foto (éxito o error), para
     * que el caché de la app no crezca con capturas ya reportadas.
     */
    fun deleteTempFile(context: Context, uriString: String) {
        if (!uriString.contains(TEMP_DIR_NAME)) return // solo borra los que nosotros creamos
        try {
            val fileName = uriString.substringAfterLast("/")
            val file = File(File(context.cacheDir, TEMP_DIR_NAME), fileName)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // no crítico: si falla el borrado, el caché del sistema lo limpiará eventualmente
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun captureInternal(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean,
        saveToGallery: Boolean
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

                val outputOptions = if (saveToGallery) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SeguroMovilUltima")
                    }
                    OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ).build()
                } else {
                    val tempDir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
                    val tempFile = File(tempDir, name)
                    OutputFileOptions.Builder(tempFile).build()
                }

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: OutputFileResults) {
                            cameraProvider.unbindAll()
                            val resultUri = if (saveToGallery) {
                                output.savedUri.toString()
                            } else {
                                // output.savedUri es null en modo File; reconstruimos
                                // el File y lo exponemos como content:// vía FileProvider,
                                // así ResultReporter puede leerlo igual que siempre.
                                val tempDir = File(context.cacheDir, TEMP_DIR_NAME)
                                val savedFile = File(tempDir, name)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    savedFile
                                ).toString()
                            }
                            continuation.resume(resultUri)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            cameraProvider.unbindAll()
                            continuation.resumeWithException(exception)
                        }
                    }
                )
            }, 800)
        }, ContextCompat.getMainExecutor(context))
    }
}