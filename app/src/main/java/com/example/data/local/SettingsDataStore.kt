package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pipeline.QualityProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "metro_legends_prefs")

data class PlayerSettings(
    val qualityProfile: QualityProfile = QualityProfile.MEDIUM,
    val selectedEnvironmentId: String = "metro_city",
    val selectedCharacterId: String = "alex",
    val highScoreMeters: Int = 0,
    val totalCoins: Int = 0
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val QUALITY = stringPreferencesKey("quality_profile")
        val ENVIRONMENT = stringPreferencesKey("selected_environment")
        val CHARACTER = stringPreferencesKey("selected_character")
        val HIGH_SCORE = intPreferencesKey("high_score_meters")
        val TOTAL_COINS = intPreferencesKey("total_coins")
    }

    val settingsFlow: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        val quality = prefs[Keys.QUALITY]?.let {
            runCatching { QualityProfile.valueOf(it) }.getOrNull()
        } ?: QualityProfile.MEDIUM
        PlayerSettings(
            qualityProfile = quality,
            selectedEnvironmentId = prefs[Keys.ENVIRONMENT] ?: "metro_city",
            selectedCharacterId = prefs[Keys.CHARACTER] ?: "alex",
            highScoreMeters = prefs[Keys.HIGH_SCORE] ?: 0,
            totalCoins = prefs[Keys.TOTAL_COINS] ?: 0
        )
    }

    suspend fun setQualityProfile(profile: QualityProfile) {
        context.dataStore.edit { it[Keys.QUALITY] = profile.name }
    }

    suspend fun setSelectedEnvironment(envId: String) {
        context.dataStore.edit { it[Keys.ENVIRONMENT] = envId }
    }

    suspend fun setSelectedCharacter(charId: String) {
        context.dataStore.edit { it[Keys.CHARACTER] = charId }
    }

    suspend fun recordRunResult(meters: Int, coinsCollected: Int) {
        context.dataStore.edit { prefs ->
            val prevHigh = prefs[Keys.HIGH_SCORE] ?: 0
            if (meters > prevHigh) {
                prefs[Keys.HIGH_SCORE] = meters
            }
            val prevCoins = prefs[Keys.TOTAL_COINS] ?: 0
            prefs[Keys.TOTAL_COINS] = prevCoins + coinsCollected
        }
    }
}
