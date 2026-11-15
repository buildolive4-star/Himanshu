package com.example.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pipeline.ParsedGltfMesh
import com.example.pipeline.QualityProfile
import java.util.concurrent.ConcurrentHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.sin

class RunnerGLRenderer(
    private val world: RunnerWorldGenerator,
    private val onHudUpdate: (RunnerHudState) -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var skyProgram: SkyBackdropProgram
    private lateinit var shader: MeshShaderProgram
    private lateinit var models: ProceduralModelBank
    private val customCpuMeshes = ConcurrentHashMap<String, ParsedGltfMesh>()
    private val customGpuMeshes = ConcurrentHashMap<String, GpuMesh>()

    @Volatile
    var qualityProfile: QualityProfile = QualityProfile.MEDIUM

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNs = System.nanoTime()
    private var totalTimeSec = 0f
    private var hudAccumulator = 0f
    private var aspectRatio = 1f

    fun setCustomMeshBindings(bindings: Map<String, ParsedGltfMesh>) {
        customCpuMeshes.clear()
        customCpuMeshes.putAll(bindings)
        customGpuMeshes.clear()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        skyProgram = SkyBackdropProgram()
        shader = MeshShaderProgram()
        models = ProceduralModelBank()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 58f, aspectRatio, 0.2f, 160f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now
        totalTimeSec += dt

        val hud = world.update(dt)
        hudAccumulator += dt
        if (hudAccumulator >= 0.14f || hud.isGameOver) {
            hudAccumulator = 0f
            onHudUpdate(hud.copy(activeCustomPrefabs = customCpuMeshes.size))
        }

        val env = world.environment
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Render smooth vertex-colored sky backdrop
        skyProgram.draw(env)

        // 2. Dynamic FOV scaling with speed & hoverboard
        val speedFactor = ((world.speed - 16f) / 18f).coerceIn(0f, 1f)
        val hoverBoost = if (world.hoverboardTimer > 0f) 4.0f else 0f
        Matrix.perspectiveM(projMatrix, 0, 56f + speedFactor * 5f + hoverBoost, aspectRatio, 0.2f, 145f)

        GLES20.glUseProgram(shader.programId)

        val camX = world.playerX * 0.45f
        val camY = 3.65f + world.playerY * 0.35f
        val camZ = 6.3f
        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            world.playerX * 0.32f, 1.35f + world.playerY * 0.28f, -16f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Normalized directional sun vector
        GLES20.glUniform3f(shader.uLightDir, 0.42f, 0.77f, 0.48f)
        GLES20.glUniform3f(shader.uFogColor, env.fogColor[0], env.fogColor[1], env.fogColor[2])
        val fogDensity = when (qualityProfile) {
            QualityProfile.LOW -> 0.015f
            QualityProfile.MEDIUM -> 0.011f
            QualityProfile.HIGH -> 0.0085f
        }
        GLES20.glUniform1f(shader.uFogDensity, fogDensity)
        GLES20.glUniform3f(shader.uCameraPos, camX, camY, camZ)

        val trackMesh = when (env) {
            GameEnvironment.METRO_CITY -> models.trackTileMetro
            GameEnvironment.ANCIENT_TEMPLE -> models.trackTileTemple
            GameEnvironment.JUNGLE -> models.trackTileJungle
            GameEnvironment.CYBER_CITY -> models.trackTileCyber
        }
        val sideMesh = when (env) {
            GameEnvironment.METRO_CITY -> resolveMesh("PF_CityBuilding", models.cityBuildingMesh)
            GameEnvironment.ANCIENT_TEMPLE -> resolveMesh("PF_TemplePillar", models.pillarMesh)
            GameEnvironment.JUNGLE -> resolveMesh("PF_JungleTree", models.jungleTreeMesh)
            GameEnvironment.CYBER_CITY -> resolveMesh("PF_CyberBuilding", models.cyberBuildingMesh)
        }
        val archMesh = when (env) {
            GameEnvironment.METRO_CITY -> models.metroArchMesh
            GameEnvironment.ANCIENT_TEMPLE -> models.templeArchMesh
            GameEnvironment.JUNGLE -> null
            GameEnvironment.CYBER_CITY -> models.cyberRingArchMesh
        }

        val maxDrawZ = when (qualityProfile) {
            QualityProfile.LOW -> -74f
            QualityProfile.MEDIUM -> -98f
            QualityProfile.HIGH -> -118f
        }

        for (seg in world.segments) {
            drawMeshAt(trackMesh, 0f, 0f, seg.zPos)

            if (seg.zPos > maxDrawZ) {
                // Overhead arches every other segment
                if (archMesh != null && seg.poolIndex % 2 == 0) {
                    drawMeshAt(archMesh, 0f, 0f, seg.zPos - 8f)
                }

                // Foreground roadside props + street lamps
                if (env == GameEnvironment.METRO_CITY || env == GameEnvironment.CYBER_CITY) {
                    drawMeshAt(models.streetLampMesh, -4.3f, 0f, seg.zPos - 3f)
                    drawMeshAt(models.streetLampMesh, 4.3f, 0f, seg.zPos - 11f, rotY = 180f)
                }

                // Near skyline / forest wall
                drawMeshAt(sideMesh, -7.2f, 0f, seg.zPos - 4f)
                drawMeshAt(sideMesh, 7.2f, 0f, seg.zPos - 10f)

                // Deep outer skyline layer for Medium / High profiles
                if (qualityProfile != QualityProfile.LOW) {
                    val outerMesh = when (env) {
                        GameEnvironment.CYBER_CITY -> models.cyberBuildingMesh
                        GameEnvironment.METRO_CITY -> models.cityBuildingMesh
                        GameEnvironment.JUNGLE -> models.jungleTreeMesh
                        GameEnvironment.ANCIENT_TEMPLE -> models.pillarMesh
                    }
                    drawMeshAt(outerMesh, -13.5f, -1.0f, seg.zPos - 9f, scaleX = 1.3f, scaleY = 1.35f, scaleZ = 1.3f)
                    drawMeshAt(outerMesh, 13.5f, -1.0f, seg.zPos - 2f, scaleX = 1.3f, scaleY = 1.25f, scaleZ = 1.3f)
                }
            }

            // Obstacles
            for (obs in seg.obstacles) {
                if (!obs.active) continue
                val ox = RunnerWorldGenerator.LANE_X[obs.lane]
                val oz = seg.zPos + obs.localZ + obs.rollOffsetZ
                val obsMesh = when (obs.kind) {
                    ObstacleKind.TRAIN -> resolveMesh("PF_MetroTrain", models.metroTrainMesh)
                    ObstacleKind.LOW_BARRIER -> models.lowBarrierMesh
                    ObstacleKind.HIGH_GATE -> models.highGateMesh
                    ObstacleKind.ROLLING_BOULDER -> resolveMesh("PF_RollingStone", models.rollingBoulderMesh)
                    ObstacleKind.FIRE_TRAP -> models.fireTrapMesh
                    ObstacleKind.PILLAR -> resolveMesh("PF_TemplePillar", models.pillarMesh)
                }
                val trainTint = if (env == GameEnvironment.CYBER_CITY && obs.kind == ObstacleKind.TRAIN) {
                    floatArrayOf(0.82f, 0.45f, 1.0f)
                } else {
                    floatArrayOf(1f, 1f, 1f)
                }
                val rotX = if (obs.kind == ObstacleKind.ROLLING_BOULDER) world.runPhase * 35f else 0f
                drawMeshAt(obsMesh, ox, 0f, oz, rotX = rotX, tint = trainTint)
            }

            // Coins
            val coinMesh = resolveMesh("PF_Collectible_Coin", models.coinMesh)
            for (coin in seg.coins) {
                if (!coin.active || coin.collected) continue
                val cx = coin.magnetX ?: RunnerWorldGenerator.LANE_X[coin.lane]
                val cz = coin.magnetZ ?: (seg.zPos + coin.localZ)
                drawMeshAt(coinMesh, cx, coin.yOffset + sin(world.runPhase * 0.4f + cz) * 0.08f, cz, rotY = world.runPhase * 42f)
            }

            // PowerUp
            val pu = seg.powerUp
            if (pu.active && !pu.collected) {
                val px = RunnerWorldGenerator.LANE_X[pu.lane]
                val pz = seg.zPos + pu.localZ
                val tint = when (pu.kind) {
                    PowerUpKind.SHIELD -> floatArrayOf(0.1f, 0.9f, 1.0f)
                    PowerUpKind.HOVERBOARD -> floatArrayOf(1.0f, 0.25f, 0.65f)
                    PowerUpKind.MAGNET -> floatArrayOf(1.0f, 0.80f, 0.10f)
                }
                val puMesh = if (pu.kind == PowerUpKind.HOVERBOARD) {
                    resolveMesh("PF_PowerUp_Hoverboard", models.hoverboardMesh)
                } else {
                    models.powerUpOrbMesh
                }
                drawMeshAt(puMesh, px, 0.88f + sin(world.runPhase * 0.5f) * 0.14f, pz, rotY = world.runPhase * 45f, tint = tint)
            }
        }

        drawPlayer()
    }

    private fun drawPlayer() {
        val ch = world.character
        val customCharMesh = resolveCustomOnly(ch.prefabName)
        val px = world.playerX
        val py = world.playerY + if (world.hoverboardTimer > 0f) 0.22f else 0f

        // Ground shadow disc scaling with jump height
        val shadowScale = (1f - (world.playerY * 0.22f)).coerceIn(0.35f, 1f)
        drawMeshAt(models.shadowDiscMesh, px, 0f, 0f, scaleX = shadowScale, scaleZ = shadowScale)

        if (world.hoverboardTimer > 0f) {
            drawMeshAt(
                resolveMesh("PF_PowerUp_Hoverboard", models.hoverboardMesh),
                px, py - 0.12f, 0f, rotY = 0f
            )
        }

        if (customCharMesh != null) {
            val scale = if (world.slideTimer > 0f) 0.65f else 1.0f
            drawMeshAt(customCharMesh, px, py, 0f, scaleY = scale, rotY = 180f)
            return
        }

        val isSliding = world.slideTimer > 0f
        val swing = if (world.isPlaying && !isSliding) sin(world.runPhase * 1.4f) * 38f else 0f
        val torsoTilt = if (isSliding) 65f else 8f
        val baseY = if (isSliding) py + 0.42f else py + 0.92f

        drawMeshAt(models.torsoMesh, px, baseY, 0f, rotX = torsoTilt, tint = ch.primaryRgb)
        drawMeshAt(models.headMesh, px, baseY + (if (isSliding) 0.22f else 0.58f), if (isSliding) -0.35f else -0.05f, rotX = torsoTilt * 0.3f)
        drawMeshAt(models.limbMesh, px - 0.40f, baseY + 0.28f, 0f, rotX = swing, tint = ch.secondaryRgb)
        drawMeshAt(models.limbMesh, px + 0.40f, baseY + 0.28f, 0f, rotX = -swing, tint = ch.secondaryRgb)
        if (!isSliding) {
            drawMeshAt(models.limbMesh, px - 0.17f, py + 0.56f, 0f, rotX = -swing, tint = ch.secondaryRgb)
            drawMeshAt(models.limbMesh, px + 0.17f, py + 0.56f, 0f, rotX = swing, tint = ch.secondaryRgb)
        }

        if (world.hasShield) {
            drawMeshAt(
                models.powerUpOrbMesh,
                px, py + 0.95f, 0f,
                scaleX = 1.6f, scaleY = 2.0f, scaleZ = 1.6f,
                tint = floatArrayOf(0.0f, 0.92f, 1.0f)
            )
        }
    }

    private fun resolveCustomOnly(slot: String): GpuMesh? {
        val cpu = customCpuMeshes[slot] ?: return null
        return customGpuMeshes.getOrPut(slot) { GpuMesh.fromParsedGltf(cpu) }
    }

    private fun resolveMesh(slot: String, fallback: GpuMesh): GpuMesh {
        return resolveCustomOnly(slot) ?: fallback
    }

    private fun drawMeshAt(
        mesh: GpuMesh,
        x: Float, y: Float, z: Float,
        rotX: Float = 0f, rotY: Float = 0f,
        scaleX: Float = 1f, scaleY: Float = 1f, scaleZ: Float = 1f,
        tint: FloatArray = floatArrayOf(1f, 1f, 1f)
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        if (rotX != 0f) Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        if (scaleX != 1f || scaleY != 1f || scaleZ != 1f) {
            Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, scaleZ)
        }
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        mesh.draw(shader, mvpMatrix, modelMatrix, tint[0], tint[1], tint[2])
    }
}

@Composable
fun RunnerGLSurface(
    world: RunnerWorldGenerator,
    isActiveRunning: Boolean,
    qualityProfile: QualityProfile,
    customBindings: Map<String, ParsedGltfMesh>,
    onHudUpdate: (RunnerHudState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val renderer = remember(world) { RunnerGLRenderer(world, onHudUpdate) }

    LaunchedEffect(qualityProfile) {
        renderer.qualityProfile = qualityProfile
    }
    LaunchedEffect(customBindings) {
        renderer.setCustomMeshBindings(customBindings)
    }

    val glView = remember(context, renderer) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    LaunchedEffect(isActiveRunning) {
        glView.renderMode = if (isActiveRunning) {
            GLSurfaceView.RENDERMODE_CONTINUOUSLY
        } else {
            GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
        glView.requestRender()
    }

    DisposableEffect(glView) {
        glView.onResume()
        onDispose { glView.onPause() }
    }

    var dragAccumX = 0f
    var dragAccumY = 0f
    var gestureHandled = false

    AndroidView(
        factory = { glView },
        modifier = modifier.pointerInput(world) {
            detectDragGestures(
                onDragStart = {
                    dragAccumX = 0f
                    dragAccumY = 0f
                    gestureHandled = false
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (gestureHandled) return@detectDragGestures
                    dragAccumX += dragAmount.x
                    dragAccumY += dragAmount.y
                    val threshold = 38f
                    if (abs(dragAccumX) > threshold || abs(dragAccumY) > threshold) {
                        gestureHandled = true
                        if (abs(dragAccumX) > abs(dragAccumY)) {
                            if (dragAccumX > 0) world.moveRight() else world.moveLeft()
                        } else {
                            if (dragAccumY < 0) world.jump() else world.slide()
                        }
                    }
                }
            )
        }
    )
}
