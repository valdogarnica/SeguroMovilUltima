package com.devg2studios.seguromovilultima.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devg2studios.seguromovilultima.R

data class HomeCardItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconRes: Int,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val items = listOf(
        HomeCardItem(
            title = "Código de desbloqueo incorrecto",
            subtitle = "Configura qué pasa tras 3 intentos fallidos",
            iconRes = R.drawable.patron,
            route = "failed_attempt_settings"
        ),
        HomeCardItem(
            title = "Apagado falso",
            subtitle = "Simula que el celular se apagó",
            iconRes = R.drawable.apagar,
            route = "fake_shutdown_settings"
        ),
        HomeCardItem(
            title = "Pruebas de funciones",
            subtitle = "Foto, alarma y ubicación manuales",
            iconRes = R.drawable.baseline_bolt_24,
            route = "debug_tools"
        ),
        HomeCardItem(
            title = "Permisos y protección",
            subtitle = "Administrador del dispositivo, batería",
            iconRes = R.drawable.permisos,
            route = "setup"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SeguroMovilUltima") },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                FeatureCard(item = item, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun FeatureCard(item: HomeCardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.AddCircle, contentDescription = null)
        }
    }
}