package com.devg2studios.seguromovilultima.data.local.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncedMediaDao {
    @Query("SELECT mediaStoreId FROM synced_media")
    suspend fun getAllSyncedIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSynced(entity: SyncedMediaEntity)

    @Query("SELECT COUNT(*) FROM synced_media")
    suspend fun count(): Int

    // --- Cola de subida: evita volver a encolar lo que ya está en vuelo ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueueBatch(entities: List<QueuedMediaEntity>)

    @Query("DELETE FROM queued_media WHERE mediaStoreId = :id")
    suspend fun dequeue(id: Long)

    @Query("SELECT mediaStoreId FROM queued_media")
    suspend fun getQueuedIds(): List<Long>

    @Query("SELECT COUNT(*) FROM queued_media")
    suspend fun queuedCount(): Int

    // SyncedMediaDao.kt — agregar:
    @Query("DELETE FROM queued_media")
    suspend fun clearAllQueued()
}