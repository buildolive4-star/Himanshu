package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.R

enum class GameEnvironment(
    val id: String,
    val title: String,
    val subtitle: String,
    val bannerRes: Int,
    val segmentIds: List<String>,
    val skyColor: FloatArray,
    val fogColor: FloatArray,
    val trackColor: FloatArray,
    val accentHex: Color
) {
    METRO_CITY(
        id = "metro_city",
        title = "Metro City",
        subtitle = "Subway rails, express trains & steel arches",
        bannerRes = R.drawable.img_env_metro_city_1794742389608,
        segmentIds = listOf("MetroSegment_01", "MetroSegment_02", "MetroSegment_03"),
        skyColor = floatArrayOf(0.05f, 0.08f, 0.16f, 1f),
        fogColor = floatArrayOf(0.06f, 0.10f, 0.20f, 1f),
        trackColor = floatArrayOf(0.18f, 0.22f, 0.30f),
        accentHex = Color(0xFF00E5FF)
    ),
    ANCIENT_TEMPLE(
        id = "ancient_temple",
        title = "Ancient Temple",
        subtitle = "Sandstone ruins, rolling boulders & fire traps",
        bannerRes = R.drawable.img_env_ancient_temple_1794742390168,
        segmentIds = listOf("TempleSegment_01", "TempleSegment_02", "TempleSegment_03"),
        skyColor = floatArrayOf(0.20f, 0.14f, 0.08f, 1f),
        fogColor = floatArrayOf(0.24f, 0.17f, 0.10f, 1f),
        trackColor = floatArrayOf(0.45f, 0.37f, 0.26f),
        accentHex = Color(0xFFFFB300)
    ),
    JUNGLE(
        id = "jungle",
        title = "Jungle",
        subtitle = "Wooden bridges, giant roots & waterfall canyon",
        bannerRes = R.drawable.img_env_jungle_1794742390083,
        segmentIds = listOf("JungleSegment_01", "JungleSegment_02", "JungleSegment_03"),
        skyColor = floatArrayOf(0.06f, 0.18f, 0.14f, 1f),
        fogColor = floatArrayOf(0.08f, 0.22f, 0.17f, 1f),
        trackColor = floatArrayOf(0.36f, 0.25f, 0.16f),
        accentHex = Color(0xFF00E676)
    ),
    CYBER_CITY(
        id = "cyber_city",
        title = "Cyber City",
        subtitle = "Maglev skyline, neon rings & holograms",
        bannerRes = R.drawable.img_env_cyber_city_1794742394323,
        segmentIds = listOf("CyberSegment_01", "CyberSegment_02", "CyberSegment_03"),
        skyColor = floatArrayOf(0.04f, 0.03f, 0.11f, 1f),
        fogColor = floatArrayOf(0.08f, 0.04f, 0.18f, 1f),
        trackColor = floatArrayOf(0.11f, 0.11f, 0.22f),
        accentHex = Color(0xFFFF3D9A)
    );

    companion object {
        fun fromId(id: String): GameEnvironment = entries.firstOrNull { it.id == id } ?: METRO_CITY
    }
}

enum class PlayableCharacter(
    val id: String,
    val displayName: String,
    val prefabName: String,
    val roleTag: String,
    val primaryRgb: FloatArray,
    val secondaryRgb: FloatArray,
    val accentColor: Color
) {
    ALEX(
        id = "alex",
        displayName = "Alex",
        prefabName = "PF_Character_Alex",
        roleTag = "Sky Runner",
        primaryRgb = floatArrayOf(0.05f, 0.78f, 0.96f),
        secondaryRgb = floatArrayOf(0.15f, 0.20f, 0.30f),
        accentColor = Color(0xFF00E5FF)
    ),
    MIA(
        id = "mia",
        displayName = "Mia",
        prefabName = "PF_Character_Mia",
        roleTag = "Neon Sprinter",
        primaryRgb = floatArrayOf(0.96f, 0.24f, 0.60f),
        secondaryRgb = floatArrayOf(0.98f, 0.78f, 0.15f),
        accentColor = Color(0xFFFF3D9A)
    ),
    RYAN(
        id = "ryan",
        displayName = "Ryan",
        prefabName = "PF_Character_Ryan",
        roleTag = "Rail Vanguard",
        primaryRgb = floatArrayOf(0.24f, 0.48f, 0.96f),
        secondaryRgb = floatArrayOf(0.95f, 0.56f, 0.12f),
        accentColor = Color(0xFF448AFF)
    ),
    ZARA(
        id = "zara",
        displayName = "Zara",
        prefabName = "PF_Character_Zara",
        roleTag = "Cyber Scout",
        primaryRgb = floatArrayOf(0.05f, 0.88f, 0.52f),
        secondaryRgb = floatArrayOf(0.12f, 0.16f, 0.24f),
        accentColor = Color(0xFF00E676)
    ),
    SHADOW(
        id = "shadow",
        displayName = "Shadow",
        prefabName = "PF_Character_Shadow",
        roleTag = "Night Phantom",
        primaryRgb = floatArrayOf(0.16f, 0.18f, 0.25f),
        secondaryRgb = floatArrayOf(0.95f, 0.22f, 0.28f),
        accentColor = Color(0xFFFF5252)
    ),
    EXPLORER(
        id = "explorer",
        displayName = "Explorer",
        prefabName = "PF_Character_Explorer",
        roleTag = "Relic Seeker",
        primaryRgb = floatArrayOf(0.86f, 0.66f, 0.25f),
        secondaryRgb = floatArrayOf(0.32f, 0.48f, 0.28f),
        accentColor = Color(0xFFFFB300)
    );

    companion object {
        fun fromId(id: String): PlayableCharacter = entries.firstOrNull { it.id == id } ?: ALEX
    }
}

enum class ObstacleKind {
    LOW_BARRIER,
    HIGH_GATE,
    TRAIN,
    ROLLING_BOULDER,
    FIRE_TRAP,
    PILLAR
}

enum class PowerUpKind {
    SHIELD,
    HOVERBOARD,
    MAGNET
}

data class RunnerHudState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val distanceMeters: Int = 0,
    val coinsInRun: Int = 0,
    val speedKmh: Int = 55,
    val activeSegmentId: String = "MetroSegment_01",
    val pooledSegmentsCount: Int = 6,
    val activePowerUp: String? = null,
    val hasShield: Boolean = false,
    val hoverboardSeconds: Float = 0f,
    val magnetSeconds: Float = 0f,
    val activeCustomPrefabs: Int = 0
)
