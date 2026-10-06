package com.devg2studios.seguromovilultima.data.local.backup

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_media")
data class QueuedMediaEntity(
    @PrimaryKey val mediaStoreId: Long,
    val enqueuedAt: Long = System.currentTimeMillis()
)