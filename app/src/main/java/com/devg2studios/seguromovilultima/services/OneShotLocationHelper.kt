package com.devg2studios.seguromovilultima.services.codigoIncorrecto

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

object OneShotLocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): DeviceLocation =
        suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            client.getCurrentLocation(request, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(
                            DeviceLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        continuation.resumeWithException(
                            IllegalStateException("Ubicación no disponible (GPS podría estar apagado)")
                        )
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
}