
package com.DevG2Studios.seguromovilultima

import android.app.Application
import com.DevG2Studios.seguromovilultima.data.local.SecuritySettingsCache
import com.DevG2Studios.seguromovilultima.data.local.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SeguroMovilApp : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                SecuritySettingsCache.current = settings
            }
        }
    }
}