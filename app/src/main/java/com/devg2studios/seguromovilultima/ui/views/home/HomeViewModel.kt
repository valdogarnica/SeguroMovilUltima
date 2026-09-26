package com.devg2studios.seguromovilultima.ui.views.home

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import com.devg2studios.seguromovilultima.data.local.DeviceIdProvider
import com.devg2studios.seguromovilultima.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdProvider: DeviceIdProvider,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val deviceId: String get() = deviceIdProvider.getDeviceId()
    val userEmail: String get() = authRepository.getSavedEmail() ?: "Sin sesión"

    val appVersion: String
        get() = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "desconocida"
        } catch (e: PackageManager.NameNotFoundException) {
            "desconocida"
        }

    fun logout() {
        authRepository.logout()
    }
}