package com.example.data.sketchfab

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SketchfabSearchResponse(
    val results: List<SketchfabModelDto> = emptyList(),
    val next: String? = null,
    val previous: String? = null,
    val cursors: SketchfabCursorsDto? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabCursorsDto(
    val next: String? = null,
    val previous: String? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabModelDto(
    val uid: String,
    val name: String = "Untitled Model",
    val viewerUrl: String? = null,
    val uri: String? = null,
    val description: String? = null,
    val faceCount: Int = 0,
    val vertexCount: Int = 0,
    val animationCount: Int = 0,
    val isDownloadable: Boolean = false,
    val isAgeRestricted: Boolean = false,
    val isPublished: Boolean = true,
    val staffpickedAt: String? = null,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val downloadCount: Int = 0,
    val user: SketchfabUserDto? = null,
    val license: SketchfabLicenseDto? = null,
    val thumbnails: SketchfabThumbnailsDto? = null,
    val tags: List<SketchfabTagDto> = emptyList(),
    val categories: List<SketchfabCategoryDto> = emptyList(),
    val archives: SketchfabArchivesDto? = null
) {
    fun bestThumbnailUrl(): String? {
        val images = thumbnails?.images.orEmpty()
        return images.sortedBy { kotlin.math.abs(it.width - 512) }.firstOrNull()?.url
    }

    fun gltfArchiveSizeBytes(): Long {
        return archives?.gltf?.size ?: 0L
    }

    fun gltfTextureCount(): Int {
        return archives?.gltf?.textureCount ?: 0
    }

    fun gltfTextureMaxRes(): Int {
        return archives?.gltf?.textureMaxResolution ?: 0
    }
}

@JsonClass(generateAdapter = true)
data class SketchfabUserDto(
    val uid: String? = null,
    val username: String = "unknown",
    val displayName: String? = null,
    val profileUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabLicenseDto(
    val uid: String? = null,
    val label: String? = null,
    val slug: String? = null,
    val url: String? = null,
    val requirements: String? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabThumbnailsDto(
    val images: List<SketchfabImageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SketchfabImageDto(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SketchfabTagDto(
    val name: String = "",
    val slug: String = ""
)

@JsonClass(generateAdapter = true)
data class SketchfabCategoryDto(
    val name: String = "",
    val slug: String = ""
)

@JsonClass(generateAdapter = true)
data class SketchfabArchivesDto(
    val gltf: SketchfabArchiveInfoDto? = null,
    val glb: SketchfabArchiveInfoDto? = null,
    val usdz: SketchfabArchiveInfoDto? = null,
    val source: SketchfabArchiveInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabArchiveInfoDto(
    val size: Long = 0L,
    val faceCount: Int? = null,
    val vertexCount: Int? = null,
    val textureCount: Int? = null,
    val textureMaxResolution: Int? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabDownloadResponse(
    val gltf: SketchfabDownloadEntryDto? = null,
    val glb: SketchfabDownloadEntryDto? = null,
    val usdz: SketchfabDownloadEntryDto? = null,
    val source: SketchfabDownloadEntryDto? = null
)

@JsonClass(generateAdapter = true)
data class SketchfabDownloadEntryDto(
    val url: String = "",
    val size: Long = 0L,
    val expires: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SketchfabMeDto(
    val uid: String = "",
    val username: String = "",
    val displayName: String? = null,
    val account: String? = null
)
