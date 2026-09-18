
package com.DevG2Studios.seguromovilultima.data.remote.api

import com.DevG2Studios.seguromovilultima.data.remote.dto.CommandResponseDto
import com.DevG2Studios.seguromovilultima.data.remote.dto.FcmTokenDto
import com.DevG2Studios.seguromovilultima.data.remote.dto.LocationReportDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CommandApi {
    @GET("api/devices/{deviceId}/commands")
    suspend fun getPendingCommands(@Path("deviceId") deviceId: String): Response<List<CommandResponseDto>>

    @POST("api/devices/commands/{commandId}/ack")
    suspend fun acknowledgeCommand(@Path("commandId") commandId: Long): Response<Unit>

    @POST("api/devices/{deviceId}/location")
    suspend fun reportLocation(
        @Path("deviceId") deviceId: String,
        @Body location: LocationReportDto
    ): Response<Unit>

    // esto es para supabase reportar el comando del resultado
    @Multipart
    @POST("api/devices/commands/{commandId}/result")
    suspend fun reportResult(
        @Path("commandId") commandId: Long,
        @Part("deviceId") deviceId: okhttp3.RequestBody,
        @Part("success") success: okhttp3.RequestBody,
        @Part("errorMessage") errorMessage: okhttp3.RequestBody?,
        @Part file: okhttp3.MultipartBody.Part?
    ): Response<Unit>



    @POST("api/devices/{deviceId}/fcm-token")
    suspend fun updateFcmToken(
        @Path("deviceId") deviceId: String,
        @Body request: FcmTokenDto
    ): Response<Unit>
}