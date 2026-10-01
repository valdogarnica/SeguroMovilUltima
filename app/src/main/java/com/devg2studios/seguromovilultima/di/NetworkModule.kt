
package com.devg2studios.seguromovilultima.di

import com.devg2studios.seguromovilultima.data.remote.api.AuthApi
import com.devg2studios.seguromovilultima.data.remote.api.CommandApi
import com.devg2studios.seguromovilultima.data.remote.api.envioEmail.SecurityIncidentApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    //  cambia esto por la URL real de tu backend cuando lo despliegues
    private const val BASE_URL = "http://192.168.1.12:8081/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // BODY intenta leer y loguear el cuerpo completo de cada petición,
            // lo cual para un archivo multipart de varios MB (foto/video) es
            // costoso y añade latencia innecesaria — HEADERS evita eso sin
            // perder visibilidad útil para depurar.
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)   // tiempo para subir el archivo
            .readTimeout(60, TimeUnit.SECONDS)    // tiempo para esperar la respuesta del backend
            .callTimeout(90, TimeUnit.SECONDS)    // límite total de la llamada completa
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideCommandApi(retrofit: Retrofit): CommandApi = retrofit.create(CommandApi::class.java)

    @Provides
    @Singleton
    fun provideSecurityIncidentApi(retrofit: Retrofit): SecurityIncidentApi = retrofit.create(SecurityIncidentApi::class.java)
}