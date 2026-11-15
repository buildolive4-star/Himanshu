package com.example.pipeline

import android.content.Context
import com.example.data.local.AssetDao
import com.example.data.local.AssetEntity
import com.example.data.sketchfab.SearchCategory
import com.example.data.sketchfab.SketchfabApiService
import com.example.data.sketchfab.SketchfabNetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PipelineActionResult {
    data class Success(val message: String, val asset: AssetEntity) : PipelineActionResult()
    data class AuthNeeded(val message: String) : PipelineActionResult()
    data class Failure(val message: String) : PipelineActionResult()
}

class AssetPipelineCoordinator(
    context: Context,
    private val dao: AssetDao,
    private val apiService: SketchfabApiService = SketchfabNetworkModule.apiService
) {
    val layout = AssetDirectoryLayout(context)
    private val downloadManager = AssetDownloadManager(apiService, layout)
    private val importer = SketchfabAssetImporter(layout)
    private val optimizer = AssetOptimizationManager(layout)
    private val prefabGenerator = PrefabGenerationManager(layout)
    private val validator = AssetValidationManager()
    private val reportGenerator = AssetReportGenerator(layout)

    suspend fun searchAndScore(
        category: SearchCategory,
        query: String
    ): Result<List<EvaluatedCandidate>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.searchModels(
                query = query,
                downloadable = true,
                sortBy = "-likeCount",
                count = 18
            )
            if (!response.isSuccessful) {
                error("Sketchfab search returned HTTP ${response.code()}: ${response.message()}")
            }
            val models = response.body()?.results.orEmpty()
            val evaluated = models.map { model ->
                ModelScoringEngine.evaluateCandidate(model, category, query)
            }.sortedByDescending { it.score.totalScore }

            // Record evaluated candidates in Room without overwriting already downloaded/imported statuses
            for (cand in evaluated) {
                val existing = dao.getAssetByUid(cand.model.uid)
                if (existing == null) {
                    val creator = cand.model.user?.displayName?.takeIf { it.isNotBlank() }
                        ?: cand.model.user?.username ?: "unknown"
                    val initialStatus = when {
                        cand.rejectionReason != null -> IntegrationStatus.FAILED
                        cand.licenseDecision.isPermitted -> IntegrationStatus.LICENSE_VERIFIED
                        else -> IntegrationStatus.DISCOVERED
                    }
                    dao.upsert(
                        AssetEntity(
                            uid = cand.model.uid,
                            name = cand.model.name,
                            creator = creator,
                            viewerUrl = cand.model.viewerUrl ?: "https://sketchfab.com/3d-models/${cand.model.uid}",
                            thumbnailUrl = cand.model.bestThumbnailUrl().orEmpty(),
                            categoryId = category.id,
                            searchTerm = query,
                            licenseLabel = cand.model.license?.label ?: cand.model.license?.slug ?: "Unknown",
                            licenseDecision = cand.licenseDecision.name,
                            attributionRequirement = cand.attributionRequirement,
                            canRedistributeOnGitHub = cand.canRedistributeOnGitHub,
                            faceCount = cand.model.faceCount,
                            vertexCount = cand.model.vertexCount,
                            animationCount = cand.model.animationCount,
                            archiveSizeBytes = cand.model.gltfArchiveSizeBytes(),
                            scoreTotal = cand.score.totalScore,
                            scoreVisual = cand.score.visualQuality,
                            scoreMobile = cand.score.mobileSuitability,
                            scoreTexture = cand.score.textureQuality,
                            scorePoly = cand.score.polygonEfficiency,
                            scoreLicense = cand.score.licenseSuitability,
                            scoreRelevance = cand.score.gameRelevance,
                            scoreAnimation = cand.score.animationSupport,
                            status = initialStatus.name,
                            failureReason = cand.rejectionReason
                        )
                    )
                }
            }
            evaluated
        }
    }

    suspend fun verifyAccountAuth(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!AssetPipelineConfig.isDownloadAuthorized) {
                error("No SKETCHFAB_API_TOKEN or SKETCHFAB_OAUTH_ACCESS_TOKEN configured in Secrets.")
            }
            val res = apiService.getCurrentUser()
            if (!res.isSuccessful) {
                error("Sketchfab auth check failed: HTTP ${res.code()}")
            }
            val me = res.body() ?: error("Empty profile response")
            "Authenticated as @${me.username} (${me.account ?: "standard"})"
        }
    }

    suspend fun runFullImportPipeline(
        candidate: EvaluatedCandidate,
        qualityProfile: QualityProfile
    ): PipelineActionResult = withContext(Dispatchers.IO) {
        val uid = candidate.model.uid
        if (!candidate.licenseDecision.isPermitted || candidate.rejectionReason != null) {
            val reason = candidate.rejectionReason ?: candidate.licenseDecision.summary
            val existing = dao.getAssetByUid(uid)
            if (existing != null) {
                dao.upsert(existing.copy(status = IntegrationStatus.FAILED.name, failureReason = reason))
            }
            return@withContext PipelineActionResult.Failure(reason)
        }

        val current = dao.getAssetByUid(uid) ?: return@withContext PipelineActionResult.Failure("Asset record missing")
        dao.upsert(current.copy(status = IntegrationStatus.DOWNLOAD_REQUESTED.name, failureReason = null))

        when (val dl = downloadManager.downloadModelArchive(uid)) {
            is DownloadOutcome.AuthRequired -> {
                dao.upsert(current.copy(status = IntegrationStatus.LICENSE_VERIFIED.name, failureReason = dl.reason))
                return@withContext PipelineActionResult.AuthNeeded(dl.reason)
            }
            is DownloadOutcome.Failed -> {
                dao.upsert(current.copy(status = IntegrationStatus.FAILED.name, failureReason = dl.reason))
                return@withContext PipelineActionResult.Failure(dl.reason)
            }
            is DownloadOutcome.Success -> {
                val dateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val downloadedEntity = current.copy(
                    status = IntegrationStatus.DOWNLOADED.name,
                    downloadDateIso = dateIso,
                    originalZipPath = dl.zipFile.absolutePath,
                    archiveSizeBytes = dl.bytesDownloaded,
                    failureReason = null
                )
                dao.upsert(downloadedEntity)

                val extracted = importer.extractAndInspect(uid, dl.zipFile).getOrElse { ex ->
                    val failed = downloadedEntity.copy(
                        status = IntegrationStatus.FAILED.name,
                        failureReason = "Extraction failed: ${ex.message}"
                    )
                    dao.upsert(failed)
                    return@withContext PipelineActionResult.Failure(failed.failureReason!!)
                }

                val extractedEntity = downloadedEntity.copy(
                    status = IntegrationStatus.EXTRACTED.name,
                    extractedDirPath = extracted.extractedDir.absolutePath,
                    mainModelPath = extracted.mainModelFile?.absolutePath
                )
                dao.upsert(extractedEntity)

                val parsedMesh = extracted.mainModelFile?.let {
                    GltfMeshParser.parseFromFile(it).getOrNull()
                }
                val triCount = parsedMesh?.triangleCount ?: extractedEntity.faceCount
                optimizer.optimizeAsset(uid, extracted.textureFiles, triCount, qualityProfile)

                val optimizedEntity = extractedEntity.copy(
                    status = IntegrationStatus.OPTIMIZED.name,
                    optimizedDirPath = File(layout.gameTexturesDir, "${uid}_${qualityProfile.name}").absolutePath,
                    qualityProfile = qualityProfile.name
                )
                dao.upsert(optimizedEntity)

                importer.importToGameArt(uid, optimizedEntity.categoryId, extracted).getOrElse { ex ->
                    val failed = optimizedEntity.copy(
                        status = IntegrationStatus.FAILED.name,
                        failureReason = "Game Art import failed: ${ex.message}"
                    )
                    dao.upsert(failed)
                    return@withContext PipelineActionResult.Failure(failed.failureReason!!)
                }

                val importedEntity = optimizedEntity.copy(status = IntegrationStatus.IMPORTED.name)
                dao.upsert(importedEntity)

                val slot = prefabGenerator.suggestedSlotForCategory(importedEntity.categoryId, importedEntity.name)
                val prefab = prefabGenerator.generatePrefabDescriptor(importedEntity, parsedMesh, slot, qualityProfile)
                val validation = validator.validate(importedEntity, parsedMesh, qualityProfile)

                val finalStatus = if (validation.passed) {
                    IntegrationStatus.PREFAB_CREATED
                } else {
                    IntegrationStatus.FAILED
                }

                val finalEntity = importedEntity.copy(
                    status = finalStatus.name,
                    prefabName = prefab.prefabName,
                    failureReason = validation.issues.takeIf { it.isNotEmpty() }?.joinToString("; ")
                )
                dao.upsert(finalEntity)
                reportGenerator.generateAndSaveAll(dao.getAllAssets())

                if (validation.passed) {
                    PipelineActionResult.Success(
                        "Imported & generated ${prefab.prefabName} (${parsedMesh?.triangleCount ?: triCount} tris)",
                        finalEntity
                    )
                } else {
                    PipelineActionResult.Failure("Validation warning: ${finalEntity.failureReason}")
                }
            }
        }
    }

    suspend fun bindAssetToGameSlot(uid: String, slot: String): Result<AssetEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.getAssetByUid(uid) ?: error("Asset not found")
            val modelFile = entity.mainModelPath?.let { File(it) }
            if (modelFile == null || !modelFile.exists()) {
                error("Cannot bind $slot: no extracted .gltf/.glb mesh found on disk")
            }
            val parsed = GltfMeshParser.parseFromFile(modelFile).getOrThrow()
            if (parsed.interleavedTriangles.isEmpty()) {
                error("Parsed mesh has 0 renderable triangles")
            }
            dao.clearBindingForSlot(slot)
            val updated = entity.copy(
                boundGameSlot = slot,
                prefabName = slot,
                status = IntegrationStatus.TESTED.name,
                updatedAtMillis = System.currentTimeMillis()
            )
            dao.upsert(updated)
            reportGenerator.generateAndSaveAll(dao.getAllAssets())
            updated
        }
    }

    suspend fun reoptimizeAsset(uid: String, profile: QualityProfile): Result<OptimizationMetrics> =
        withContext(Dispatchers.IO) {
            runCatching {
                val entity = dao.getAssetByUid(uid) ?: error("Asset not found")
                val dir = entity.extractedDirPath?.let { File(it) } ?: error("Asset not extracted yet")
                val textures = dir.walkTopDown().filter {
                    it.isFile && it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp")
                }.toList()
                val parsed = entity.mainModelPath?.let { GltfMeshParser.parseFromFile(File(it)).getOrNull() }
                val metrics = optimizer.optimizeAsset(
                    uid = uid,
                    textureFiles = textures,
                    triangleCount = parsed?.triangleCount ?: entity.faceCount,
                    profile = profile
                )
                dao.upsert(entity.copy(qualityProfile = profile.name))
                metrics
            }
        }

    suspend fun refreshReports(): GeneratedReportBundle = withContext(Dispatchers.IO) {
        reportGenerator.generateAndSaveAll(dao.getAllAssets())
    }

    suspend fun autoAcquireStarterPack(
        qualityProfile: QualityProfile,
        onProgress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!AssetPipelineConfig.isDownloadAuthorized) {
                error("Add SKETCHFAB_API_TOKEN in AI Studio Secrets and Apply changes before batch download.")
            }

            val targets = listOf(
                Triple(SearchCategory.METRO_CITY, "subway train 3D", "PF_MetroTrain"),
                Triple(SearchCategory.ANCIENT_TEMPLE, "ancient pillars", "PF_TemplePillar"),
                Triple(SearchCategory.JUNGLE, "palm trees", "PF_JungleTree"),
                Triple(SearchCategory.FUTURISTIC_CITY, "futuristic skyscraper", "PF_CyberBuilding"),
                Triple(SearchCategory.OBSTACLES, "rolling stone", "PF_RollingStone"),
                Triple(SearchCategory.COLLECTIBLES, "gold coin 3D", "PF_Collectible_Coin")
            )

            var importedCount = 0
            var boundCount = 0

            for ((index, spec) in targets.withIndex()) {
                val (category, term, slot) = spec
                onProgress("(${index + 1}/${targets.size}) Searching Sketchfab: \"$term\"...")
                val candidates = searchAndScore(category, term).getOrElse { emptyList() }
                val best = candidates.firstOrNull { cand ->
                    cand.licenseDecision.isPermitted &&
                        cand.rejectionReason == null &&
                        cand.model.faceCount in 50..85_000
                } ?: candidates.firstOrNull { it.licenseDecision.isPermitted && it.rejectionReason == null }

                if (best != null) {
                    onProgress("(${index + 1}/${targets.size}) Importing ${best.model.name} (${best.score.totalScore}/100)...")
                    when (val res = runFullImportPipeline(best, qualityProfile)) {
                        is PipelineActionResult.Success -> {
                            importedCount++
                            if (bindAssetToGameSlot(best.model.uid, slot).isSuccess) {
                                boundCount++
                            }
                        }
                        is PipelineActionResult.AuthNeeded -> error(res.message)
                        is PipelineActionResult.Failure -> { /* continue to next category */ }
                    }
                }
            }
            refreshReports()
            "Starter pack complete: $importedCount models imported, $boundCount bound to 3D Runner."
        }
    }

    fun availableGameSlots(): List<String> = prefabGenerator.availableGameSlots()
}
