package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.sketchfab.SearchCategory
import com.example.pipeline.AssetPipelineConfig
import com.example.pipeline.EvaluatedCandidate
import com.example.pipeline.SketchfabAuthMode
import com.example.ui.DiscoveryUiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onSelectCategory: (SearchCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onRunSearch: (String?) -> Unit,
    onVerifyAccount: () -> Unit,
    onImportCandidate: (EvaluatedCandidate) -> Unit,
    onAutoAcquireStarterPack: () -> Unit,
    onOpenOEmbed: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AuthStatusBanner(
                accountStatusText = state.accountStatusText,
                batchProgressText = state.batchProgressText,
                onVerifyAccount = onVerifyAccount,
                onAutoAcquireStarterPack = onAutoAcquireStarterPack,
                onOpenOEmbedSample = {
                    onOpenOEmbed(
                        "https://sketchfab.com/models/dGUrytaktlDeNudCEGKk31oTJY",
                        "Sketchfab Live oEmbed 3D Demo"
                    )
                }
            )
        }

        // 7 Categories A–G
        item {
            Text(
                text = "SKETCHFAB SEARCH CATEGORIES (DATA API v3)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchCategory.entries) { cat ->
                    FilterChip(
                        selected = cat == state.selectedCategory,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat.displayName) },
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }
        }

        // Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.queryText,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    label = { Text("Search downloadable glTF models") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sketchfab_search_input")
                )
                Button(
                    onClick = { onRunSearch(null) },
                    enabled = !state.isSearching,
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("sketchfab_search_button")
                ) {
                    if (state.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianBg
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search Sketchfab")
                    }
                }
            }
        }

        // Part 3 Keyword Presets
        item {
            Text(
                text = "CURATED KEYWORDS — ${state.selectedCategory.targetFolder}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.selectedCategory.searchTerms) { term ->
                    AssistChip(
                        onClick = { onRunSearch(term) },
                        label = { Text(term, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.testTag("preset_term_$term")
                    )
                }
            }
        }

        if (state.errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, CrimsonAlert)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = CrimsonAlert,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (state.candidates.isEmpty() && !state.isSearching && state.errorMessage == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Tap a keyword chip or Search to evaluate live Sketchfab models",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Each model is scored out of 100 across Visual, Mobile, Texture, Polygon, License, Relevance, and Animation criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        items(state.candidates, key = { it.model.uid }) { candidate ->
            CandidateScoreCard(
                candidate = candidate,
                isImporting = state.importingUid == candidate.model.uid,
                onImport = { onImportCandidate(candidate) },
                onOpenOEmbed = {
                    onOpenOEmbed(
                        candidate.model.viewerUrl ?: candidate.model.uid,
                        candidate.model.name
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthStatusBanner(
    accountStatusText: String?,
    batchProgressText: String?,
    onVerifyAccount: () -> Unit,
    onAutoAcquireStarterPack: () -> Unit,
    onOpenOEmbedSample: () -> Unit
) {
    val mode = AssetPipelineConfig.authMode
    val authorized = mode != SketchfabAuthMode.UNCONFIGURED
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (authorized) CarbonSurfaceElevated else CarbonSurface
        ),
        border = BorderStroke(1.dp, if (authorized) EmeraldVerified else ElectricCyan)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (authorized) Icons.Default.VerifiedUser else Icons.Default.Public,
                        contentDescription = "Sketchfab Auth Status",
                        tint = if (authorized) EmeraldVerified else ElectricCyan
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (authorized) {
                                "Sketchfab API Token Active (No OAuth Needed)"
                            } else {
                                "Search & oEmbed Ready (No OAuth Needed)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (authorized) EmeraldVerified else ElectricCyan
                        )
                        Text(
                            text = accountStatusText ?: if (authorized) {
                                "Your SKETCHFAB_API_TOKEN is active. You can search, preview via oEmbed, and download glTF archives."
                            } else {
                                "Search & oEmbed 3D Viewer work with zero tokens. Add SKETCHFAB_API_TOKEN in AI Studio Secrets for raw glTF ZIP downloads."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                if (authorized) {
                    OutlinedButton(
                        onClick = onVerifyAccount,
                        modifier = Modifier.testTag("verify_sketchfab_auth_button")
                    ) {
                        Text("Verify")
                    }
                }
            }

            if (batchProgressText != null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = batchProgressText,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenOEmbedSample,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_oembed_sample_button")
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("oEmbed 3D")
                    }
                    Button(
                        onClick = onAutoAcquireStarterPack,
                        enabled = authorized,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("auto_acquire_starter_pack_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Auto-Acquire Pack")
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateScoreCard(
    candidate: EvaluatedCandidate,
    isImporting: Boolean,
    onImport: () -> Unit,
    onOpenOEmbed: () -> Unit
) {
    val m = candidate.model
    val permitted = candidate.licenseDecision.isPermitted && candidate.rejectionReason == null
    val scoreColor = when {
        !permitted -> CrimsonAlert
        candidate.score.totalScore >= 80 -> EmeraldVerified
        candidate.score.totalScore >= 65 -> ElectricCyan
        else -> AmberGold
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, if (permitted) SlateBorder else CrimsonAlert.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = m.bestThumbnailUrl(),
                    contentDescription = m.name,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonSurfaceElevated),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = m.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = scoreColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, scoreColor)
                        ) {
                            Text(
                                text = "${candidate.score.totalScore}/100",
                                style = MaterialTheme.typography.labelLarge,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "by ${m.user?.displayName ?: m.user?.username ?: "unknown"} • UID ${m.uid.take(10)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${m.faceCount} faces • ${m.vertexCount} verts • ${m.animationCount} anims",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (permitted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "License status",
                            tint = if (permitted) EmeraldVerified else CrimsonAlert,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = m.license?.label ?: m.license?.slug ?: "Unknown License",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (permitted) EmeraldVerified else CrimsonAlert,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 7-Dimension Scoring Rubric Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { (candidate.score.totalScore / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = scoreColor,
                    trackColor = CarbonSurfaceElevated
                )
                Text(
                    text = "Vis ${candidate.score.visualQuality}/20 • Mob ${candidate.score.mobileSuitability}/20 • Tex ${candidate.score.textureQuality}/15 • Poly ${candidate.score.polygonEfficiency}/15 • Lic ${candidate.score.licenseSuitability}/15 • Rel ${candidate.score.gameRelevance}/10 • Anim ${candidate.score.animationSupport}/5",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (candidate.rejectionReason != null) {
                Text(
                    text = candidate.rejectionReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonAlert
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenOEmbed,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("oembed_view_${m.uid}")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("3D Preview")
                }
                Button(
                    onClick = onImport,
                    enabled = permitted && !isImporting,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("download_import_${m.uid}")
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianBg
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Importing...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (AssetPipelineConfig.isDownloadAuthorized) "Download glTF" else "Needs Token")
                    }
                }
            }
        }
    }
}
