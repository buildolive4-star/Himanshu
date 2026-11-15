package com.example.data.sketchfab

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SketchfabOEmbedResponse(
    val title: String = "",
    @Json(name = "author_name") val authorName: String = "",
    @Json(name = "author_url") val authorUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "thumbnail_width") val thumbnailWidth: Int = 640,
    @Json(name = "thumbnail_height") val thumbnailHeight: Int = 360,
    val width: Int = 640,
    val height: Int = 360,
    val html: String = "",
    @Json(name = "provider_name") val providerName: String = "Sketchfab",
    val type: String = "rich"
) {
    fun extractIframeSrc(): String? {
        val regex = Regex("""src="([^"]+)"""")
        return regex.find(html)?.groupValues?.getOrNull(1)
    }
}

interface SketchfabOEmbedApi {
    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
        @Query("maxwidth") maxWidth: Int = 640,
        @Query("maxheight") maxHeight: Int = 360
    ): Response<SketchfabOEmbedResponse>
}

object SketchfabOEmbedClient {
    val api: SketchfabOEmbedApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://sketchfab.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SketchfabOEmbedApi::class.java)
    }

    fun normalizeModelUrl(uidOrUrl: String): String {
        val trimmed = uidOrUrl.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://sketchfab.com/models/$trimmed"
        }
    }
}
