package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerSettings
import com.example.game.GameEnvironment
import com.example.game.PlayableCharacter
import com.example.game.RunnerGLSurface
import com.example.game.RunnerHudState
import com.example.game.RunnerWorldGenerator
import com.example.pipeline.ParsedGltfMesh
import com.example.pipeline.QualityProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RunnerScreen(
    world: RunnerWorldGenerator,
    settings: PlayerSettings,
    hud: RunnerHudState,
    customBindings: Map<String, ParsedGltfMesh>,
    onHudUpdate: (RunnerHudState) -> Unit,
    onStartRun: () -> Unit,
    onTogglePause: () -> Unit,
    onBackToLobby: () -> Unit,
    onSelectEnvironment: (GameEnvironment) -> Unit,
    onSelectCharacter: (PlayableCharacter) -> Unit,
    onSelectQuality: (QualityProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEnv = GameEnvironment.fromId(settings.selectedEnvironmentId)
    val activeChar = PlayableCharacter.fromId(settings.selectedCharacterId)
    val inActiveSession = hud.isPlaying || hud.isPaused || hud.isGameOver

    Box(modifier = modifier.fillMaxSize()) {
        if (inActiveSession) {
            // Live 60fps VBO-accelerated OpenGL ES 3D Viewport
            RunnerGLSurface(
                world = world,
                isActiveRunning = hud.isPlaying && !hud.isPaused && !hud.isGameOver,
                qualityProfile = settings.qualityProfile,
                customBindings = customBindings,
                onHudUpdate = onHudUpdate,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("runner_gl_surface")
            )
        } else {
            // Zero-lag crisp AAA environment backdrop in Lobby
            Image(
                painter = painterResource(id = activeEnv.bannerRes),
                contentDescription = activeEnv.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Active Gameplay HUD
        if (hud.isPlaying || hud.isPaused) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Telemetry Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "${hud.distanceMeters} m",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ElectricCyan
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Coins collected",
                                        tint = AmberGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${hud.coinsInRun}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AmberGold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Speed",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${hud.speedKmh} km/h",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${hud.activeSegmentId} • Pool(${hud.pooledSegmentsCount}) • ${settings.qualityProfile.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hud.activePowerUp != null) {
                            Surface(
                                color = ElectricCyan.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, ElectricCyan)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Active power-up",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = hud.activePowerUp,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }

                        FilledTonalIconButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("pause_run_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = ObsidianBg.copy(alpha = 0.8f),
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = if (hud.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (hud.isPaused) "Resume run" else "Pause run"
                            )
                        }
                    }
                }

                // Pause Overlay
                if (hud.isPaused) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface.copy(alpha = 0.94f)),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("RUN PAUSED", style = MaterialTheme.typography.headlineSmall)
                            Button(
                                onClick = onTogglePause,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("resume_run_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Resume")
                            }
                            OutlinedButton(
                                onClick = onBackToLobby,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("exit_to_lobby_button")
                            ) {
                                Text("Return to Staging Lobby")
                            }
                        }
                    }
                }

                // Thumb-friendly Controls Bar (in addition to full-screen swipe gestures)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlPadButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            label = "Left",
                            tag = "move_left_button",
                            onClick = { world.moveLeft() }
                        )
                        ControlPadButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            label = "Right",
                            tag = "move_right_button",
                            onClick = { world.moveRight() }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlPadButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "Slide",
                            tag = "slide_button",
                            onClick = { world.slide() }
                        )
                        ControlPadButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "Jump",
                            tag = "jump_button",
                            onClick = { world.jump() }
                        )
                    }
                }
            }
        }

        // Pre-Run Staging Lobby Overlay
        AnimatedVisibility(
            visible = !hud.isPlaying && !hud.isPaused && !hud.isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ObsidianBg.copy(alpha = 0.72f),
                                ObsidianBg.copy(alpha = 0.42f),
                                ObsidianBg.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .align(Alignment.Center)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "METRO LEGENDS",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "3D Infinite Run • ${customBindings.size} Sketchfab Prefabs Bound",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Surface(
                            color = CarbonSurface.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Best Distance", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${settings.highScoreMeters}m", style = MaterialTheme.typography.labelLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = "Total Coins", tint = AmberGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${settings.totalCoins}", style = MaterialTheme.typography.labelLarge, color = AmberGold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Environment Selector Cards
                    Text("WORLD ENVIRONMENT", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(GameEnvironment.entries) { env ->
                            val selected = env == activeEnv
                            Card(
                                modifier = Modifier
                                    .width(210.dp)
                                    .clickable { onSelectEnvironment(env) }
                                    .testTag("env_card_${env.id}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) CarbonSurfaceElevated else CarbonSurface.copy(alpha = 0.88f)
                                ),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) env.accentHex else SlateBorder)
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(id = env.bannerRes),
                                        contentDescription = env.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(92.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = env.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) env.accentHex else TextPrimary
                                        )
                                        Text(
                                            text = env.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Playable Characters
                    Text("RUNNER PREFAB (${activeChar.prefabName})", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlayableCharacter.entries) { ch ->
                            val selected = ch == activeChar
                            val hasCustomMesh = customBindings.containsKey(ch.prefabName)
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCharacter(ch) },
                                label = {
                                    Text(
                                        if (hasCustomMesh) "${ch.displayName} (glTF)" else ch.displayName,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ch.accentColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ch.accentColor.copy(alpha = 0.2f),
                                    selectedLabelColor = ch.accentColor
                                ),
                                modifier = Modifier.testTag("char_chip_${ch.id}")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Mobile Optimization Profile
                    Text("MOBILE 3D QUALITY PROFILE", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QualityProfile.entries.forEach { profile ->
                            val selected = profile == settings.qualityProfile
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectQuality(profile) },
                                label = { Text(profile.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quality_chip_${profile.name}")
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Start Run CTA
                    Button(
                        onClick = onStartRun,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_run_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeEnv.accentHex,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LAUNCH ${activeEnv.title.uppercase()} RUN",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Game Over Summary Overlay
        AnimatedVisibility(
            visible = hud.isGameOver,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.78f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 400.dp),
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    border = BorderStroke(1.dp, activeEnv.accentHex)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("RUN COMPLETE", style = MaterialTheme.typography.headlineMedium, color = activeEnv.accentHex)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DISTANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${hud.distanceMeters} m", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("COINS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${hud.coinsInRun}", style = MaterialTheme.typography.headlineSmall, color = AmberGold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BEST", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${settings.highScoreMeters} m", style = MaterialTheme.typography.headlineSmall, color = EmeraldVerified)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onStartRun,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_again_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Run Again")
                        }
                        OutlinedButton(
                            onClick = onBackToLobby,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("game_over_lobby_button")
                        ) {
                            Text("Change World / Character")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .border(1.dp, SlateBorder, CircleShape)
            .testTag(tag),
        shape = CircleShape,
        color = ObsidianBg.copy(alpha = 0.72f),
        contentColor = ElectricCyan
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(30.dp))
        }
    }
}
