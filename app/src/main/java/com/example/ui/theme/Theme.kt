package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MetroDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ColorTokens.CyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = AmberGold,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberGold,
    tertiary = NeonMagenta,
    onTertiary = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = CarbonSurfaceElevated,
    surfaceContainerHigh = CarbonSurfaceHighest,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = CrimsonAlert,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonAlert,
)

private object ColorTokens {
    val CyanContainer = androidx.compose.ui.graphics.Color(0xFF003642)
}

val MetroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MetroLegendsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MetroDarkColorScheme,
        typography = MetroTypography,
        shapes = MetroShapes,
        content = content
    )
}
