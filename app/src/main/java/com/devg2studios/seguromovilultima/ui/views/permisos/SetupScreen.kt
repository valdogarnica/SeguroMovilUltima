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
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.devg2studios.seguromovilultima.admin.SecurityDeviceAdminReceiver
import com.devg2studios.seguromovilultima.services.BatteryOptimizationHelper
import com.devg2studios.seguromovilultima.services.LocationForegroundService
import com.devg2studios.seguromovilultima.service.codigoIncorrecto.VideoCaptureHelper
import com.devg2studios.seguromovilultima.ui.debug.DebugToolsViewModel
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.devg2studios.seguromovilultima.utils.ServiceUtils


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

    // --- NUEVO: acceso a notificaciones (para NotificationListenerService) ---
    var hasNotificationListenerAccess by remember {
        mutableStateOf(XiaomiPermissionHelper.isNotificationListenerEnabled(context))
    }
    val notificationListenerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasNotificationListenerAccess = XiaomiPermissionHelper.isNotificationListenerEnabled(context)
    }


   /* val lifecycleOwnerForProtection = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwnerForProtection) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isProtectionActive = ProtectionManager.isActuallyRunning(context)
            }
        }
        lifecycleOwnerForProtection.lifecycle.addObserver(observer)
        onDispose { lifecycleOwnerForProtection.lifecycle.removeObserver(observer) }
    }*/

    // Agrega este estado junto a los demás
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Notificacion", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!hasNotificationPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasNotificationPermission) "Notificaciones permitidas ✓" else "Permitir notificaciones")
                }
            }

            // --- NUEVO: acceso a notificaciones, junto a lo demás de notificaciones ---
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!hasNotificationListenerAccess) {
                        notificationListenerLauncher.launch(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasNotificationListenerAccess) "Acceso a notificaciones ✓" else "Permitir acceso a notificaciones")
            }
            Text(
                "Necesario para detectar si el usuario elimina la notificación de sincronización y reiniciarla.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))
            /*Button(
                onClick = {
                    if (!hasFineLocation) return@Button
                    if (isProtectionActive) {
                        context.stopService(Intent(context, LocationForegroundService::class.java))
                        isProtectionActive = false
                        viewModel.setLocationTrackingEnabled(false)
                    } else {
                        context.startForegroundService(Intent(context, LocationForegroundService::class.java))
                        isProtectionActive = true
                        viewModel.setLocationTrackingEnabled(true)
                    }
                },
                enabled = hasFineLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isProtectionActive) "Proteccion Activa ✓" else "Proteccion Inactiva")
            }*/
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protección activa",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (isProtectionActive) "Monitoreando el dispositivo" else "Desactivada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isProtectionActive,
                    onCheckedChange = { checked ->
                        if (!hasFineLocation) return@Switch

                        val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
                        if (checked) {
                            context.startForegroundService(Intent(context, LocationForegroundService::class.java))
                            prefs.edit().putBoolean("protection_enabled", true).apply()
                        } else {
                            context.stopService(Intent(context, LocationForegroundService::class.java))
                            prefs.edit().putBoolean("protection_enabled", false).apply()
                        }
                        isProtectionActive = checked
                        viewModel.setLocationTrackingEnabled(checked)
                    },
                    enabled = hasFineLocation
                )
            }

            Text("General", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isAdminActive) "Administrador activo ✓" else "Activar administrador del dispositivo")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!isIgnoringBatteryOpt) {
                        batteryOptLauncher.launch(BatteryOptimizationHelper.buildRequestIntent(context))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isIgnoringBatteryOpt) "Segundo plano permitido ✓" else "Ejecutar siempre en segundo plano")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!hasAudioPermission) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasAudioPermission) "Micrófono permitido ✓" else "Permitir grabar audio en videos")
            }
            // la funcion heurfana
            Spacer(modifier = Modifier.height(12.dp))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(
                    onClick = {
                        if (!hasExactAlarmPermission) {
                            exactAlarmLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasExactAlarmPermission) "Alarmas exactas permitidas ✓" else "Permitir alarmas exactas")
                }
                Text(
                    "Necesario para que el watchdog reinicie el servicio de forma puntual, incluso en reposo profundo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Ubicación, cámara y notificaciones", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!hasFineLocation) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasFineLocation) "Ubicación permitida ✓" else "Permitir ubicación")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!hasBackgroundLocation && hasFineLocation) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                    enabled = hasFineLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (hasBackgroundLocation) "Ubicación en segundo plano ✓" else "Permitir ubicación en segundo plano")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasCameraPermission) "Cámara permitida ✓" else "Permitir cámara")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Apagado falso", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isAccessibilityEnabled) {
                        accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isAccessibilityEnabled) "Accesibilidad activa ✓" else "Activar servicio de accesibilidad")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!canDrawOverlays) {
                        overlayLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (canDrawOverlays) "Mostrar sobre otras apps ✓" else "Permitir mostrarse sobre otras apps")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!canWriteSettings) {
                        writeSettingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                "package:${context.packageName}".toUri()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (canWriteSettings) "Control de brillo ✓" else "Permitir modificar ajustes del sistema")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!hasDndAccess) {
                        dndLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasDndAccess) "Acceso a No molestar ✓" else "Permitir controlar No molestar")
            }

            Spacer(modifier = Modifier.height(24.dp))
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

            Button(
                onClick = {
                    if (!canUseFullScreenIntent && Build.VERSION.SDK_INT >= 34) {
                        fullScreenIntentLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (canUseFullScreenIntent) "Pantalla completa permitida ✓" else "Permitir notificaciones de pantalla completa")
            }

            // --- Sección Xiaomi ---
            val isXiaomi = remember { XiaomiPermissionHelper.isXiaomiDevice() }

            if (isXiaomi) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                Text("Ajustes específicos de Xiaomi", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Necesarios para que el servicio no sea cerrado por el sistema.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                            // refresca también el acceso a notificaciones al volver de cualquier ajuste
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

                Button(
                    onClick = {
                        wentToSettings = true
                        XiaomiPermissionHelper.openAutoStartSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (autoStartDone) "Inicio automático ✓" else "Activar inicio automático")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!isIgnoringBatteryOpt) {
                            batteryOptLauncher.launch(
                                XiaomiPermissionHelper.requestIgnoreBatteryOptimizations(context)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isIgnoringBatteryOpt) "Batería sin restricciones ✓" else "Poner batería en 'Sin restricciones'")
                }
                // --- NUEVO: batería específica de Xiaomi (pantalla nativa MIUI, distinta de la de Android) ---
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { XiaomiPermissionHelper.openXiaomiAppBatterySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ajustar batería en MIUI (avanzado)")
                }
                Text(
                    "Abre el control de batería propio de Xiaomi para esta app. Selecciona 'Sin restricciones'.",
                    style = MaterialTheme.typography.bodySmall
                )

// --- NUEVO: desactivar optimización de MIUI ---
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { XiaomiPermissionHelper.openMiuiOptimizationSettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Desactivar optimización de MIUI")
                }
                Text(
                    "Activa Opciones de desarrollador si no aparece, luego busca 'Desactivar optimización de MIUI' y actívalo. Requiere reiniciar el teléfono.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Bloquear en Recientes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Abre la app, ve a Recientes (□), desliza esta app hacia abajo " +
                                    "y toca el ícono de candado para bloquearla. Esto no se puede " +
                                    "activar por código, es un paso manual del usuario.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
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