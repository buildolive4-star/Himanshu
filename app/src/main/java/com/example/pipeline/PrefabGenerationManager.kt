package com.example.pipeline

import com.example.data.local.AssetEntity
import org.json.JSONObject
import java.io.File

class PrefabGenerationManager(private val layout: AssetDirectoryLayout) {

    fun availableGameSlots(): List<String> = listOf(
        "PF_MetroTrain",
        "PF_RailwaySignal",
        "PF_CityBuilding",
        "PF_TemplePillar",
        "PF_RollingStone",
        "PF_JungleTree",
        "PF_CyberBuilding",
        "PF_Character_Alex",
        "PF_Character_Mia",
        "PF_Character_Ryan",
        "PF_Character_Zara",
        "PF_Character_Shadow",
        "PF_Character_Explorer",
        "PF_Collectible_Coin",
        "PF_PowerUp_Hoverboard"
    )

    fun suggestedSlotForCategory(categoryId: String, modelName: String): String {
        val lower = modelName.lowercase()
        return when (categoryId) {
            "metro_city" -> when {
                lower.contains("train") || lower.contains("subway") -> "PF_MetroTrain"
                lower.contains("signal") -> "PF_RailwaySignal"
                else -> "PF_CityBuilding"
            }
            "ancient_temple" -> when {
                lower.contains("boulder") || lower.contains("stone") -> "PF_RollingStone"
                else -> "PF_TemplePillar"
            }
            "jungle" -> "PF_JungleTree"
            "futuristic_city" -> "PF_CyberBuilding"
            "player_characters" -> "PF_Character_Alex"
            "obstacles" -> "PF_RollingStone"
            "collectibles" -> if (lower.contains("board")) "PF_PowerUp_Hoverboard" else "PF_Collectible_Coin"
            else -> "PF_MetroTrain"
        }
    }

    fun generatePrefabDescriptor(
        asset: AssetEntity,
        parsedMesh: ParsedGltfMesh?,
        targetSlot: String,
        profile: QualityProfile
    ): PrefabBindingSpec {
        val destFolder = layout.folderForCategory(asset.categoryId)
        val scale = parsedMesh?.normalizedScale ?: 1.0f
        val dx = parsedMesh?.let { (it.boundsMax[0] - it.boundsMin[0]) * scale } ?: 1.2f
        val dy = parsedMesh?.let { (it.boundsMax[1] - it.boundsMin[1]) * scale } ?: 1.8f
        val dz = parsedMesh?.let { (it.boundsMax[2] - it.boundsMin[2]) * scale } ?: 1.2f

        val prefabJson = JSONObject().apply {
            put("prefabName", targetSlot)
            put("sourceUid", asset.uid)
            put("sourceName", asset.name)
            put("engine", "Android OpenGL ES 2.0 / Compose Runner")
            put("mainModelPath", asset.mainModelPath ?: "")
            put("normalizedScale", scale.toDouble())
            put("pivotMode", "BottomCenter")
            put("collider", JSONObject().apply {
                put("type", "BoxCollider")
                put("sizeX", dx.toDouble())
                put("sizeY", dy.toDouble())
                put("sizeZ", dz.toDouble())
            })
            put("qualityProfile", profile.name)
            put("lodLevels", profile.lodLevels)
            put("gpuInstancing", true)
        }

        val outFile = File(destFolder, "$targetSlot.prefab.json")
        outFile.writeText(prefabJson.toString(2))

        return PrefabBindingSpec(
            prefabName = targetSlot,
            targetGameSlot = targetSlot,
            folderPath = outFile.absolutePath,
            normalizedScale = scale,
            colliderSize = Triple(dx, dy, dz),
            lodGroupCount = profile.lodLevels
        )
    }
}

class AssetValidationManager {

    fun validate(
        asset: AssetEntity,
        parsedMesh: ParsedGltfMesh?,
        profile: QualityProfile
    ): ValidationCheckResult {
        val issues = mutableListOf<String>()

        val mainFile = asset.mainModelPath?.let { File(it) }
        val hasModel = mainFile != null && mainFile.exists()
        if (!hasModel) issues.add("Missing main .gltf or .glb file on disk")

        val extractedDir = asset.extractedDirPath?.let { File(it) }
        val hasBinOrGlb = if (mainFile != null && mainFile.extension.equals("glb", ignoreCase = true)) {
            true
        } else {
            extractedDir?.walkTopDown()?.any { it.extension.equals("bin", ignoreCase = true) } == true ||
                (mainFile?.exists() == true && mainFile.readText().contains("data:application/octet-stream"))
        }
        if (!hasBinOrGlb) issues.add("Missing binary .bin buffer or embedded base64 buffer")

        val texturesVerified = extractedDir?.walkTopDown()?.any {
            it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp")
        } == true || parsedMesh != null
        if (!texturesVerified) issues.add("No texture maps or valid material color channels found")

        val licenseCleared = !asset.licenseDecision.startsWith("REJECTED")
        if (!licenseCleared) issues.add("License does not permit commercial game distribution")

        val triCount = parsedMesh?.triangleCount ?: asset.faceCount
        val withinBudget = triCount <= profile.maxTrianglesPerProp * 2
        if (!withinBudget) {
            issues.add("Triangle count ($triCount) exceeds mobile ceiling for ${profile.displayName}")
        }

        return ValidationCheckResult(
            passed = issues.isEmpty(),
            hasGltfOrGlb = hasModel,
            hasBinOrEmbeddedBuffer = hasBinOrGlb,
            texturesVerified = texturesVerified,
            licenseCleared = licenseCleared,
            withinMobileBudget = withinBudget,
            issues = issues
        )
    }
}
