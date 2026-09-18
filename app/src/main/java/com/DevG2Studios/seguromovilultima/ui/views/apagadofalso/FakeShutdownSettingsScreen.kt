
package com.DevG2Studios.seguromovilultima.ui.views.apagadoFalso

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
fun FakeShutdownSettingsScreen(
    onBack: () -> Unit,
    viewModel: FakeShutdownSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apagado falso") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text(
                "Cuando alguien intente apagar el dispositivo mientras está bloqueado, se simulará un apagado real.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            SwitchRow(
                title = "Activar apagado falso",
                checked = settings.fakeShutdownEnabled,
                onCheckedChange = viewModel::onFakeShutdownEnabledChanged
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Al detectarse el intento, además de simular el apagado:",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            SwitchRow(
                title = "Tomar foto",
                checked = settings.fakeShutdownSendPhoto,
                enabled = settings.fakeShutdownEnabled,
                onCheckedChange = viewModel::onSendPhotoChanged
            )
            HorizontalDivider()

            SwitchRow(
                title = "Grabar video (10 segundos)",
                checked = settings.fakeShutdownSendVideo,
                enabled = settings.fakeShutdownEnabled,
                onCheckedChange = viewModel::onSendVideoChanged
            )
            HorizontalDivider()

            SwitchRow(
                title = "Enviar ubicación",
                checked = settings.fakeShutdownSendLocation,
                enabled = settings.fakeShutdownEnabled,
                onCheckedChange = viewModel::onSendLocationChanged
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}