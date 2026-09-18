package com.DevG2Studios.seguromovilultima.services.codigoIncorrecto

import android.content.Context
import android.net.Uri
import android.util.Log
import com.DevG2Studios.seguromovilultima.data.remote.api.CommandApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object ResultReporter {

    private const val TAG = "RESULT_REPORTER"

    suspend fun report(
        api: CommandApi,
        commandId: Long,
        deviceId: String,
        success: Boolean,
        errorMessage: String? = null,
        fileUri: Uri? = null,
        context: Context? = null
    ) {
        try {
            Log.d(TAG, "Reportando resultado: commandId=$commandId, success=$success, fileUri=$fileUri")

            val filePart = if (fileUri != null && context != null) {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                bytes?.let {
                    val mimeType = context.contentResolver.getType(fileUri)
                        ?: guessMimeTypeFromUri(fileUri)
                    val extension = mimeType.substringAfter("/", "dat")
                    val fileName = "result.$extension"

                    Log.d(TAG, "Archivo leído: ${it.size} bytes, mimeType=$mimeType, fileName=$fileName")

                    val requestBody = it.toRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("file", fileName, requestBody)
                }
            } else null

            val response = api.reportResult(
                commandId = commandId,
                deviceId = deviceId.toRequestBody("text/plain".toMediaTypeOrNull()),
                success = success.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                errorMessage = errorMessage?.toRequestBody("text/plain".toMediaTypeOrNull()),
                file = filePart
            )

            if (response.isSuccessful) {
                Log.d(TAG, "Resultado reportado exitosamente al backend")
            } else {
                Log.e(TAG, "El backend respondió con error: ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPCIÓN al reportar resultado: ${e.message}", e)
        }
    }

    private fun guessMimeTypeFromUri(uri: Uri): String {
        val path = uri.toString().lowercase()
        return when {
            path.contains(".jpg") || path.contains(".jpeg") -> "image/jpeg"
            path.contains(".mp4") -> "video/mp4"
            path.contains(".m4a") -> "audio/mp4"
            else -> "application/octet-stream"
        }
    }
}