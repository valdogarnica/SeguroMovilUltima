package com.devg2studios.seguromovilultima.di

import android.content.Context
import com.devg2studios.seguromovilultima.data.local.backup.BackupDatabase
import com.devg2studios.seguromovilultima.data.local.backup.SyncedMediaDao
import com.devg2studios.seguromovilultima.data.local.backup.UploadProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupDatabaseModule {
    @Provides
    @Singleton
    fun provideBackupDatabase(@ApplicationContext context: Context): BackupDatabase =
        BackupDatabase.getInstance(context)

    @Provides
    fun provideSyncedMediaDao(db: BackupDatabase): SyncedMediaDao = db.syncedMediaDao()

    @Provides
    @Singleton
    fun provideUploadProgressDao(db: BackupDatabase): UploadProgressDao = db.uploadProgressDao()
}