package com.DevG2Studios.seguromovilultima.data.remote.api

import com.DevG2Studios.seguromovilultima.data.remote.dto.LoginRequestDto
import com.DevG2Studios.seguromovilultima.data.remote.dto.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<Map<String, String>>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<Map<String, String>>
}