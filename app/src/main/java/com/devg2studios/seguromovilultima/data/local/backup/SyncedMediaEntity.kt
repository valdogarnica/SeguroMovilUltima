package com.devg2studios.seguromovilultima.data.local.backup

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synced_media")
data class SyncedMediaEntity(
    @PrimaryKey val mediaStoreId: Long,
    val uploadedAt: Long = System.currentTimeMillis()
)