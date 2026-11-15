package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AssetDatabase
import com.example.data.local.AssetEntity
import com.example.data.local.PlayerSettings
import com.example.data.local.SettingsDataStore
import com.example.data.sketchfab.SearchCategory
import com.example.data.sketchfab.SketchfabOEmbedClient
import com.example.data.sketchfab.SketchfabOEmbedResponse
import com.example.game.GameEnvironment
import com.example.game.PlayableCharacter
import com.example.game.RunnerHudState
import com.example.game.RunnerWorldGenerator
import com.example.pipeline.AssetPipelineConfig
import com.example.pipeline.AssetPipelineCoordinator
import com.example.pipeline.EvaluatedCandidate
import com.example.pipeline.GeneratedReportBundle
import com.example.pipeline.GltfMeshParser
import com.example.pipeline.ParsedGltfMesh
import com.example.pipeline.PipelineActionResult
import com.example.pipeline.QualityProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DiscoveryUiState(
    val selectedCategory: SearchCategory = SearchCategory.METRO_CITY,
    val queryText: String = SearchCategory.METRO_CITY.searchTerms.first(),
    val isSearching: Boolean = false,
    val importingUid: String? = null,
    val batchProgressText: String? = null,
    val candidates: List<EvaluatedCandidate> = emptyList(),
    val errorMessage: String? = null,
    val accountStatusText: String? = null
)

data class VaultUiState(
    val folderSnapshot: List<Pair<String, Int>> = emptyList(),
    val inspectedAsset: AssetEntity? = null,
    val inspectedMesh: ParsedGltfMesh? = null,
    val isParsingMesh: Boolean = false,
    val showFolderTree: Boolean = false
)

data class OEmbedPreviewState(
    val isVisible: Boolean = false,
    val title: String = "Sketchfab 3D Model",
    val isLoading: Boolean = false,
    val response: SketchfabOEmbedResponse? = null,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AssetDatabase.getInstance(application)
    private val dao = db.assetDao()
    private val settingsStore = SettingsDataStore(application)
    private val coordinator = AssetPipelineCoordinator(application, dao)

    val world = RunnerWorldGenerator()

    val settings: StateFlow<PlayerSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerSettings())

    val allAssets: StateFlow<List<AssetEntity>> = dao.observeAllAssets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _hudState = MutableStateFlow(RunnerHudState())
    val hudState: StateFlow<RunnerHudState> = _hudState.asStateFlow()

    private val _customMeshBindings = MutableStateFlow<Map<String, ParsedGltfMesh>>(emptyMap())
    val customMeshBindings: StateFlow<Map<String, ParsedGltfMesh>> = _customMeshBindings.asStateFlow()

    private val _discoveryState = MutableStateFlow(DiscoveryUiState())
    val discoveryState: StateFlow<DiscoveryUiState> = _discoveryState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    private val _oEmbedState = MutableStateFlow(OEmbedPreviewState())
    val oEmbedState: StateFlow<OEmbedPreviewState> = _oEmbedState.asStateFlow()

    private val _reports = MutableStateFlow<GeneratedReportBundle?>(null)
    val reports: StateFlow<GeneratedReportBundle?> = _reports.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        world.onGameOverCallback = { meters, coins ->
            viewModelScope.launch {
                settingsStore.recordRunResult(meters, coins)
            }
        }

        viewModelScope.launch {
            settings.collectLatest { s ->
                world.environment = GameEnvironment.fromId(s.selectedEnvironmentId)
                world.character = PlayableCharacter.fromId(s.selectedCharacterId)
                if (!world.isPlaying && !world.isGameOver) {
                    world.resetWorld(startImmediately = false)
                }
            }
        }

        viewModelScope.launch {
            dao.observeBoundAssets().collectLatest { bound ->
                val map = withContext(Dispatchers.IO) {
                    val loaded = mutableMapOf<String, ParsedGltfMesh>()
                    for (asset in bound) {
                        val slot = asset.boundGameSlot ?: continue
                        val path = asset.mainModelPath ?: continue
                        val parsed = GltfMeshParser.parseFromFile(File(path)).getOrNull()
                        if (parsed != null && parsed.interleavedTriangles.isNotEmpty()) {
                            loaded[slot] = parsed
                        }
                    }
                    loaded
                }
                _customMeshBindings.value = map
            }
        }

        refreshFolderSnapshot()
        refreshReports()
    }

    fun updateHud(state: RunnerHudState) {
        _hudState.value = state
    }

    fun startRun() {
        world.resetWorld(startImmediately = true)
    }

    fun togglePause() {
        if (world.isPlaying && !world.isGameOver) {
            world.isPaused = !world.isPaused
        }
    }

    fun resetToLobby() {
        world.resetWorld(startImmediately = false)
    }

    fun selectEnvironment(env: GameEnvironment) {
        viewModelScope.launch {
            settingsStore.setSelectedEnvironment(env.id)
            world.environment = env
            world.resetWorld(startImmediately = false)
        }
    }

    fun selectCharacter(character: PlayableCharacter) {
        viewModelScope.launch {
            settingsStore.setSelectedCharacter(character.id)
            world.character = character
        }
    }

    fun selectQualityProfile(profile: QualityProfile) {
        viewModelScope.launch {
            settingsStore.setQualityProfile(profile)
            emitMessage("Optimization profile switched to ${profile.displayName}")
        }
    }

    fun selectSearchCategory(category: SearchCategory) {
        _discoveryState.value = _discoveryState.value.copy(
            selectedCategory = category,
            queryText = category.searchTerms.first(),
            errorMessage = null
        )
    }

    fun updateQueryText(text: String) {
        _discoveryState.value = _discoveryState.value.copy(queryText = text)
    }

    fun runSearch(customQuery: String? = null) {
        val state = _discoveryState.value
        val query = (customQuery ?: state.queryText).trim()
        if (query.isEmpty()) return

        _discoveryState.value = state.copy(
            queryText = query,
            isSearching = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = coordinator.searchAndScore(state.selectedCategory, query)
            result.fold(
                onSuccess = { list ->
                    _discoveryState.value = _discoveryState.value.copy(
                        isSearching = false,
                        candidates = list,
                        errorMessage = if (list.isEmpty()) "No downloadable models found for \"$query\"" else null
                    )
                    refreshReports()
                },
                onFailure = { ex ->
                    _discoveryState.value = _discoveryState.value.copy(
                        isSearching = false,
                        errorMessage = ex.message ?: "Sketchfab search failed"
                    )
                }
            )
        }
    }

    fun verifySketchfabAuth() {
        viewModelScope.launch {
            if (!AssetPipelineConfig.isDownloadAuthorized) {
                _discoveryState.value = _discoveryState.value.copy(
                    accountStatusText = "Not configured — Add SKETCHFAB_API_TOKEN in AI Studio Secrets"
                )
                return@launch
            }
            val res = coordinator.verifyAccountAuth()
            res.fold(
                onSuccess = { msg ->
                    _discoveryState.value = _discoveryState.value.copy(accountStatusText = msg)
                    emitMessage(msg)
                },
                onFailure = { ex ->
                    _discoveryState.value = _discoveryState.value.copy(
                        accountStatusText = ex.message ?: "Verification failed"
                    )
                }
            )
        }
    }

    fun importCandidate(candidate: EvaluatedCandidate) {
        _discoveryState.value = _discoveryState.value.copy(importingUid = candidate.model.uid)
        viewModelScope.launch {
            val res = coordinator.runFullImportPipeline(candidate, settings.value.qualityProfile)
            _discoveryState.value = _discoveryState.value.copy(importingUid = null)
            when (res) {
                is PipelineActionResult.Success -> {
                    emitMessage(res.message)
                    refreshFolderSnapshot()
                    refreshReports()
                }
                is PipelineActionResult.AuthNeeded -> {
                    emitMessage(res.message)
                    _discoveryState.value = _discoveryState.value.copy(accountStatusText = res.message)
                }
                is PipelineActionResult.Failure -> {
                    emitMessage(res.message)
                    refreshReports()
                }
            }
        }
    }

    fun autoAcquireStarterPack() {
        if (_discoveryState.value.batchProgressText != null) return
        _discoveryState.value = _discoveryState.value.copy(
            batchProgressText = "Initializing Sketchfab 3D Starter Pack..."
        )
        viewModelScope.launch {
            val res = coordinator.autoAcquireStarterPack(settings.value.qualityProfile) { step ->
                _discoveryState.value = _discoveryState.value.copy(batchProgressText = step)
            }
            _discoveryState.value = _discoveryState.value.copy(batchProgressText = null)
            res.fold(
                onSuccess = { summary ->
                    emitMessage(summary)
                    refreshFolderSnapshot()
                    refreshReports()
                },
                onFailure = { ex ->
                    emitMessage(ex.message ?: "Auto-acquire failed")
                }
            )
        }
    }

    fun toggleVaultViewMode(showFolders: Boolean) {
        _vaultState.value = _vaultState.value.copy(showFolderTree = showFolders)
        if (showFolders) refreshFolderSnapshot()
    }

    fun refreshFolderSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val snap = coordinator.layout.treeSnapshot()
            _vaultState.value = _vaultState.value.copy(folderSnapshot = snap)
        }
    }

    fun openInspector(asset: AssetEntity) {
        _vaultState.value = _vaultState.value.copy(
            inspectedAsset = asset,
            inspectedMesh = null,
            isParsingMesh = asset.mainModelPath != null
        )
        val path = asset.mainModelPath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = GltfMeshParser.parseFromFile(File(path)).getOrNull()
            _vaultState.value = _vaultState.value.copy(
                inspectedMesh = parsed,
                isParsingMesh = false
            )
        }
    }

    fun closeInspector() {
        _vaultState.value = _vaultState.value.copy(
            inspectedAsset = null,
            inspectedMesh = null,
            isParsingMesh = false
        )
    }

    fun bindAssetToSlot(uid: String, slot: String) {
        viewModelScope.launch {
            coordinator.bindAssetToGameSlot(uid, slot).fold(
                onSuccess = { updated ->
                    _vaultState.value = _vaultState.value.copy(inspectedAsset = updated)
                    emitMessage("Bound ${updated.name} -> $slot (TESTED)")
                    refreshReports()
                },
                onFailure = { ex ->
                    emitMessage(ex.message ?: "Failed to bind prefab slot")
                }
            )
        }
    }

    fun reoptimizeAsset(uid: String, profile: QualityProfile) {
        viewModelScope.launch {
            coordinator.reoptimizeAsset(uid, profile).fold(
                onSuccess = { m ->
                    val savedKb = ((m.originalTextureBytes - m.optimizedTextureBytes) / 1024).coerceAtLeast(0)
                    emitMessage("Optimized ${m.texturesProcessed} textures for ${profile.displayName} (saved ${savedKb}KB)")
                    refreshReports()
                },
                onFailure = { ex ->
                    emitMessage(ex.message ?: "Optimization failed")
                }
            )
        }
    }

    fun availableGameSlots(): List<String> = coordinator.availableGameSlots()

    fun refreshReports() {
        viewModelScope.launch {
            _reports.value = coordinator.refreshReports()
        }
    }

    fun openOEmbedPreview(uidOrUrl: String, title: String = "Sketchfab 3D Viewer") {
        val targetUrl = SketchfabOEmbedClient.normalizeModelUrl(uidOrUrl)
        _oEmbedState.value = OEmbedPreviewState(
            isVisible = true,
            title = title,
            isLoading = true
        )
        viewModelScope.launch(Dispatchers.IO) {
            val res = runCatching {
                val resp = SketchfabOEmbedClient.api.getOEmbed(targetUrl)
                if (!resp.isSuccessful) error("oEmbed HTTP ${resp.code()}")
                resp.body() ?: error("Empty oEmbed response")
            }
            res.fold(
                onSuccess = { data ->
                    _oEmbedState.value = OEmbedPreviewState(
                        isVisible = true,
                        title = data.title.ifBlank { title },
                        isLoading = false,
                        response = data
                    )
                },
                onFailure = { ex ->
                    _oEmbedState.value = OEmbedPreviewState(
                        isVisible = true,
                        title = title,
                        isLoading = false,
                        error = ex.message ?: "Failed to load oEmbed viewer"
                    )
                }
            )
        }
    }

    fun closeOEmbedPreview() {
        _oEmbedState.value = OEmbedPreviewState(isVisible = false)
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    private fun emitMessage(msg: String) {
        _snackbarMessage.value = msg
    }
}
