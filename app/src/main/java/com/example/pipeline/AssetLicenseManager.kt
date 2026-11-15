package com.example.pipeline

import com.example.data.sketchfab.SketchfabLicenseDto
import java.util.Locale

object AssetLicenseManager {

    data class LicenseEvaluation(
        val decision: LicensePolicyDecision,
        val normalizedLabel: String,
        val attributionText: String,
        val canRedistributeOnGitHub: Boolean,
        val scorePoints: Int // out of 15
    )

    fun evaluate(
        license: SketchfabLicenseDto?,
        modelName: String,
        creatorName: String,
        modelUrl: String?
    ): LicenseEvaluation {
        val raw = listOfNotNull(license?.slug, license?.label, license?.requirements)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        val label = license?.label?.takeIf { it.isNotBlank() } ?: license?.slug ?: "Unknown"

        val urlPart = modelUrl?.takeIf { it.isNotBlank() } ?: "https://sketchfab.com"

        return when {
            raw.contains("nc") || raw.contains("noncommercial") || raw.contains("non-commercial") -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.REJECTED_NON_COMMERCIAL,
                    normalizedLabel = label,
                    attributionText = "Restricted (Non-Commercial): $modelName by $creatorName",
                    canRedistributeOnGitHub = false,
                    scorePoints = 0
                )
            }
            raw.contains("nd") || raw.contains("noderivs") || raw.contains("no-derivatives") -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.REJECTED_NO_DERIVATIVES,
                    normalizedLabel = label,
                    attributionText = "Restricted (NoDerivs): $modelName by $creatorName",
                    canRedistributeOnGitHub = false,
                    scorePoints = 0
                )
            }
            raw.contains("cc0") || raw.contains("public domain") -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.CC0_PUBLIC_DOMAIN,
                    normalizedLabel = "CC0 1.0 Public Domain",
                    attributionText = "CC0 Public Domain — \"$modelName\" by $creatorName ($urlPart)",
                    canRedistributeOnGitHub = true,
                    scorePoints = 15
                )
            }
            raw.contains("by-sa") || raw.contains("sharealike") -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.CC_BY_SA_SHARE_ALIKE,
                    normalizedLabel = label,
                    attributionText = "\"$modelName\" by $creatorName ($urlPart) — Licensed under $label (ShareAlike required for derivative distributions)",
                    canRedistributeOnGitHub = true,
                    scorePoints = 11
                )
            }
            raw.contains("by") || raw.contains("attribution") -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.CC_BY_ATTRIBUTION,
                    normalizedLabel = label,
                    attributionText = "\"$modelName\" by $creatorName ($urlPart) — Licensed under $label",
                    canRedistributeOnGitHub = true,
                    scorePoints = 14
                )
            }
            else -> {
                LicenseEvaluation(
                    decision = LicensePolicyDecision.REJECTED_UNCLEAR_LICENSE,
                    normalizedLabel = label,
                    attributionText = "Unclear license ($label): excluded per Metro Legends policy",
                    canRedistributeOnGitHub = false,
                    scorePoints = 0
                )
            }
        }
    }
}
