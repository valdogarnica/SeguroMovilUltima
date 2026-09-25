package com.DevG2Studios.seguromovilultima.data.remote.ws

import kotlinx.serialization.Serializable

@Serializable
data class ScreenFrameDto(
    val deviceId: String,
    val imageBase64: String,
    val timestamp: Long,
    val realWidth: Int,
    val realHeight: Int
)

@Serializable
data class TouchCommandDto(
    val deviceId: String,
    val action: String, // "TAP" o "SWIPE"
    val x: Float,
    val y: Float,
    val endX: Float = 0f,
    val endY: Float = 0f
)

@Serializable
data class RemoteControlToggleDto(
    val deviceId: String,
    val touchEnabled: Boolean
)