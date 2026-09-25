package com.devg2studios.seguromovilultima.ui.views.codigoIncorrecto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devg2studios.seguromovilultima.data.local.SecuritySettings
import com.devg2studios.seguromovilultima.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FailedAttemptSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<SecuritySettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = SecuritySettings()
    )


    fun onFailedAttemptEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setFailedAttemptDetectionEnabled(enabled) }
    }

    fun onSendLocationChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setSendLocation(enabled) }
    }
    fun onSendPhotoChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSendPhoto(enabled)
            if (enabled) repository.setSendVideo(false) // exclusión mutua
        }
    }

    fun onSendVideoChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSendVideo(enabled)
            if (enabled) repository.setSendPhoto(false) // exclusión mutua
        }
    }

    fun onStartAlarmChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.setStartAlarm(enabled)
            if (!enabled) repository.setShowFlashScreen(false) // si apagas alarma, apaga flash también
        }
    }

    fun onShowFlashScreenChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setShowFlashScreen(enabled) }
    }

}