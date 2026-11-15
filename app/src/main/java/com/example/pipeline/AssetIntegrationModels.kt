package com.example.pipeline

import com.example.data.sketchfab.SearchCategory
import com.example.data.sketchfab.SketchfabModelDto

enum class IntegrationStatus(val stepOrder: Int, val label: String) {
    DISCOVERED(1, "Discovered"),
    LICENSE_VERIFIED(2, "License Verified"),
    DOWNLOAD_REQUESTED(3, "Download Requested"),
    DOWNLOADED(4, "Downloaded"),
    EXTRACTED(5, "Extracted"),
    OPTIMIZED(6, "Optimized"),
    IMPORTED(7, "Imported"),
    PREFAB_CREATED(8, "Prefab Created"),
    GAME_INTEGRATED(9, "Game Integrated"),
    TESTED(10, "Tested"),
    FAILED(0, "Failed")
}

enum class QualityProfile(
    val displayName: String,
    val maxTexturePx: Int,
    val maxTrianglesPerProp: Int,
    val lodLevels: Int,
    val shadowQuality: String,
    val materialMode: String
) {
    LOW(
        displayName = "Low (512px)",
        maxTexturePx = 512,
        maxTrianglesPerProp = 8_000,
        lodLevels = 2,
        shadowQuality = "Blob / Reduced",
        materialMode = "Simplified Mobile Lit"
    ),
    MEDIUM(
        displayName = "Medium (1024px)",
        maxTexturePx = 1024,
        maxTrianglesPerProp = 22_000,
        lodLevels = 3,
        shadowQuality = "Balanced Cascade",
        materialMode = "Standard Mobile PBR"
    ),
    HIGH(
        displayName = "High (2048px)",
        maxTexturePx = 2048,
        maxTrianglesPerProp = 50_000,
        lodLevels = 3,
        shadowQuality = "High Soft Shadows",
        materialMode = "Full PBR + Emissive"
    )
}

enum class LicensePolicyDecision(val isPermitted: Boolean, val summary: String) {
    CC0_PUBLIC_DOMAIN(true, "CC0 — Commercial use permitted without attribution requirement"),
    CC_BY_ATTRIBUTION(true, "CC BY — Commercial use permitted with creator attribution"),
    CC_BY_SA_SHARE_ALIKE(true, "CC BY-SA — Commercial use permitted with attribution & ShareAlike"),
    REJECTED_NON_COMMERCIAL(false, "Rejected — CC BY-NC prohibits commercial game distribution"),
    REJECTED_NO_DERIVATIVES(false, "Rejected — CC BY-ND restricts modifications/retargeting"),
    REJECTED_UNCLEAR_LICENSE(false, "Rejected — License is missing or unclear")
}

data class ScoreBreakdown(
    val visualQuality: Int,      // max 20
    val mobileSuitability: Int,  // max 20
    val textureQuality: Int,     // max 15
    val polygonEfficiency: Int,  // max 15
    val licenseSuitability: Int, // max 15
    val gameRelevance: Int,      // max 10
    val animationSupport: Int    // max 5
) {
    val totalScore: Int
        get() = visualQuality + mobileSuitability + textureQuality +
            polygonEfficiency + licenseSuitability + gameRelevance + animationSupport
}

data class EvaluatedCandidate(
    val model: SketchfabModelDto,
    val category: SearchCategory,
    val searchTerm: String,
    val score: ScoreBreakdown,
    val licenseDecision: LicensePolicyDecision,
    val attributionRequirement: String,
    val canRedistributeOnGitHub: Boolean,
    val rejectionReason: String? = null
)

data class OptimizationMetrics(
    val profile: QualityProfile,
    val originalTextureBytes: Long,
    val optimizedTextureBytes: Long,
    val texturesProcessed: Int,
    val triangleCount: Int,
    val meetsPolygonBudget: Boolean,
    val recommendedLodDistances: String
)

data class ValidationCheckResult(
    val passed: Boolean,
    val hasGltfOrGlb: Boolean,
    val hasBinOrEmbeddedBuffer: Boolean,
    val texturesVerified: Boolean,
    val licenseCleared: Boolean,
    val withinMobileBudget: Boolean,
    val issues: List<String>
)

data class PrefabBindingSpec(
    val prefabName: String,
    val targetGameSlot: String, // e.g., "PF_MetroTrain", "PF_Character_Alex", "PF_RollingStone"
    val folderPath: String,
    val normalizedScale: Float,
    val colliderSize: Triple<Float, Float, Float>,
    val lodGroupCount: Int
)
