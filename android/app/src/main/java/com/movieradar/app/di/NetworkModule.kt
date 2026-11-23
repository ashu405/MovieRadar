package com.movieradar.app.di

import com.movieradar.app.BuildConfig
import com.movieradar.app.data.remote.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("X-App-Token", BuildConfig.APP_TOKEN).build())
        })
        .addInterceptor(RetryOnNetworkError(maxRetries = 2))
        .build()

    @Provides @Singleton
    fun tmdbApi(client: OkHttpClient, json: Json): TmdbApi {
        val base = BuildConfig.PROXY_BASE_URL.ifBlank { "https://proxy-not-configured.invalid/" }
            .let { if (it.endsWith("/")) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }
}

/** Retries idempotent GETs on connection resets / timeouts and on proxy 502s. */
class RetryOnNetworkError(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val req = chain.request()
        var attempt = 0
        while (true) {
            try {
                val res = chain.proceed(req)
                if (res.code != 502 || req.method != "GET" || attempt >= maxRetries) return res
                res.close()
            } catch (e: IOException) {
                if (req.method != "GET" || attempt >= maxRetries) throw e
            }
            attempt++
            Thread.sleep(300L * attempt)
        }
    }
}
