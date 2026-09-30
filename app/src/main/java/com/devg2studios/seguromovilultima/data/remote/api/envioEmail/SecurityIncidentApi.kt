package com.devg2studios.seguromovilultima.data.remote.api.envioEmail

import com.devg2studios.seguromovilultima.data.remote.dto.envioEmail.IncidentIdResponseDto
import com.devg2studios.seguromovilultima.data.remote.dto.envioEmail.IncidentLocationDto
import com.devg2studios.seguromovilultima.data.remote.dto.envioEmail.StartIncidentDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SecurityIncidentApi {
    @POST("api/devices/{deviceId}/security-incident/start")
    suspend fun startIncident(
        @Path("deviceId") deviceId: String,
        @Body request: StartIncidentDto
    ): Response<IncidentIdResponseDto>

    @Multipart
    @POST("api/devices/security-incident/{incidentId}/photo")
    suspend fun reportPhoto(
        @Path("incidentId") incidentId: Long,
        @Part("deviceId") deviceId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @Multipart
    @POST("api/devices/security-incident/{incidentId}/video")
    suspend fun reportVideo(
        @Path("incidentId") incidentId: Long,
        @Part("deviceId") deviceId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @POST("api/devices/security-incident/{incidentId}/location")
    suspend fun reportLocation(
        @Path("incidentId") incidentId: Long,
        @Body location: IncidentLocationDto
    ): Response<Unit>
}