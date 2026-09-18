package com.DevG2Studios.seguromovilultima.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings

object AlarmHelper {

    private var mediaPlayer: MediaPlayer? = null
    private var originalRingerMode: Int? = null
    private var originalVolume: Int? = null

    fun playAlarm(context: Context) {
        stopAlarm(context) // por si ya había una sonando

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Guarda el estado original para poder restaurarlo después
        originalRingerMode = audioManager.ringerMode
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Fuerza volumen máximo en el stream de alarma
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        val alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI
            ?: Settings.System.DEFAULT_NOTIFICATION_URI

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, alarmUri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stopAlarm(context: Context) {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        // Restaura el volumen original
        originalVolume?.let { vol ->
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}