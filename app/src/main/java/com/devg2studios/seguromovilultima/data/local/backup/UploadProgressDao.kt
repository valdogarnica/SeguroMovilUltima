package com.devg2studios.seguromovilultima.data.local.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UploadProgressDao {
    @Query("SELECT ultimaParteCompletada FROM upload_progress WHERE mediaStoreId = :id")
    suspend fun getUltimaParte(id: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarProgreso(entity: UploadProgressEntity)

    @Query("DELETE FROM upload_progress WHERE mediaStoreId = :id")
    suspend fun limpiarProgreso(id: Long)
}