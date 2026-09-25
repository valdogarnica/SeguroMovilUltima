
package com.devg2studios.seguromovilultima.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommandResponseDto(
    val id: Long,
    val type: String,
    val deviceId: String
)