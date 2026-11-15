package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val creator: String,
    val viewerUrl: String,
    val thumbnailUrl: String,
    val categoryId: String,
    val searchTerm: String,
    val licenseLabel: String,
    val licenseDecision: String,
    val attributionRequirement: String,
    val canRedistributeOnGitHub: Boolean,
    val faceCount: Int,
    val vertexCount: Int,
    val animationCount: Int,
    val archiveSizeBytes: Long,
    val scoreTotal: Int,
    val scoreVisual: Int,
    val scoreMobile: Int,
    val scoreTexture: Int,
    val scorePoly: Int,
    val scoreLicense: Int,
    val scoreRelevance: Int,
    val scoreAnimation: Int,
    val status: String, // IntegrationStatus.name
    val downloadDateIso: String? = null,
    val originalZipPath: String? = null,
    val extractedDirPath: String? = null,
    val mainModelPath: String? = null,
    val optimizedDirPath: String? = null,
    val prefabName: String? = null,
    val boundGameSlot: String? = null,
    val qualityProfile: String = "MEDIUM",
    val failureReason: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
