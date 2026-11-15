package com.example.pipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

class AssetOptimizationManager(private val layout: AssetDirectoryLayout) {

    suspend fun optimizeAsset(
        uid: String,
        textureFiles: List<File>,
        triangleCount: Int,
        profile: QualityProfile
    ): OptimizationMetrics = withContext(Dispatchers.IO) {
        val outDir = File(layout.gameTexturesDir, "${uid}_${profile.name}")
        if (!outDir.exists()) outDir.mkdirs()

        var originalBytes = 0L
        var optimizedBytes = 0L
        var processed = 0

        for (tex in textureFiles) {
            if (!tex.exists()) continue
            originalBytes += tex.length()
            val outFile = File(outDir, tex.nameWithoutExtension + ".jpg")

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tex.absolutePath, opts)
            val maxDim = max(opts.outWidth, opts.outHeight)
            if (maxDim <= 0) {
                optimizedBytes += tex.length()
                continue
            }

            var sampleSize = 1
            while (maxDim / sampleSize > profile.maxTexturePx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(tex.absolutePath, decodeOpts)
            if (bitmap != null) {
                val curMax = max(bitmap.width, bitmap.height)
                val finalBmp = if (curMax > profile.maxTexturePx) {
                    val scale = profile.maxTexturePx.toFloat() / curMax.toFloat()
                    val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                    val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(bitmap, w, h, true).also {
                        if (it !== bitmap) bitmap.recycle()
                    }
                } else {
                    bitmap
                }

                val quality = when (profile) {
                    QualityProfile.LOW -> 75
                    QualityProfile.MEDIUM -> 85
                    QualityProfile.HIGH -> 92
                }
                FileOutputStream(outFile).use { fos ->
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
                finalBmp.recycle()
                optimizedBytes += outFile.length()
                processed++
            }
        }

        val lodDistances = when (profile) {
            QualityProfile.LOW -> "LOD0: 0–18m | Culled: >28m"
            QualityProfile.MEDIUM -> "LOD0: 0–22m | LOD1: 22–48m | Culled: >65m"
            QualityProfile.HIGH -> "LOD0: 0–30m | LOD1: 30–65m | LOD2: 65–110m"
        }

        OptimizationMetrics(
            profile = profile,
            originalTextureBytes = originalBytes,
            optimizedTextureBytes = optimizedBytes,
            texturesProcessed = processed,
            triangleCount = triangleCount,
            meetsPolygonBudget = triangleCount <= profile.maxTrianglesPerProp,
            recommendedLodDistances = lodDistances
        )
    }
}
