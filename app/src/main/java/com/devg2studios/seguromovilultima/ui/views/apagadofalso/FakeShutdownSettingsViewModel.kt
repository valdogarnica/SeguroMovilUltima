
package com.devg2studios.seguromovilultima.ui.views.apagadoFalso

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
class FakeShutdownSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<SecuritySettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecuritySettings()
    )

    fun onFakeShutdownEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setFakeShutdownEnabled(enabled) }
    }

    fun onSendPhotoChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFakeShutdownSendPhoto(enabled)
            if (enabled) repository.setFakeShutdownSendVideo(false) // exclusión mutua
        }
    }

    fun onSendVideoChanged(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFakeShutdownSendVideo(enabled)
            if (enabled) repository.setFakeShutdownSendPhoto(false) // exclusión mutua
        }
    }

    fun onSendLocationChanged(enabled: Boolean) {
        viewModelScope.launch { repository.setFakeShutdownSendLocation(enabled) }
    }
}