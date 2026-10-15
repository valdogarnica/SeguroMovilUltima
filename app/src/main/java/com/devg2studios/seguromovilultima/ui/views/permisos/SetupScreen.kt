package com.devg2studios.seguromovilultima.ui.views.permisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.devg2studios.seguromovilultima.admin.SecurityDeviceAdminReceiver
import com.devg2studios.seguromovilultima.services.BatteryOptimizationHelper
import com.devg2studios.seguromovilultima.services.LocationForegroundService
import com.devg2studios.seguromovilultima.service.codigoIncorrecto.VideoCaptureHelper
import com.devg2studios.seguromovilultima.ui.debug.DebugToolsViewModel
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.devg2studios.seguromovilultima.services.backup.MediaBackupService
import com.devg2studios.seguromovilultima.utils.ServiceUtils
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    viewModel: DebugToolsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // --- Permisos generales (código incorrecto) ---
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
    var isAdminActive by remember { mutableStateOf(devicePolicyManager.isAdminActive(adminComponent)) }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
    }

    var isIgnoringBatteryOpt by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    val batteryOptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isIgnoringBatteryOpt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    var hasAudioPermission by remember {
        mutableStateOf(VideoCaptureHelper.hasAudioPermission(context))
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // --- Permisos de apagado falso ---
    var isAccessibilityEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
    }

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlays = Settings.canDrawOverlays(context)
    }

    var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canWriteSettings = Settings.System.canWrite(context)
    }

    var hasDndAccess by remember {
        mutableStateOf(
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .isNotificationPolicyAccessGranted
        )
    }
    val dndLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasDndAccess = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted
    }

    // --- Permisos runtime de ubicación, cámara y notificaciones ---
    var hasFineLocation by remember {
        mutableStateOf(isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                isPermissionGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else true
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(isPermissionGranted(context, Manifest.permission.CAMERA))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
            else true
        )
    }
    // --- Permiso de medios para el respaldo de fotos/videos ---
    var hasMediaPermission by remember {
        mutableStateOf(hasMediaReadPermission(context))
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMediaPermission = results.values.all { it }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasFineLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // --- Estado del rastreo de ubicación continuo ---
    var isProtectionActive by remember {
        mutableStateOf(ServiceUtils.isServiceRunning(context, LocationForegroundService::class.java))
    }

    // --- Acceso a notificaciones (para NotificationListenerService) ---
    var hasNotificationListenerAccess by remember {
        mutableStateOf(XiaomiPermissionHelper.isNotificationListenerEnabled(context))
    }
    val notificationListenerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasNotificationListenerAccess = XiaomiPermissionHelper.isNotificationListenerEnabled(context)
    }

    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(AlarmManager::class.java)).canScheduleExactAlarms()
            } else true
        )
    }
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasExactAlarmPermission = (context.getSystemService(AlarmManager::class.java)).canScheduleExactAlarms()
        }
    }

    var canUseFullScreenIntent by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 34) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .canUseFullScreenIntent()
            } else true
        )
    }
    val fullScreenIntentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= 34) {
            canUseFullScreenIntent = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .canUseFullScreenIntent()
        }
    }

    val isXiaomi = remember { XiaomiPermissionHelper.isXiaomiDevice() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permisos y protección") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionHeader("Protección", Color(0xFF26A69A))
            var mediaBackupEnabled by remember {
                mutableStateOf(com.devg2studios.seguromovilultima.data.local.SecuritySettingsCache.current.mediaBackupEnabled)
            }
            var mediaBackupWifiOnly by remember {
                mutableStateOf(com.devg2studios.seguromovilultima.data.local.SecuritySettingsCache.current.mediaBackupWifiOnly)
            }

            ProtectionToggleCard(
                isActive = isProtectionActive,
                enabled = hasFineLocation,
                onCheckedChange = { checked ->
                    if (!hasFineLocation) return@ProtectionToggleCard
                    val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
                    if (checked) {
                        context.startForegroundService(Intent(context, LocationForegroundService::class.java))
                        prefs.edit { putBoolean("protection_enabled", true) }
                    } else {
                        context.stopService(Intent(context, LocationForegroundService::class.java))
                        prefs.edit { putBoolean("protection_enabled", false) }
                    }
                    isProtectionActive = checked
                    viewModel.setLocationTrackingEnabled(checked)

                    // AQUI LA COPIA DE SEGURIDAD
                    if(hasMediaPermission) {
                        mediaBackupEnabled = checked
                        viewModel.setMediaBackupEnabled(checked)
                        if (checked) {
                            context.startForegroundService(Intent(context, MediaBackupService::class.java))
                        } else {
                            context.stopService(Intent(context, MediaBackupService::class.java))
                        }
                    }
                }
            )
            // botones para fotos usando datos moviles o wifi
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wi-Fi y datos móviles",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    //Text(
                    //    text = "",
                    //    style = MaterialTheme.typography.bodySmall,
                    //    color = MaterialTheme.colorScheme.onSurfaceVariant
                    //)
                }
                Switch(
                    checked = !mediaBackupWifiOnly,
                    onCheckedChange = { checked ->
                        mediaBackupWifiOnly = !checked
                        viewModel.setMediaBackupWifiOnly(!checked)
                    },
                    enabled = mediaBackupEnabled
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Notificaciones", Color(0xFF5C6BC0))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    title = "Notificaciones",
                    description = "Permite mostrar el estado de sincronización",
                    isGranted = hasNotificationPermission,
                    accentColor = Color(0xFF5C6BC0),
                    onClick = {
                        if (!hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            PermissionCard(
                title = "Acceso a notificaciones",
                description = "Detecta si eliminan la notificación de sincronización y la reinicia",
                isGranted = hasNotificationListenerAccess,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!hasNotificationListenerAccess) {
                        notificationListenerLauncher.launch(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("General", Color(0xFFEF5350))

            PermissionCard(
                title = "Administrador del dispositivo",
                description = "Detecta intentos de desbloqueo fallidos",
                isGranted = isAdminActive,
                accentColor = Color(0xFFEF5350),
                onClick = {
                    if (!isAdminActive) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "SeguroMovilUltima necesita este permiso para detectar intentos de desbloqueo fallidos."
                            )
                        }
                        adminLauncher.launch(intent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Segundo plano",
                description = "Ejecutar siempre, sin restricciones de batería",
                isGranted = isIgnoringBatteryOpt,
                accentColor = Color(0xFFEF5350),
                onClick = {
                    if (!isIgnoringBatteryOpt) {
                        batteryOptLauncher.launch(BatteryOptimizationHelper.buildRequestIntent(context))
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Micrófono",
                description = "Permite grabar audio en videos",
                isGranted = hasAudioPermission,
                accentColor = Color(0xFFEF5350),
                onClick = {
                    if (!hasAudioPermission) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(10.dp))
                PermissionCard(
                    title = "Alarmas exactas",
                    description = "El watchdog reinicia el servicio de forma puntual, incluso en reposo profundo",
                    isGranted = hasExactAlarmPermission,
                    accentColor = Color(0xFFEF5350),
                    onClick = {
                        if (!hasExactAlarmPermission) {
                            exactAlarmLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Ubicación y cámara", Color(0xFFFFA726))

            PermissionCard(
                title = "Ubicación",
                description = "Necesaria para localizar el dispositivo",
                isGranted = hasFineLocation,
                accentColor = Color(0xFFFFA726),
                onClick = {
                    if (!hasFineLocation) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(modifier = Modifier.height(10.dp))
                PermissionCard(
                    title = "Ubicación en segundo plano",
                    description = "Permite rastrear aunque la app no esté abierta",
                    isGranted = hasBackgroundLocation,
                    enabled = hasFineLocation,
                    accentColor = Color(0xFFFFA726),
                    onClick = {
                        if (!hasBackgroundLocation && hasFineLocation) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Cámara",
                description = "Necesaria para fotos y video remotos",
                isGranted = hasCameraPermission,
                accentColor = Color(0xFFFFA726),
                onClick = {
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Acceso a fotos y videos",
                description = "Necesario para respaldar tu galería, incluyendo WhatsApp y otras apps",
                isGranted = hasMediaPermission,
                accentColor = Color(0xFFFFA726),
                onClick = {
                    if (!hasMediaPermission) {
                        mediaPermissionLauncher.launch(mediaPermissionsToRequest())
                    }
                }
            )

            var hasAllFilesAccess by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true
                )
            }
            val allFilesAccessLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hasAllFilesAccess = Environment.isExternalStorageManager()
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Acceso a todos los archivos",
                description = "Necesario para respaldar los estados y archivos privados de WhatsApp (no indexados por el sistema)",
                isGranted = hasAllFilesAccess,
                accentColor = Color(0xFFFFA726),
                onClick = {
                    if (!hasAllFilesAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        allFilesAccessLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Apagado falso", Color(0xFF5C6BC0))

            PermissionCard(
                title = "Servicio de accesibilidad",
                description = "Detecta el menú de apagado real",
                isGranted = isAccessibilityEnabled,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!isAccessibilityEnabled) {
                        accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Mostrar sobre otras apps",
                description = "Dibuja el overlay del apagado falso",
                isGranted = canDrawOverlays,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!canDrawOverlays) {
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Control de brillo",
                description = "Modifica ajustes del sistema para bajar el brillo",
                isGranted = canWriteSettings,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!canWriteSettings) {
                        writeSettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Acceso a No molestar",
                description = "Silencia notificaciones durante el apagado falso",
                isGranted = hasDndAccess,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!hasDndAccess) {
                        dndLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PermissionCard(
                title = "Pantalla completa",
                description = "Permite lanzar comandos con la pantalla bloqueada",
                isGranted = canUseFullScreenIntent,
                accentColor = Color(0xFF5C6BC0),
                onClick = {
                    if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= 34) {
                        fullScreenIntentLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    }
                }
            )
            if (isXiaomi) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader("Ajustes específicos de Xiaomi", Color(0xFFEF5350))
                Text(
                    "Necesarios para que el servicio no sea cerrado por el sistema.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                var autoStartDone by remember {
                    mutableStateOf(
                        XiaomiPermissionHelper.isAutoStartEnabled(context)
                            ?: XiaomiPermissionHelper.isAutoStartConfirmed(context)
                    )
                }
                var showConfirmDialog by remember { mutableStateOf(false) }
                var wentToSettings by remember { mutableStateOf(false) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (wentToSettings) {
                                wentToSettings = false
                                val detected = XiaomiPermissionHelper.isAutoStartEnabled(context)
                                if (detected != null) {
                                    autoStartDone = detected
                                } else {
                                    showConfirmDialog = true
                                }
                            }
                            hasNotificationListenerAccess =
                                XiaomiPermissionHelper.isNotificationListenerEnabled(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                if (showConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("¿Activaste el inicio automático?") },
                        text = { Text("No podemos verificarlo automáticamente en este dispositivo.") },
                        confirmButton = {
                            TextButton(onClick = {
                                XiaomiPermissionHelper.setAutoStartConfirmed(context, true)
                                autoStartDone = true
                                showConfirmDialog = false
                            }) { Text("Sí, ya lo activé") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) { Text("No") }
                        }
                    )
                }

                PermissionCard(
                    title = "Inicio automático",
                    description = "Permite que la app arranque sola tras reiniciar",
                    isGranted = autoStartDone,
                    accentColor = Color(0xFFEF5350),
                    onClick = {
                        wentToSettings = true
                        XiaomiPermissionHelper.openAutoStartSettings(context)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PermissionCard(
                    title = "Batería sin restricciones",
                    description = "Evita que MIUI cierre el servicio",
                    isGranted = isIgnoringBatteryOpt,
                    accentColor = Color(0xFFEF5350),
                    onClick = {
                        if (!isIgnoringBatteryOpt) {
                            batteryOptLauncher.launch(
                                XiaomiPermissionHelper.requestIgnoreBatteryOptimizations(context)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionCard(
                    title = "Ajustar batería en MIUI (avanzado)",
                    description = "Abre el control propio de Xiaomi. Selecciona 'Sin restricciones'.",
                    accentColor = Color(0xFFEF5350),
                    onClick = { XiaomiPermissionHelper.openXiaomiAppBatterySettings(context) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionCard(
                    title = "Desactivar optimización de MIUI",
                    description = "Activa Opciones de desarrollador si no aparece, luego busca esta opción y actívala. Requiere reiniciar el teléfono.",
                    accentColor = Color(0xFFEF5350),
                    onClick = { XiaomiPermissionHelper.openMiuiOptimizationSettings(context) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                InfoNoteCard(
                    title = "Bloquear en Recientes",
                    description = "Abre la app, ve a Recientes (□), desliza esta app hacia abajo y toca el ícono de candado para bloquearla. Esto no se puede activar por código, es un paso manual del usuario."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun StatusChip(isGranted: Boolean) {
    val bg = if (isGranted) Color(0xFF26A69A).copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (isGranted) Color(0xFF26A69A)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        if (isGranted) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = if (isGranted) "Activo" else "Activar",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        (if (isGranted) Color(0xFF26A69A) else accentColor).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Info,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF26A69A) else accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(isGranted = isGranted)
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoNoteCard(title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProtectionToggleCard(
    isActive: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF26A69A).copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF26A69A).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Check else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Protección activa",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isActive) "Monitoreando el dispositivo" else "Desactivada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF26A69A))
            )
        }
    }
}

fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    }
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val expectedComponentName = ComponentName(
        context.packageName,
        "${context.packageName}.services.apagadoFalso.ShutdownMenuAccessibilityService"
    ).flattenToString()

    val colonSplitter = enabledServices.split(":")
    return colonSplitter.any { it.equals(expectedComponentName, ignoreCase = true) }
}
private fun hasMediaReadPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        isPermissionGranted(context, Manifest.permission.READ_MEDIA_IMAGES) &&
                isPermissionGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun mediaPermissionsToRequest(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}