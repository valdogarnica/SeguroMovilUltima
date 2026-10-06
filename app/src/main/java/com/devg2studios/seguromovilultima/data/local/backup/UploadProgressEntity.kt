package com.devg2studios.seguromovilultima.data.local.backup

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_progress")
data class UploadProgressEntity(
    @PrimaryKey val mediaStoreId: Long,
    val ultimaParteCompletada: Int
)