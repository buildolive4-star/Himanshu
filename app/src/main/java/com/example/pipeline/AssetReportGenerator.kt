package com.example.pipeline

import com.example.data.local.AssetEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedReportBundle(
    val creditsMd: String,
    val manifestJson: String,
    val importReportMd: String,
    val statusJson: String,
    val downloadInstructionsMd: String
)

class AssetReportGenerator(private val layout: AssetDirectoryLayout) {

    fun generateAndSaveAll(assets: List<AssetEntity>): GeneratedReportBundle {
        layout.ensureAllDirectoriesExist()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())

        val creditsMd = buildCreditsMd(assets, now)
        val manifestJson = buildManifestJson(assets, now)
        val importReportMd = buildImportReportMd(assets, now)
        val statusJson = buildStatusJson(assets, now)
        val instructionsMd = buildDownloadInstructionsMd(assets, now)

        File(layout.attributionDir, "ASSET_CREDITS.md").writeText(creditsMd)
        File(layout.attributionDir, "ASSET_MANIFEST.json").writeText(manifestJson)
        File(layout.importReportsDir, "ASSET_IMPORT_REPORT.md").writeText(importReportMd)
        File(layout.importReportsDir, "ASSET_INTEGRATION_STATUS.json").writeText(statusJson)
        File(layout.licenseRecordsDir, "ASSET_DOWNLOAD_INSTRUCTIONS.md").writeText(instructionsMd)

        return GeneratedReportBundle(
            creditsMd = creditsMd,
            manifestJson = manifestJson,
            importReportMd = importReportMd,
            statusJson = statusJson,
            downloadInstructionsMd = instructionsMd
        )
    }

    private fun buildCreditsMd(assets: List<AssetEntity>, timestamp: String): String {
        val permitted = assets.filter { !it.licenseDecision.startsWith("REJECTED") }
        return buildString {
            appendLine("# METRO LEGENDS — SKETCHFAB ASSET CREDITS")
            appendLine("Generated: $timestamp")
            appendLine("Engine: Android Native (Kotlin + Jetpack Compose + OpenGL ES 2.0)")
            appendLine()
            if (permitted.isEmpty()) {
                appendLine("_No Sketchfab assets have been shortlisted or imported yet. Built-in procedural 3D meshes are active._")
            } else {
                permitted.forEachIndexed { idx, a ->
                    appendLine("## ${idx + 1}. ${a.name}")
                    appendLine("- Model name: ${a.name}")
                    appendLine("- Creator: ${a.creator}")
                    appendLine("- Sketchfab model URL: ${a.viewerUrl}")
                    appendLine("- Model UID: ${a.uid}")
                    appendLine("- License: ${a.licenseLabel} (${a.licenseDecision})")
                    appendLine("- Download date: ${a.downloadDateIso ?: "Not downloaded yet"}")
                    appendLine("- Original filename: ${a.originalZipPath?.let { File(it).name } ?: "Pending"}")
                    appendLine("- Game usage: ${a.boundGameSlot ?: a.categoryId}")
                    appendLine("- Modifications: Mobile texture optimization (${a.qualityProfile}), pivot normalization, box collider generation")
                    appendLine("- Attribution requirements: ${a.attributionRequirement}")
                    appendLine()
                }
            }
        }
    }

    private fun buildManifestJson(assets: List<AssetEntity>, timestamp: String): String {
        val arr = JSONArray()
        assets.forEach { a ->
            arr.put(JSONObject().apply {
                put("uid", a.uid)
                put("name", a.name)
                put("creator", a.creator)
                put("url", a.viewerUrl)
                put("category", a.categoryId)
                put("license", a.licenseLabel)
                put("licenseDecision", a.licenseDecision)
                put("canRedistributeOnGitHub", a.canRedistributeOnGitHub)
                put("faceCount", a.faceCount)
                put("vertexCount", a.vertexCount)
                put("animationCount", a.animationCount)
                put("scoreTotal", a.scoreTotal)
                put("status", a.status)
                put("prefabName", a.prefabName ?: JSONObject.NULL)
                put("boundGameSlot", a.boundGameSlot ?: JSONObject.NULL)
            })
        }
        return JSONObject().apply {
            put("project", "METRO LEGENDS")
            put("engine", "Android Kotlin / Jetpack Compose / OpenGL ES 2.0")
            put("generatedAt", timestamp)
            put("assetCount", assets.size)
            put("assets", arr)
        }.toString(2)
    }

    private fun buildImportReportMd(assets: List<AssetEntity>, timestamp: String): String {
        val total = assets.size
        val shortlisted = assets.count { it.scoreTotal >= 65 && !it.licenseDecision.startsWith("REJECTED") }
        val downloaded = assets.count { it.originalZipPath != null }
        val imported = assets.count {
            it.status in setOf(
                IntegrationStatus.IMPORTED.name,
                IntegrationStatus.PREFAB_CREATED.name,
                IntegrationStatus.GAME_INTEGRATED.name,
                IntegrationStatus.TESTED.name
            )
        }
        val rejected = assets.filter { it.status == IntegrationStatus.FAILED.name || it.licenseDecision.startsWith("REJECTED") }

        return buildString {
            appendLine("# METRO LEGENDS — AUTOMATED ASSET IMPORT REPORT")
            appendLine("Generated: $timestamp")
            appendLine("Auth Status: ${AssetPipelineConfig.authMode.name}")
            appendLine()
            appendLine("## Summary Metrics")
            appendLine("- Total models evaluated: $total")
            appendLine("- Total models shortlisted (>=65/100 & commercial license): $shortlisted")
            appendLine("- Total models downloaded: $downloaded")
            appendLine("- Total models successfully imported: $imported")
            appendLine("- Total models rejected/failed: ${rejected.size}")
            appendLine()
            appendLine("## License Verification Results")
            assets.forEach { a ->
                appendLine("- [${a.uid}] ${a.name}: ${a.licenseLabel} -> ${a.licenseDecision}")
            }
            appendLine()
            if (rejected.isNotEmpty()) {
                appendLine("## Rejection & Failure Reasons")
                rejected.forEach { r ->
                    appendLine("- ${r.name} (${r.uid}): ${r.failureReason ?: r.licenseDecision}")
                }
            }
            appendLine()
            appendLine("## Integrated Prefab Paths")
            assets.filter { it.prefabName != null }.forEach { a ->
                appendLine("- ${a.prefabName}: ${a.mainModelPath ?: "Procedural fallback"}")
            }
        }
    }

    private fun buildStatusJson(assets: List<AssetEntity>, timestamp: String): String {
        val arr = JSONArray()
        assets.forEach { a ->
            arr.put(JSONObject().apply {
                put("uid", a.uid)
                put("name", a.name)
                put("status", a.status)
                put("score", a.scoreTotal)
                put("failureReason", a.failureReason ?: JSONObject.NULL)
            })
        }
        return JSONObject().apply {
            put("generatedAt", timestamp)
            put("statuses", arr)
        }.toString(2)
    }

    private fun buildDownloadInstructionsMd(assets: List<AssetEntity>, timestamp: String): String {
        val nonRedistributable = assets.filter { !it.canRedistributeOnGitHub || it.licenseDecision.contains("SHARE_ALIKE") }
        return buildString {
            appendLine("# ASSET DOWNLOAD & REDISTRIBUTION INSTRUCTIONS")
            appendLine("Generated: $timestamp")
            appendLine()
            appendLine("1. Configure `SKETCHFAB_API_TOKEN` or `SKETCHFAB_OAUTH_ACCESS_TOKEN` in the AI Studio Secrets panel (mapped via `.env` / `BuildConfig`).")
            appendLine("2. Original Sketchfab ZIP archives in `Assets/_External/Sketchfab/OriginalDownloads/` are excluded via `.gitignore`.")
            appendLine("3. Run the in-app Discovery -> Download -> Optimize -> Bind workflow to fetch fresh temporary glTF URLs on demand.")
            appendLine()
            if (nonRedistributable.isNotEmpty()) {
                appendLine("## Assets Requiring Separate Download or Special License Terms")
                nonRedistributable.forEach { a ->
                    appendLine("- ${a.name} (`${a.uid}`): ${a.viewerUrl} — ${a.licenseDecision}")
                }
            }
        }
    }
}
