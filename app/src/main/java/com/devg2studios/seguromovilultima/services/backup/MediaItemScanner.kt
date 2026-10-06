package com.devg2studios.seguromovilultima.services.backup

import android.content.Context
import android.provider.MediaStore

data class ScannedMediaItem(
    val mediaStoreId: Long,
    val contentUri: android.net.Uri,
    val mimeType: String,
    val sourceFolder: String,
    val takenAtMillis: Long,
    val isVideo: Boolean
)

object MediaItemScanner {

    /**
     * Consulta MediaStore.Files filtrando solo imágenes y videos — esto
     * incluye todo lo indexado por el sistema: cámara, capturas de
     * pantalla, WhatsApp Images/Video, Snapchat, Telegram, etc., ya que
     * todas esas apps guardan sus archivos a través de MediaStore desde
     * Android 10 en adelante (scoped storage).
     */
    fun scanAll(context: Context): List<ScannedMediaItem> {
        val items = mutableListOf<ScannedMediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val queryUri = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(
            queryUri, projection, selection, selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeCol) ?: "application/octet-stream"
                val folder = cursor.getString(folderCol) ?: "Otros"
                val dateAddedSeconds = cursor.getLong(dateCol)
                val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                val contentUri = if (isVideo) {
                    android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                items.add(
                    ScannedMediaItem(
                        mediaStoreId = id,
                        contentUri = contentUri,
                        mimeType = mimeType,
                        sourceFolder = folder,
                        takenAtMillis = dateAddedSeconds * 1000,
                        isVideo = isVideo
                    )
                )
            }
        }

        return items
    }
}