package com.devg2studios.seguromovilultima.ui.views.codigoIncorrecto

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedAttemptSettingsScreen(
    onBack: () -> Unit,
    viewModel: FailedAttemptSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val masterEnabled = settings.failedAttemptDetectionEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Código de desbloqueo incorrecto") },
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
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Cuando se detecten 3 intentos de desbloqueo fallidos, se ejecutarán las acciones que actives aquí.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingSwitchRow(
                title = "Activar detección de intentos fallidos de desbloqueo",
                checked = masterEnabled,
                onCheckedChange = viewModel::onFailedAttemptEnabledChanged
            )
            HorizontalDivider()

            SettingSwitchRow(
                title = "Enviar ubicación",
                checked = settings.sendLocationOnFailedAttempt,
                enabled = masterEnabled,
                onCheckedChange = viewModel::onSendLocationChanged
            )
            HorizontalDivider()

            SettingSwitchRow(
                title = "Enviar foto",
                checked = settings.sendPhotoOnFailedAttempt,
                enabled = masterEnabled,
                onCheckedChange = viewModel::onSendPhotoChanged
            )
            HorizontalDivider()

            SettingSwitchRow(
                title = "Grabar video (10 segundos)",
                checked = settings.sendVideoOnFailedAttempt,
                enabled = masterEnabled,
                onCheckedChange = viewModel::onSendVideoChanged
            )
            HorizontalDivider()

            SettingSwitchRow(
                title = "Iniciar alarma",
                checked = settings.startAlarmOnFailedAttempt,
                enabled = masterEnabled,
                onCheckedChange = viewModel::onStartAlarmChanged
            )
            HorizontalDivider()

            SettingSwitchRow(
                title = "Pantalla blanca con brillo máximo",
                subtitle = "Ayuda a iluminar la escena para la foto/video",
                checked = settings.showFlashScreenOnAlarm,
                enabled = masterEnabled && settings.startAlarmOnFailedAttempt,
                onCheckedChange = viewModel::onShowFlashScreenChanged
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            subtitle?.let {
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}