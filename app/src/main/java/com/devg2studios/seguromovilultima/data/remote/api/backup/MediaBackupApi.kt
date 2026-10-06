package com.devg2studios.seguromovilultima.data.remote.api.backup

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MediaBackupApi {
    @Multipart
    @POST("api/devices/{deviceId}/media-backup")
    suspend fun uploadBackup(
        @Path("deviceId") deviceId: String,
        @Part file: MultipartBody.Part,
        @Part("sourceFolder") sourceFolder: RequestBody,
        @Part("mediaType") mediaType: RequestBody,
        @Part("takenAtEpochMillis") takenAtEpochMillis: RequestBody
    ): Response<Unit>

    @Multipart
    @POST("api/devices/{deviceId}/media-backup/chunk")
    suspend fun uploadBackupChunk(
        @Path("deviceId") deviceId: String,
        @Part file: MultipartBody.Part,
        @Part("mediaStoreId") mediaStoreId: RequestBody,
        @Part("parte") parte: RequestBody,
        @Part("totalPartes") totalPartes: RequestBody,
        @Part("sourceFolder") sourceFolder: RequestBody,
        @Part("takenAtEpochMillis") takenAtEpochMillis: RequestBody
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/devices/{deviceId}/media-backup/complete")
    suspend fun completeBackupChunks(
        @Path("deviceId") deviceId: String,
        @Field("mediaStoreId") mediaStoreId: Long,
        @Field("totalPartes") totalPartes: Int,
        @Field("sourceFolder") sourceFolder: String,
        @Field("mediaType") mediaType: String,
        @Field("takenAtEpochMillis") takenAtEpochMillis: Long,
        @Field("fileSizeBytes") fileSizeBytes: Long
    ): Response<Unit>
}