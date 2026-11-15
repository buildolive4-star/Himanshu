package com.example.pipeline

import com.example.data.sketchfab.SearchCategory
import com.example.data.sketchfab.SketchfabModelDto
import java.util.Locale
import kotlin.math.min

object ModelScoringEngine {

    fun evaluateCandidate(
        model: SketchfabModelDto,
        category: SearchCategory,
        searchTerm: String
    ): EvaluatedCandidate {
        val creator = model.user?.displayName?.takeIf { it.isNotBlank() }
            ?: model.user?.username ?: "unknown"
        val licenseEval = AssetLicenseManager.evaluate(
            license = model.license,
            modelName = model.name,
            creatorName = creator,
            modelUrl = model.viewerUrl
        )

        // 1. Visual quality (20 pts)
        var visual = 6
        if (!model.bestThumbnailUrl().isNullOrBlank()) visual += 4
        if (!model.staffpickedAt.isNullOrBlank()) visual += 4
        visual += when {
            model.likeCount >= 200 -> 6
            model.likeCount >= 50 -> 4
            model.likeCount >= 10 -> 2
            else -> 1
        }
        visual = min(20, visual)

        // 2. Mobile suitability (20 pts)
        val sizeMb = model.gltfArchiveSizeBytes() / (1024.0 * 1024.0)
        val faces = model.faceCount
        val mobile = when {
            faces == 0 -> 8
            faces <= 15_000 && (sizeMb == 0.0 || sizeMb <= 12.0) -> 20
            faces <= 35_000 && (sizeMb == 0.0 || sizeMb <= 25.0) -> 16
            faces <= 75_000 && (sizeMb == 0.0 || sizeMb <= 45.0) -> 11
            faces <= 150_000 -> 6
            else -> 2
        }

        // 3. Texture quality (15 pts)
        val texCount = model.gltfTextureCount()
        val maxRes = model.gltfTextureMaxRes()
        val texture = when {
            texCount >= 2 && maxRes in 512..2048 -> 15
            texCount >= 1 && maxRes in 512..4096 -> 12
            texCount >= 1 -> 10
            model.archives?.gltf != null -> 8
            else -> 6
        }

        // 4. Polygon efficiency (15 pts)
        val poly = when {
            faces in 200..12_000 -> 15
            faces in 12_001..30_000 -> 13
            faces in 30_001..65_000 -> 9
            faces in 65_001..150_000 -> 5
            faces > 150_000 -> 1
            else -> 7
        }

        // 5. License suitability (15 pts)
        val licenseScore = licenseEval.scorePoints

        // 6. Game relevance (10 pts)
        val haystack = buildString {
            append(model.name)
            append(' ')
            append(model.description.orEmpty())
            append(' ')
            append(model.tags.joinToString(" ") { it.name })
        }.lowercase(Locale.ROOT)
        val queryTokens = searchTerm.lowercase(Locale.ROOT).split(" ").filter { it.length >= 3 }
        val matches = queryTokens.count { haystack.contains(it) }
        val relevance = when {
            queryTokens.isEmpty() -> 6
            matches >= 2 -> 10
            matches == 1 -> 7
            else -> 4
        }

        // 7. Animation support (5 pts)
        val animation = when {
            category == SearchCategory.PLAYER_CHARACTERS && model.animationCount > 0 -> 5
            category == SearchCategory.PLAYER_CHARACTERS && model.animationCount == 0 -> 1
            model.animationCount > 0 -> 5
            else -> 3
        }

        val breakdown = ScoreBreakdown(
            visualQuality = visual,
            mobileSuitability = mobile,
            textureQuality = texture,
            polygonEfficiency = poly,
            licenseSuitability = licenseScore,
            gameRelevance = relevance,
            animationSupport = animation
        )

        val rejection = when {
            !model.isDownloadable -> "Model is not marked downloadable by creator"
            !licenseEval.decision.isPermitted -> licenseEval.decision.summary
            faces > 250_000 -> "Exceeds mobile triangle threshold (${faces} faces) without decimation"
            else -> null
        }

        return EvaluatedCandidate(
            model = model,
            category = category,
            searchTerm = searchTerm,
            score = breakdown,
            licenseDecision = licenseEval.decision,
            attributionRequirement = licenseEval.attributionText,
            canRedistributeOnGitHub = licenseEval.canRedistributeOnGitHub,
            rejectionReason = rejection
        )
    }
}
