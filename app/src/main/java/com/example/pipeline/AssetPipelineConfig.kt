package com.example.pipeline

import com.example.BuildConfig

enum class SketchfabAuthMode {
    API_TOKEN,
    UNCONFIGURED
}

object AssetPipelineConfig {
    private fun cleanSecret(raw: String, placeholder: String): String {
        val trimmed = raw.trim()
        return if (trimmed.isEmpty() || trimmed == placeholder) "" else trimmed
    }

    val sketchfabApiToken: String
        get() = cleanSecret(BuildConfig.SKETCHFAB_API_TOKEN, "MY_SKETCHFAB_API_TOKEN")

    val authMode: SketchfabAuthMode
        get() = if (sketchfabApiToken.isNotEmpty()) SketchfabAuthMode.API_TOKEN else SketchfabAuthMode.UNCONFIGURED

    val isDownloadAuthorized: Boolean
        get() = authMode == SketchfabAuthMode.API_TOKEN

    fun authorizationHeader(): String? = when (authMode) {
        SketchfabAuthMode.API_TOKEN -> "Token $sketchfabApiToken"
        SketchfabAuthMode.UNCONFIGURED -> null
    }
}
