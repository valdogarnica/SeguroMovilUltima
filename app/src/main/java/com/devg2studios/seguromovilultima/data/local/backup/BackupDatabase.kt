package com.devg2studios.seguromovilultima.data.local.backup

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SyncedMediaEntity::class, QueuedMediaEntity::class, UploadProgressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BackupDatabase : RoomDatabase() {
    abstract fun syncedMediaDao(): SyncedMediaDao
    abstract fun uploadProgressDao(): UploadProgressDao

    companion object {
        @Volatile private var INSTANCE: BackupDatabase? = null

        fun getInstance(context: Context): BackupDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BackupDatabase::class.java,
                    "backup_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}