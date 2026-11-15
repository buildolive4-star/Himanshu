package com.example.data.sketchfab

import com.example.pipeline.AssetPipelineConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SketchfabApiService {

    @GET("v3/search")
    suspend fun searchModels(
        @Query("type") type: String = "models",
        @Query("q") query: String,
        @Query("downloadable") downloadable: Boolean = true,
        @Query("sort_by") sortBy: String = "-likeCount",
        @Query("count") count: Int = 18,
        @Query("cursor") cursor: String? = null,
        @Query("archives_flavours") archivesFlavours: Boolean = true
    ): Response<SketchfabSearchResponse>

    @GET("v3/models/{uid}")
    suspend fun getModelDetails(
        @Path("uid") uid: String
    ): Response<SketchfabModelDto>

    @GET("v3/models/{uid}/download")
    suspend fun getModelDownloadUrls(
        @Path("uid") uid: String
    ): Response<SketchfabDownloadResponse>

    @GET("v3/me")
    suspend fun getCurrentUser(): Response<SketchfabMeDto>
}

class SketchfabAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val authHeader = AssetPipelineConfig.authorizationHeader()
        val request = if (!authHeader.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .build()
        } else {
            original.newBuilder()
                .header("Accept", "application/json")
                .build()
        }
        return chain.proceed(request)
    }
}

object SketchfabNetworkModule {
    private const val BASE_URL = "https://api.sketchfab.com/"

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }
        OkHttpClient.Builder()
            .addInterceptor(SketchfabAuthInterceptor())
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: SketchfabApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SketchfabApiService::class.java)
    }
}
