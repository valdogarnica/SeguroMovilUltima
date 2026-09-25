package com.devg2studios.seguromovilultima.ui.debug

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import com.devg2studios.seguromovilultima.services.LocationForegroundService
import com.devg2studios.seguromovilultima.services.AlarmHelper
import com.devg2studios.seguromovilultima.services.PhotoCaptureHelper

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Integer.MAX_VALUE).any {
        it.service.className == serviceClass.name
    }
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsScreen(
    onBack: () -> Unit,
    onGoToPermissions: () -> Unit,
    viewModel: DebugToolsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isProtectionActive by remember {
        mutableStateOf(isServiceRunning(context, LocationForegroundService::class.java))
    }
    val lastLocation by LocationForegroundService.lastLocation.collectAsState()

    var lastPhotoUri by remember { mutableStateOf<String?>(null) }
    var isAlarmPlaying by remember { mutableStateOf(false) }

    val hasLocationPermission = isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val hasCameraPermission = isPermissionGranted(context, Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pruebas de funciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasLocationPermission || !hasCameraPermission) {
                Text(
                    "Faltan permisos. Ve a \"Permisos y protección\" para concederlos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onGoToPermissions) {
                    Text("Ir a Permisos y protección")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = if (isProtectionActive) "Protección: ACTIVA" else "Protección: INACTIVA",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            lastLocation?.let { loc ->
                Text("Lat: ${loc.latitude}")
                Text("Lng: ${loc.longitude}")
            } ?: Text("Sin ubicación registrada aún")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!hasLocationPermission) {
                        onGoToPermissions()
                        return@Button
                    }
                    if (isProtectionActive) {
                        context.stopService(Intent(context, LocationForegroundService::class.java))
                        isProtectionActive = false
                        viewModel.setLocationTrackingEnabled(false)
                    } else {
                        context.startForegroundService(Intent(context, LocationForegroundService::class.java))
                        isProtectionActive = true
                        viewModel.setLocationTrackingEnabled(true)
                    }
                }
            ) {
                Text(if (isProtectionActive) "Detener protección" else "Activar protección")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (!hasCameraPermission) {
                    onGoToPermissions()
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        val uri = PhotoCaptureHelper.captureSilentPhoto(context, lifecycleOwner)
                        lastPhotoUri = uri
                    } catch (e: Exception) {
                        lastPhotoUri = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Tomar foto (prueba)")
            }
            lastPhotoUri?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (isAlarmPlaying) {
                    AlarmHelper.stopAlarm(context)
                    isAlarmPlaying = false
                } else {
                    AlarmHelper.playAlarm(context)
                    isAlarmPlaying = true
                }
            }) {
                Text(if (isAlarmPlaying) "Detener alarma" else "Sonar alarma")
            }
        }
    }
}