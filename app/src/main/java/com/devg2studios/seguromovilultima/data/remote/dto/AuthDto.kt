package com.devg2studios.seguromovilultima.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceName: String?
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)