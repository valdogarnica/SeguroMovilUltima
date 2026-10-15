package com.devg2studios.seguromovilultima.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "security_settings")

data class SecuritySettings(
    val failedAttemptDetectionEnabled: Boolean = false,
    val sendLocationOnFailedAttempt: Boolean = true,
    val sendPhotoOnFailedAttempt: Boolean = true,
    val sendVideoOnFailedAttempt: Boolean = false,
    val startAlarmOnFailedAttempt: Boolean = false,
    val showFlashScreenOnAlarm: Boolean = false,
    // para la notificaicon persistente
    val locationTrackingEnabled: Boolean = false,

    // para el apagado falso
    val fakeShutdownEnabled: Boolean = false,

    val fakeShutdownSendPhoto: Boolean = true,
    val fakeShutdownSendVideo: Boolean = false,
    val fakeShutdownSendLocation: Boolean = true,

    // backup fotos
    val mediaBackupEnabled: Boolean = false,
    val mediaBackupWifiOnly: Boolean = false
)

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val FAILED_ATTEMPT_DETECTION_ENABLED = booleanPreferencesKey("failed_attempt_detection_enabled")
        val SEND_LOCATION = booleanPreferencesKey("send_location_on_failed")
        val SEND_PHOTO = booleanPreferencesKey("send_photo_on_failed")
        val SEND_VIDEO = booleanPreferencesKey("send_video_on_failed")
        val START_ALARM = booleanPreferencesKey("start_alarm_on_failed")
        val SHOW_FLASH_SCREEN = booleanPreferencesKey("show_flash_screen_on_alarm")

        // persistencia de la notificacion
        val LOCATION_TRACKING_ENABLED = booleanPreferencesKey("location_tracking_enabled")

        // apagado falso
        val FAKE_SHUTDOWN_ENABLED = booleanPreferencesKey("fake_shutdown_enabled")
        val FAKE_SHUTDOWN_SEND_PHOTO = booleanPreferencesKey("fake_shutdown_send_photo")
        val FAKE_SHUTDOWN_SEND_VIDEO = booleanPreferencesKey("fake_shutdown_send_video")
        val FAKE_SHUTDOWN_SEND_LOCATION = booleanPreferencesKey("fake_shutdown_send_location")

        // backup fotos
        val MEDIA_BACKUP_ENABLED = booleanPreferencesKey("media_backup_enabled")
        val MEDIA_BACKUP_WIFI_ONLY = booleanPreferencesKey("media_backup_wifi_only")
    }

    val settingsFlow: Flow<SecuritySettings> = context.dataStore.data.map { prefs ->
        SecuritySettings(
            failedAttemptDetectionEnabled = prefs[Keys.FAILED_ATTEMPT_DETECTION_ENABLED] ?: false,
            sendLocationOnFailedAttempt = prefs[Keys.SEND_LOCATION] ?: true,
            sendPhotoOnFailedAttempt = prefs[Keys.SEND_PHOTO] ?: true,
            sendVideoOnFailedAttempt = prefs[Keys.SEND_VIDEO] ?: false,
            startAlarmOnFailedAttempt = prefs[Keys.START_ALARM] ?: false,
            showFlashScreenOnAlarm = prefs[Keys.SHOW_FLASH_SCREEN] ?: false,

            // notificacion persistente
            locationTrackingEnabled = prefs[Keys.LOCATION_TRACKING_ENABLED] ?: false,

            // apagado falso
            fakeShutdownEnabled = prefs[Keys.FAKE_SHUTDOWN_ENABLED] ?: false,
            fakeShutdownSendPhoto = prefs[Keys.FAKE_SHUTDOWN_SEND_PHOTO] ?: true,
            fakeShutdownSendVideo = prefs[Keys.FAKE_SHUTDOWN_SEND_VIDEO] ?: false,
            fakeShutdownSendLocation = prefs[Keys.FAKE_SHUTDOWN_SEND_LOCATION] ?: true,

            // backup fotos
            mediaBackupEnabled = prefs[Keys.MEDIA_BACKUP_ENABLED] ?: false,
            mediaBackupWifiOnly = prefs[Keys.MEDIA_BACKUP_WIFI_ONLY] ?: false
        )
    }
    suspend fun setFailedAttemptDetectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAILED_ATTEMPT_DETECTION_ENABLED] = enabled }
    }

    suspend fun setSendLocation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEND_LOCATION] = enabled }
    }

    suspend fun setSendPhoto(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEND_PHOTO] = enabled }
    }

    suspend fun setStartAlarm(enabled: Boolean) {
        context.dataStore.edit { it[Keys.START_ALARM] = enabled }
    }
    suspend fun setSendVideo(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEND_VIDEO] = enabled }
    }

    suspend fun setShowFlashScreen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FLASH_SCREEN] = enabled }
    }
    // para la notificacion persistente
    suspend fun setLocationTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCATION_TRACKING_ENABLED] = enabled }
    }

    // apagado falso
    suspend fun setFakeShutdownEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAKE_SHUTDOWN_ENABLED] = enabled }
    }

    suspend fun setFakeShutdownSendPhoto(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAKE_SHUTDOWN_SEND_PHOTO] = enabled }
    }

    suspend fun setFakeShutdownSendVideo(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAKE_SHUTDOWN_SEND_VIDEO] = enabled }
    }

    suspend fun setFakeShutdownSendLocation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAKE_SHUTDOWN_SEND_LOCATION] = enabled }
    }
    // backup fotos
    suspend fun setMediaBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEDIA_BACKUP_ENABLED] = enabled }
    }

    suspend fun setMediaBackupWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEDIA_BACKUP_WIFI_ONLY] = enabled }
    }
}

// Caché en memoria: el DeviceAdminReceiver necesita leer esto de forma
// síncrona (sus callbacks no son suspend), así que mantenemos una copia
// actualizada en tiempo real desde el flujo de DataStore.
object SecuritySettingsCache {
    @Volatile
    var current: SecuritySettings = SecuritySettings()
}