package com.example.pipeline

import android.content.Context
import java.io.File

class AssetDirectoryLayout(context: Context) {
    val rootAssetsDir = File(context.filesDir, "Assets")

    private val requiredDirs = listOf(
        "Assets/_Game/Art/Characters",
        "Assets/_Game/Art/Environment/MetroCity",
        "Assets/_Game/Art/Environment/AncientTemple",
        "Assets/_Game/Art/Environment/Jungle",
        "Assets/_Game/Art/Environment/CyberCity",
        "Assets/_Game/Art/Environment/SnowMountain",
        "Assets/_Game/Art/Vehicles/Trains",
        "Assets/_Game/Art/Obstacles",
        "Assets/_Game/Art/Collectibles",
        "Assets/_Game/Art/PowerUps",
        "Assets/_Game/Art/Props",
        "Assets/_Game/Art/Nature",
        "Assets/_Game/Art/Buildings",
        "Assets/_Game/Art/Materials",
        "Assets/_Game/Art/Textures",
        "Assets/_Game/Art/Animations",
        "Assets/_Game/Art/VFX",
        "Assets/_External/Sketchfab/OriginalDownloads",
        "Assets/_External/Sketchfab/Extracted",
        "Assets/_External/Sketchfab/Attribution",
        "Assets/_External/Sketchfab/LicenseRecords",
        "Assets/_External/Sketchfab/ImportReports"
    )

    val originalDownloadsDir = File(context.filesDir, "Assets/_External/Sketchfab/OriginalDownloads")
    val extractedDir = File(context.filesDir, "Assets/_External/Sketchfab/Extracted")
    val attributionDir = File(context.filesDir, "Assets/_External/Sketchfab/Attribution")
    val licenseRecordsDir = File(context.filesDir, "Assets/_External/Sketchfab/LicenseRecords")
    val importReportsDir = File(context.filesDir, "Assets/_External/Sketchfab/ImportReports")
    val gameTexturesDir = File(context.filesDir, "Assets/_Game/Art/Textures")

    fun ensureAllDirectoriesExist(): List<String> {
        return requiredDirs.map { rel ->
            val dir = File(rootAssetsDir.parentFile, rel)
            if (!dir.exists()) dir.mkdirs()
            rel
        }
    }

    fun folderForCategory(categoryId: String): File {
        val rel = when (categoryId) {
            "metro_city" -> "Assets/_Game/Art/Environment/MetroCity"
            "ancient_temple" -> "Assets/_Game/Art/Environment/AncientTemple"
            "jungle" -> "Assets/_Game/Art/Environment/Jungle"
            "futuristic_city" -> "Assets/_Game/Art/Environment/CyberCity"
            "player_characters" -> "Assets/_Game/Art/Characters"
            "obstacles" -> "Assets/_Game/Art/Obstacles"
            "collectibles" -> "Assets/_Game/Art/Collectibles"
            else -> "Assets/_Game/Art/Props"
        }
        return File(rootAssetsDir.parentFile, rel).apply { if (!exists()) mkdirs() }
    }

    fun treeSnapshot(): List<Pair<String, Int>> {
        ensureAllDirectoriesExist()
        return requiredDirs.map { rel ->
            val dir = File(rootAssetsDir.parentFile, rel)
            val fileCount = dir.listFiles()?.size ?: 0
            rel to fileCount
        }
    }
}
