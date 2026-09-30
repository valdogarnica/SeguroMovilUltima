package com.devg2studios.seguromovilultima.data.remote.dto.envioEmail

import kotlinx.serialization.Serializable

@Serializable
data class StartIncidentDto(
    val photoExpected: Boolean,
    val videoExpected: Boolean,
    val locationExpected: Boolean,
    val alarmTriggered: Boolean
)

@Serializable
data class IncidentIdResponseDto(
    val incidentId: Long
)

@Serializable
data class IncidentLocationDto(
    val latitude: Double,
    val longitude: Double
)