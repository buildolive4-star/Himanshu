package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SketchfabOEmbedBottomSheet
import com.example.ui.screens.AssetVaultScreen
import com.example.ui.screens.DiscoveryScreen
import com.example.ui.screens.ReportsCreditsScreen
import com.example.ui.screens.RunnerScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ObsidianBg

enum class AppDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    PLAY("Play", Icons.AutoMirrored.Filled.DirectionsRun, Icons.AutoMirrored.Outlined.DirectionsRun, "nav_play"),
    DISCOVER("Sketchfab", Icons.Filled.TravelExplore, Icons.Outlined.TravelExplore, "nav_discover"),
    VAULT("Assets", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_vault"),
    REPORTS("Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment, "nav_reports")
}

@Composable
fun MetroLegendsApp(viewModel: MainViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(AppDestination.PLAY) }
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val customBindings by viewModel.customMeshBindings.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val allAssets by viewModel.allAssets.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val oEmbedState by viewModel.oEmbedState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    if (currentTab != AppDestination.PLAY) {
        BackHandler {
            currentTab = AppDestination.PLAY
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail && !(currentTab == AppDestination.PLAY && hud.isPlaying)) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !(currentTab == AppDestination.PLAY && hud.isPlaying)) {
                    NavigationRail(containerColor = CarbonSurface) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.tag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0A2A42),
                                        Color(0xFF070B14)
                                    ),
                                    center = Offset(size.width * 0.8f, size.height * 0.12f),
                                    radius = size.maxDimension * 0.85f
                                )
                            )
                            val step = 64f
                            var x = 0f
                            while (x < size.width) {
                                drawLine(
                                    color = Color(0x0D00E5FF),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f
                                )
                                x += step
                            }
                        }
                ) {
                    when (currentTab) {
                        AppDestination.PLAY -> RunnerScreen(
                            world = viewModel.world,
                            settings = settings,
                            hud = hud,
                            customBindings = customBindings,
                            onHudUpdate = viewModel::updateHud,
                            onStartRun = viewModel::startRun,
                            onTogglePause = viewModel::togglePause,
                            onBackToLobby = viewModel::resetToLobby,
                            onSelectEnvironment = viewModel::selectEnvironment,
                            onSelectCharacter = viewModel::selectCharacter,
                            onSelectQuality = viewModel::selectQualityProfile
                        )

                        AppDestination.DISCOVER -> DiscoveryScreen(
                            state = discoveryState,
                            onSelectCategory = viewModel::selectSearchCategory,
                            onQueryChange = viewModel::updateQueryText,
                            onRunSearch = viewModel::runSearch,
                            onVerifyAccount = viewModel::verifySketchfabAuth,
                            onImportCandidate = viewModel::importCandidate,
                            onAutoAcquireStarterPack = viewModel::autoAcquireStarterPack,
                            onOpenOEmbed = viewModel::openOEmbedPreview,
                            modifier = Modifier.statusBarsPadding()
                        )

                        AppDestination.VAULT -> AssetVaultScreen(
                            assets = allAssets,
                            vaultState = vaultState,
                            qualityProfile = settings.qualityProfile,
                            availableSlots = viewModel.availableGameSlots(),
                            onToggleFolderTree = viewModel::toggleVaultViewMode,
                            onOpenInspector = viewModel::openInspector,
                            onCloseInspector = viewModel::closeInspector,
                            onReoptimize = viewModel::reoptimizeAsset,
                            onBindSlot = viewModel::bindAssetToSlot,
                            modifier = Modifier.statusBarsPadding()
                        )

                        AppDestination.REPORTS -> ReportsCreditsScreen(
                            reports = reports,
                            onRefreshReports = viewModel::refreshReports,
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                }
            }
        }
    }

    if (oEmbedState.isVisible) {
        SketchfabOEmbedBottomSheet(
            title = oEmbedState.title,
            oEmbed = oEmbedState.response,
            isLoading = oEmbedState.isLoading,
            error = oEmbedState.error,
            onDismiss = viewModel::closeOEmbedPreview
        )
    }
}
