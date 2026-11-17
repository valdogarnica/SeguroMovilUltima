
package com.devg2studios.seguromovilultima.di

import android.util.Log
import com.devg2studios.seguromovilultima.data.remote.api.AuthApi
import com.devg2studios.seguromovilultima.data.remote.api.CommandApi
import com.devg2studios.seguromovilultima.data.remote.api.backup.MediaBackupApi
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
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    //  cambia esto por la URL real de tu backend cuando lo despliegues
    private const val BASE_URL = "https://www.seguromovil.app/"

    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseurl(): String = BASE_URL

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
            .writeTimeout(10, TimeUnit.MINUTES)   // tiempo para subir el archivo
            .readTimeout(10, TimeUnit.MINUTES)    // tiempo para esperar la respuesta del backend
            .callTimeout(15, TimeUnit.MINUTES)    // límite total de la llamada completa
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .eventListener(object : okhttp3.EventListener() {
                var t0 = 0L
                override fun callStart(call: okhttp3.Call) {
                    t0 = System.currentTimeMillis()
                }
                override fun dnsStart(call: okhttp3.Call, domainName: String) {
                    Log.d("NET_TIMING", "dnsStart +${System.currentTimeMillis()-t0}ms")
                }
                override fun dnsEnd(call: okhttp3.Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                    Log.d("NET_TIMING", "dnsEnd +${System.currentTimeMillis()-t0}ms addrs=$inetAddressList")
                }
                override fun connectStart(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
                    Log.d("NET_TIMING", "connectStart +${System.currentTimeMillis()-t0}ms -> $inetSocketAddress")
                }
                override fun connectFailed(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: okhttp3.Protocol?, ioe: java.io.IOException) {
                    Log.d("NET_TIMING", "connectFailed +${System.currentTimeMillis()-t0}ms -> $inetSocketAddress ($ioe)")
                }
                override fun secureConnectEnd(call: okhttp3.Call, handshake: okhttp3.Handshake?) {
                    Log.d("NET_TIMING", "tlsEnd +${System.currentTimeMillis()-t0}ms")
                }
                override fun responseHeadersStart(call: okhttp3.Call) {
                    Log.d("NET_TIMING", "responseHeadersStart +${System.currentTimeMillis()-t0}ms")
                }
                override fun callEnd(call: okhttp3.Call) {
                    Log.d("NET_TIMING", "callEnd +${System.currentTimeMillis()-t0}ms")
                }
            })
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

    @Provides
    @Singleton
    fun provideMediaBackupApi(retrofit: Retrofit): MediaBackupApi = retrofit.create(MediaBackupApi::class.java)
}