package com.devg2studios.seguromovilultima.ui.views.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devg2studios.seguromovilultima.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class HomeCardItem(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val route: String,
    val accentColor: Color
)

private fun emailInitials(email: String): String =
    email.takeIf { it.contains("@") }
        ?.substringBefore("@")
        ?.take(2)
        ?.uppercase()
        ?: "?"

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
            route = "failed_attempt_settings",
            accentColor = Color(0xFFEF5350)
        ),
        HomeCardItem(
            title = "Apagado falso",
            subtitle = "Simula que el celular se apagó",
            iconRes = R.drawable.apagar,
            route = "fake_shutdown_settings",
            accentColor = Color(0xFF5C6BC0)
        ),
        HomeCardItem(
            title = "Pruebas de funciones",
            subtitle = "Foto, alarma y ubicación manuales",
            iconRes = R.drawable.baseline_bolt_24,
            route = "debug_tools",
            accentColor = Color(0xFFFFA726)
        ),
        HomeCardItem(
            title = "Permisos y protección",
            subtitle = "Administrador del dispositivo, batería",
            iconRes = R.drawable.permisos,
            route = "setup",
            accentColor = Color(0xFF26A69A)
        )
    )

    var isLoggingOut by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isLoggingOut) {
        if (isLoggingOut) {
            delay(350.milliseconds)
            viewModel.logout()
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { HomeTitle() },
                actions = {
                    TopBarProfileButton(
                        email = viewModel.userEmail,
                        isAnimatingOut = isLoggingOut,
                        onClick = { showProfileSheet = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        // LazyColumn ya soporta scroll de forma nativa: al agregar más
        // HomeCardItem a la lista en el futuro, la pantalla se desplaza
        // automáticamente sin necesitar ningún cambio de código.
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                FeatureCard(item = item, onClick = { onNavigate(item.route) })
            }
            item {
                DeviceFooter(
                    deviceId = viewModel.deviceId,
                    appVersion = viewModel.appVersion
                )
            }
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState
        ) {
            ProfileSheetContent(
                email = viewModel.userEmail,
                deviceId = viewModel.deviceId,
                appVersion = viewModel.appVersion,
                onLogoutClick = {
                    scope.launch {
                        sheetState.hide()
                        showProfileSheet = false
                        isLoggingOut = true
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = buildAnnotatedTitle(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tu dispositivo está protegido",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun buildAnnotatedTitle() = buildAnnotatedString {
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
        append("Seguro")
    }
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
        append("Movil")
    }
}

@Composable
private fun TopBarProfileButton(
    email: String,
    isAnimatingOut: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )
    val exitRotation by animateFloatAsState(
        targetValue = if (isAnimatingOut) 90f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "exitRotation"
    )
    val exitScale by animateFloatAsState(
        targetValue = if (isAnimatingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "exitScale"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .scale(pressScale * exitScale)
            .rotate(exitRotation)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
            .clickable(
                enabled = !isAnimatingOut,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emailInitials(email),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ProfileSheetContent(
    email: String,
    deviceId: String,
    appVersion: String,
    onLogoutClick: () -> Unit
) {
    var isConfirmingLogout by remember { mutableStateOf(false) }

    LaunchedEffect(isConfirmingLogout) {
        if (isConfirmingLogout) {
            delay(200.milliseconds)
            onLogoutClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emailInitials(email),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = email,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cuenta protegida",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        InfoRow(
            icon = Icons.Filled.Phone,
            label = "ID del dispositivo",
            value = deviceId
        )
        Spacer(modifier = Modifier.height(16.dp))
        InfoRow(
            icon = Icons.Filled.Info,
            label = "Versión de la app",
            value = appVersion
        )

        Spacer(modifier = Modifier.height(28.dp))

        AnimatedLogoutFullButton(
            isConfirming = isConfirmingLogout,
            onClick = { isConfirmingLogout = true }
        )
    }
}

@Composable
private fun AnimatedLogoutFullButton(
    isConfirming: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "logoutPressScale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isConfirming) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "logoutIconRotation"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = !isConfirming,
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale),
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        if (isConfirming) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(iconRotation)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isConfirming) "Cerrando sesión..." else "Cerrar sesión")
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FeatureCard(item: HomeCardItem, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardPressScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra de acento lateral: identifica la categoría de un
            // vistazo, incluso antes de leer el ícono o el texto.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(item.accentColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        tint = item.accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceFooter(deviceId: String, appVersion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ID: $deviceId",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Versión $appVersion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}