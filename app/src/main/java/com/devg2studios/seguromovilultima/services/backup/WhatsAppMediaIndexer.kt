
package com.devg2studios.seguromovilultima.services.backup

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

object WhatsAppMediaIndexer {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    private val SUBFOLDERS = listOf(
        "WhatsApp/Media/WhatsApp Images",
        "WhatsApp/Media/WhatsApp Images/Sent",
        "WhatsApp/Media/WhatsApp Video",
        "WhatsApp/Media/WhatsApp Video/Sent",
        "WhatsApp/Media/WhatsApp Animated Gifs",
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Media/WhatsApp Profile Photos"
    )

    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "mp4", "3gp", "mkv", "gif")

    /** Requiere MANAGE_EXTERNAL_STORAGE concedido. Indexa archivos nuevos en
     *  MediaStore para que el pipeline normal de respaldo los recoja. */
    fun indexPrivateMedia(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Log.w("MEDIA_BACKUP", "Sin permiso de Acceso a todos los archivos, no se indexa WhatsApp privado")
            return
        }

        val root = Environment.getExternalStorageDirectory()
        val paths = mutableListOf<String>()

        for (pkg in WHATSAPP_PACKAGES) {
            for (sub in SUBFOLDERS) {
                collectFiles(File(root, "Android/media/$pkg/$sub"), paths)
            }
        }

        if (paths.isEmpty()) {
            Log.d("MEDIA_BACKUP", "Nada nuevo que indexar de WhatsApp")
            return
        }

        Log.d("MEDIA_BACKUP", "Indexando ${paths.size} archivos privados de WhatsApp")
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
    }

    private fun collectFiles(dir: File, out: MutableList<String>) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectFiles(file, out)
            } else if (file.extension.lowercase() in EXTENSIONS) {
                out.add(file.absolutePath)
            }
        }
    }
}