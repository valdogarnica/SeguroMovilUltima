package com.devg2studios.seguromovilultima.services.backup

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.os.bundleOf

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
     * incluye
     * lo que está indexado en la
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

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val queryArgs = bundleOf(
                ContentResolver.QUERY_ARG_SQL_SELECTION to
                        "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?",
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                ),
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER to "${MediaStore.Files.FileColumns.DATE_ADDED} DESC",
                // sin esto, MediaStore oculta por defecto lo que está en la papelera
                MediaStore.QUERY_ARG_MATCH_TRASHED to MediaStore.MATCH_INCLUDE
            )
            context.contentResolver.query(queryUri, projection, queryArgs, null)
        } else {
            val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
            val selectionArgs = arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            context.contentResolver.query(
                queryUri, projection, selection, selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )
        }

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val folderCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val mimeType = c.getString(mimeCol) ?: "application/octet-stream"
                val folder = c.getString(folderCol) ?: "Otros"
                val dateAddedSeconds = c.getLong(dateCol)
                val isVideo = c.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

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