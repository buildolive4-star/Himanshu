package com.example.pipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ExtractedAssetPackage(
    val extractedDir: File,
    val mainModelFile: File?,
    val binFiles: List<File>,
    val textureFiles: List<File>,
    val extraFiles: List<File>
)

class SketchfabAssetImporter(private val layout: AssetDirectoryLayout) {

    suspend fun extractAndInspect(uid: String, zipFile: File): Result<ExtractedAssetPackage> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!zipFile.exists()) error("Archive not found: ${zipFile.absolutePath}")
                val targetDir = File(layout.extractedDir, uid)
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                val canonicalDest = targetDir.canonicalPath
                ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)
                        val canonicalOut = outFile.canonicalPath
                        if (!canonicalOut.startsWith(canonicalDest + File.separator)) {
                            throw SecurityException("Blocked unsafe ZIP path: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val allFiles = targetDir.walkTopDown().filter { it.isFile }.toList()
                val mainModel = allFiles.firstOrNull { it.name.equals("scene.gltf", ignoreCase = true) }
                    ?: allFiles.firstOrNull { it.extension.equals("gltf", ignoreCase = true) }
                    ?: allFiles.firstOrNull { it.extension.equals("glb", ignoreCase = true) }

                val bins = allFiles.filter { it.extension.equals("bin", ignoreCase = true) }
                val textures = allFiles.filter {
                    it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp")
                }
                val extras = allFiles.filter { it != mainModel && it !in bins && it !in textures }

                ExtractedAssetPackage(
                    extractedDir = targetDir,
                    mainModelFile = mainModel,
                    binFiles = bins,
                    textureFiles = textures,
                    extraFiles = extras
                )
            }
        }

    suspend fun importToGameArt(
        uid: String,
        categoryId: String,
        extracted: ExtractedAssetPackage
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val destFolder = File(layout.folderForCategory(categoryId), uid)
            if (destFolder.exists()) destFolder.deleteRecursively()
            destFolder.mkdirs()
            extracted.extractedDir.copyRecursively(destFolder, overwrite = true)
            destFolder
        }
    }
}
