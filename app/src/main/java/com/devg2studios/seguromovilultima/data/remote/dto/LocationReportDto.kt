package com.devg2studios.seguromovilultima.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationReportDto(
    val latitude: Double,
    val longitude: Double
)