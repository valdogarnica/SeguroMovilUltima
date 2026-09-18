
package com.DevG2Studios.seguromovilultima.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommandResponseDto(
    val id: Long,
    val type: String,
    val deviceId: String
)