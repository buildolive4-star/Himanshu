package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AssetEntity
import com.example.game.ModelInspectorGLView
import com.example.pipeline.IntegrationStatus
import com.example.pipeline.QualityProfile
import com.example.ui.VaultUiState
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetVaultScreen(
    assets: List<AssetEntity>,
    vaultState: VaultUiState,
    qualityProfile: QualityProfile,
    availableSlots: List<String>,
    onToggleFolderTree: (Boolean) -> Unit,
    onOpenInspector: (AssetEntity) -> Unit,
    onCloseInspector: () -> Unit,
    onReoptimize: (String, QualityProfile) -> Unit,
    onBindSlot: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val importedCount = assets.count {
        it.status in setOf(
            IntegrationStatus.IMPORTED.name,
            IntegrationStatus.PREFAB_CREATED.name,
            IntegrationStatus.GAME_INTEGRATED.name,
            IntegrationStatus.TESTED.name
        )
    }
    val boundCount = assets.count { it.boundGameSlot != null }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ASSET PIPELINE VAULT", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${assets.size} evaluated • $importedCount imported • $boundCount bound",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !vaultState.showFolderTree,
                    onClick = { onToggleFolderTree(false) },
                    label = { Text("Assets") },
                    leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("vault_tab_assets")
                )
                FilterChip(
                    selected = vaultState.showFolderTree,
                    onClick = { onToggleFolderTree(true) },
                    label = { Text("Folders") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("vault_tab_folders")
                )
            }
        }

        if (vaultState.showFolderTree) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultState.folderSnapshot) { (path, fileCount) ->
                    Surface(
                        color = CarbonSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (path.contains("_External")) AmberGold else ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "$fileCount files",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fileCount > 0) EmeraldVerified else TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            if (assets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No Evaluated Assets Yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Use the Sketchfab Discovery tab to search, score, download, and optimize 3D glTF assets into Metro Legends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(assets, key = { it.uid }) { asset ->
                        VaultAssetCard(
                            asset = asset,
                            qualityProfile = qualityProfile,
                            onInspect = { onOpenInspector(asset) },
                            onOptimize = { onReoptimize(asset.uid, qualityProfile) }
                        )
                    }
                }
            }
        }
    }

    // 3D Model Inspector + Prefab Binder Bottom Sheet
    val inspected = vaultState.inspectedAsset
    if (inspected != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseInspector,
            containerColor = CarbonSurface
        ) {
            var zoom by remember { mutableFloatStateOf(4.0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(inspected.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "UID ${inspected.uid} • ${inspected.status}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                    IconButton(onClick = onCloseInspector, modifier = Modifier.testTag("close_inspector_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Close 3D Inspector")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CarbonSurfaceElevated)
                ) {
                    ModelInspectorGLView(
                        parsedMesh = vaultState.inspectedMesh,
                        zoomDist = zoom,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (vaultState.isParsingMesh) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    Surface(
                        color = CarbonSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        val mesh = vaultState.inspectedMesh
                        Text(
                            text = if (mesh != null) {
                                "glTF Loaded • ${mesh.triangleCount} tris • ${mesh.submeshCount} submeshes"
                            } else {
                                "Procedural Preview (Download glTF to inspect source mesh)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 2.0f..8.0f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("BIND TO GAME PREFAB SLOT", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableSlots) { slot ->
                        val isBound = inspected.boundGameSlot == slot
                        FilterChip(
                            selected = isBound,
                            enabled = inspected.mainModelPath != null,
                            onClick = { onBindSlot(inspected.uid, slot) },
                            label = { Text(slot) },
                            modifier = Modifier.testTag("bind_slot_$slot")
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun VaultAssetCard(
    asset: AssetEntity,
    qualityProfile: QualityProfile,
    onInspect: () -> Unit,
    onOptimize: () -> Unit
) {
    val statusColor = when (asset.status) {
        IntegrationStatus.TESTED.name,
        IntegrationStatus.GAME_INTEGRATED.name,
        IntegrationStatus.PREFAB_CREATED.name -> EmeraldVerified
        IntegrationStatus.FAILED.name -> CrimsonAlert
        IntegrationStatus.IMPORTED.name,
        IntegrationStatus.OPTIMIZED.name -> ElectricCyan
        else -> AmberGold
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = asset.thumbnailUrl,
                    contentDescription = asset.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurfaceElevated),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${asset.creator} • Score ${asset.scoreTotal}/100 • ${asset.faceCount} tris",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = asset.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        if (asset.boundGameSlot != null) {
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricCyan)
                            ) {
                                Text(
                                    text = asset.boundGameSlot,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (asset.failureReason != null) {
                Text(
                    text = asset.failureReason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonAlert
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("inspect_asset_${asset.uid}")
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inspect 3D")
                }
                Button(
                    onClick = onOptimize,
                    enabled = asset.extractedDirPath != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("optimize_asset_${asset.uid}")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Optimize (${qualityProfile.maxTexturePx}px)")
                }
            }
        }
    }
}
