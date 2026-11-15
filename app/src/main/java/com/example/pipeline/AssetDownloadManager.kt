package com.example.pipeline

import com.example.data.sketchfab.SketchfabApiService
import com.example.data.sketchfab.SketchfabNetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

sealed class DownloadOutcome {
    data class Success(val zipFile: File, val bytesDownloaded: Long) : DownloadOutcome()
    data class AuthRequired(val reason: String) : DownloadOutcome()
    data class Failed(val reason: String) : DownloadOutcome()
}

class AssetDownloadManager(
    private val apiService: SketchfabApiService,
    private val layout: AssetDirectoryLayout
) {
    suspend fun downloadModelArchive(uid: String): DownloadOutcome = withContext(Dispatchers.IO) {
        if (!AssetPipelineConfig.isDownloadAuthorized) {
            return@withContext DownloadOutcome.AuthRequired(
                "Missing Sketchfab credentials. Add SKETCHFAB_API_TOKEN or SKETCHFAB_OAUTH_ACCESS_TOKEN in the AI Studio Secrets panel."
            )
        }

        layout.ensureAllDirectoriesExist()

        // Request a fresh temporary download URL per Sketchfab Download API rules
        val response = runCatching { apiService.getModelDownloadUrls(uid) }.getOrElse { ex ->
            return@withContext DownloadOutcome.Failed("Network error requesting download URL: ${ex.message}")
        }

        if (response.code() == 401 || response.code() == 403) {
            return@withContext DownloadOutcome.AuthRequired(
                "Sketchfab authorization rejected (HTTP ${response.code()}). Verify your SKETCHFAB_API_TOKEN / OAuth scope."
            )
        }
        if (response.code() == 429) {
            // Respect API rate limits with a polite backoff once
            delay(2_000)
            return@withContext DownloadOutcome.Failed("Sketchfab API rate limit hit (HTTP 429). Try again shortly.")
        }
        if (!response.isSuccessful) {
            return@withContext DownloadOutcome.Failed("Download endpoint failed with HTTP ${response.code()}")
        }

        val body = response.body()
        val entry = body?.gltf ?: body?.glb
        if (entry == null || entry.url.isBlank()) {
            return@withContext DownloadOutcome.Failed("No glTF/GLB archive available for model $uid")
        }

        val targetZip = File(layout.originalDownloadsDir, "$uid.zip")
        val tempZip = File(layout.originalDownloadsDir, "$uid.zip.part")

        val request = Request.Builder().url(entry.url).get().build()
        val downloadResult = runCatching {
            SketchfabNetworkModule.okHttpClient.newCall(request).execute().use { res ->
                if (!res.isSuccessful) {
                    error("Temporary archive download failed with HTTP ${res.code}")
                }
                val stream = res.body?.byteStream() ?: error("Empty archive stream")
                FileOutputStream(tempZip).use { out ->
                    stream.copyTo(out, bufferSize = 16 * 1024)
                }
            }
            // Validate ZIP integrity before promoting
            ZipFile(tempZip).use { zip ->
                val entries = zip.entries()
                if (!entries.hasMoreElements()) error("Downloaded ZIP archive is empty")
            }
            if (targetZip.exists()) targetZip.delete()
            if (!tempZip.renameTo(targetZip)) error("Failed to move validated ZIP to OriginalDownloads")
            targetZip.length()
        }

        downloadResult.fold(
            onSuccess = { size -> DownloadOutcome.Success(targetZip, size) },
            onFailure = { ex ->
                tempZip.delete()
                DownloadOutcome.Failed(ex.message ?: "ZIP integrity check failed")
            }
        )
    }
}
