
package com.devg2studios.seguromovilultima.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devg2studios.seguromovilultima.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugToolsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun setLocationTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocationTrackingEnabled(enabled)
        }
    }
    fun setMediaBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMediaBackupEnabled(enabled)
        }
    }

    fun setMediaBackupWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMediaBackupWifiOnly(enabled)
        }
    }
}
