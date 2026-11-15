package com.example.game

import kotlin.math.abs
import kotlin.random.Random

class PooledObstacle {
    var active: Boolean = false
    var lane: Int = 1
    var localZ: Float = 0f
    var kind: ObstacleKind = ObstacleKind.LOW_BARRIER
    var rollOffsetZ: Float = 0f
}

class PooledCoin {
    var active: Boolean = false
    var lane: Int = 1
    var localZ: Float = 0f
    var yOffset: Float = 0.75f
    var collected: Boolean = false
    var magnetX: Float? = null
    var magnetZ: Float? = null
}

class PooledPowerUp {
    var active: Boolean = false
    var lane: Int = 1
    var localZ: Float = 0f
    var kind: PowerUpKind = PowerUpKind.SHIELD
    var collected: Boolean = false
}

class WorldSegment(val poolIndex: Int) {
    var zPos: Float = 0f
    var segmentId: String = "MetroSegment_01"
    var variant: Int = 0
    val obstacles = Array(4) { PooledObstacle() }
    val coins = Array(8) { PooledCoin() }
    val powerUp = PooledPowerUp()
}

class RunnerWorldGenerator {
    companion object {
        const val SEGMENT_LENGTH = 16f
        const val POOL_SIZE = 7
        val LANE_X = floatArrayOf(-2.2f, 0f, 2.2f)
    }

    val segments = Array(POOL_SIZE) { WorldSegment(it) }
    private val rng = Random(System.currentTimeMillis())

    var environment: GameEnvironment = GameEnvironment.METRO_CITY
    var character: PlayableCharacter = PlayableCharacter.ALEX

    var isPlaying: Boolean = false
    var isPaused: Boolean = false
    var isGameOver: Boolean = false

    var targetLane: Int = 1
    var playerX: Float = 0f
    var playerY: Float = 0f
    var verticalVelocity: Float = 0f
    var slideTimer: Float = 0f
    var runPhase: Float = 0f

    var speed: Float = 16f
    var distanceMetersFloat: Float = 0f
    var coinsCollected: Int = 0
    var hasShield: Boolean = false
    var hoverboardTimer: Float = 0f
    var magnetTimer: Float = 0f

    var onGameOverCallback: ((Int, Int) -> Unit)? = null

    init {
        resetWorld(startImmediately = false)
    }

    fun resetWorld(startImmediately: Boolean) {
        isPlaying = startImmediately
        isPaused = false
        isGameOver = false
        targetLane = 1
        playerX = 0f
        playerY = 0f
        verticalVelocity = 0f
        slideTimer = 0f
        runPhase = 0f
        speed = 16.5f
        distanceMetersFloat = 0f
        coinsCollected = 0
        hasShield = false
        hoverboardTimer = 0f
        magnetTimer = 0f

        for (i in 0 until POOL_SIZE) {
            val seg = segments[i]
            seg.zPos = -i * SEGMENT_LENGTH
            populateSegment(seg, index = i, safeZone = i <= 1)
        }
    }

    fun moveLeft() {
        if (isPlaying && !isPaused && !isGameOver) {
            targetLane = (targetLane - 1).coerceIn(0, 2)
        }
    }

    fun moveRight() {
        if (isPlaying && !isPaused && !isGameOver) {
            targetLane = (targetLane + 1).coerceIn(0, 2)
        }
    }

    fun jump() {
        if (!isPlaying || isPaused || isGameOver) return
        if (playerY <= 0.05f) {
            verticalVelocity = 12.8f
            slideTimer = 0f
        }
    }

    fun slide() {
        if (!isPlaying || isPaused || isGameOver) return
        if (playerY > 0.25f) {
            verticalVelocity = -18f // fast slam down
        }
        slideTimer = 0.68f
    }

    fun update(dt: Float): RunnerHudState {
        val clampedDt = dt.coerceIn(0f, 0.05f)
        if (!isPlaying || isPaused || isGameOver) {
            runPhase += clampedDt * 2.5f
            return currentHudState()
        }

        // Smooth lane interpolation
        val desiredX = LANE_X[targetLane]
        playerX += (desiredX - playerX) * (clampedDt * 14f).coerceAtMost(1f)

        // Vertical jump / gravity
        if (playerY > 0f || verticalVelocity > 0f) {
            playerY += verticalVelocity * clampedDt
            verticalVelocity -= 34f * clampedDt
            if (playerY <= 0f) {
                playerY = 0f
                verticalVelocity = 0f
            }
        }

        if (slideTimer > 0f) {
            slideTimer = (slideTimer - clampedDt).coerceAtLeast(0f)
        }
        if (hoverboardTimer > 0f) {
            hoverboardTimer = (hoverboardTimer - clampedDt).coerceAtLeast(0f)
        }
        if (magnetTimer > 0f) {
            magnetTimer = (magnetTimer - clampedDt).coerceAtLeast(0f)
        }

        val effectiveSpeed = if (hoverboardTimer > 0f) speed * 1.25f else speed
        speed = (16.5f + distanceMetersFloat * 0.012f).coerceAtMost(33f)
        distanceMetersFloat += effectiveSpeed * clampedDt
        runPhase += effectiveSpeed * clampedDt * 0.72f

        // Advance pooled segments and recycle those behind camera
        var minZ = Float.MAX_VALUE
        for (seg in segments) {
            seg.zPos += effectiveSpeed * clampedDt
            if (seg.zPos < minZ) minZ = seg.zPos
        }

        for (seg in segments) {
            if (seg.zPos > SEGMENT_LENGTH) {
                seg.zPos = minZ - SEGMENT_LENGTH
                minZ = seg.zPos
                populateSegment(seg, index = (distanceMetersFloat / SEGMENT_LENGTH).toInt(), safeZone = false)
            }
        }

        // Update coins, powerups, and check collisions
        val playerBottom = playerY + if (hoverboardTimer > 0f) 0.25f else 0f
        val playerTop = playerBottom + if (slideTimer > 0f) 0.75f else 1.65f

        for (seg in segments) {
            for (obs in seg.obstacles) {
                if (!obs.active) continue
                if (obs.kind == ObstacleKind.ROLLING_BOULDER) {
                    obs.rollOffsetZ += clampedDt * 2.6f
                }
                val obsZ = seg.zPos + obs.localZ + obs.rollOffsetZ
                val obsHalfZ = if (obs.kind == ObstacleKind.TRAIN) 3.75f else 0.55f
                if (abs(obsZ) < obsHalfZ + 0.35f && abs(playerX - LANE_X[obs.lane]) < 0.78f) {
                    val hit = when (obs.kind) {
                        ObstacleKind.LOW_BARRIER, ObstacleKind.FIRE_TRAP -> playerBottom < 0.85f
                        ObstacleKind.HIGH_GATE -> playerTop > 1.25f
                        ObstacleKind.ROLLING_BOULDER -> playerBottom < 1.45f
                        ObstacleKind.TRAIN, ObstacleKind.PILLAR -> playerBottom < 2.4f
                    }
                    if (hit) {
                        if (hoverboardTimer > 0f) {
                            obs.active = false // smash through on hoverboard!
                        } else if (hasShield) {
                            hasShield = false
                            obs.active = false
                        } else {
                            isGameOver = true
                            isPlaying = false
                            onGameOverCallback?.invoke(distanceMetersFloat.toInt(), coinsCollected)
                            return currentHudState()
                        }
                    }
                }
            }

            for (coin in seg.coins) {
                if (!coin.active || coin.collected) continue
                val cz = coin.magnetZ ?: (seg.zPos + coin.localZ)
                val cx = coin.magnetX ?: LANE_X[coin.lane]

                if (magnetTimer > 0f && abs(cz) < 12f) {
                    val nx = cx + (playerX - cx) * (clampedDt * 12f).coerceAtMost(1f)
                    val nz = cz + (0f - cz) * (clampedDt * 12f).coerceAtMost(1f)
                    coin.magnetX = nx
                    coin.magnetZ = nz
                }

                val testX = coin.magnetX ?: cx
                val testZ = coin.magnetZ ?: cz
                if (abs(testZ) < 0.9f && abs(playerX - testX) < 0.95f && abs((playerY + 0.8f) - coin.yOffset) < 1.3f) {
                    coin.collected = true
                    coinsCollected++
                }
            }

            val pu = seg.powerUp
            if (pu.active && !pu.collected) {
                val pz = seg.zPos + pu.localZ
                if (abs(pz) < 0.95f && abs(playerX - LANE_X[pu.lane]) < 0.95f) {
                    pu.collected = true
                    when (pu.kind) {
                        PowerUpKind.SHIELD -> hasShield = true
                        PowerUpKind.HOVERBOARD -> hoverboardTimer = 8.0f
                        PowerUpKind.MAGNET -> magnetTimer = 7.5f
                    }
                }
            }
        }

        return currentHudState()
    }

    private fun populateSegment(seg: WorldSegment, index: Int, safeZone: Boolean) {
        val segList = environment.segmentIds
        seg.variant = index % segList.size
        seg.segmentId = segList[seg.variant]

        seg.obstacles.forEach {
            it.active = false
            it.rollOffsetZ = 0f
        }
        seg.coins.forEach {
            it.active = false
            it.collected = false
            it.magnetX = null
            it.magnetZ = null
        }
        seg.powerUp.active = false
        seg.powerUp.collected = false

        if (safeZone) {
            // Spawn welcoming coin line in center lane
            for (c in 0 until 5) {
                seg.coins[c].apply {
                    active = true
                    lane = 1
                    localZ = -2f - c * 2.2f
                    yOffset = 0.75f
                }
            }
            return
        }

        // Choose one guaranteed clear lane
        val clearLane = rng.nextInt(3)
        val difficultyBoost = (distanceMetersFloat / 400f).coerceIn(0f, 1f)
        var obsIdx = 0

        for (lane in 0..2) {
            if (lane == clearLane) continue
            if (obsIdx < seg.obstacles.size && rng.nextFloat() < (0.58f + difficultyBoost * 0.25f)) {
                val kind = pickObstacleKind(environment)
                seg.obstacles[obsIdx++].apply {
                    active = true
                    this.lane = lane
                    localZ = if (kind == ObstacleKind.TRAIN) -8f else -4f - rng.nextFloat() * 7f
                    this.kind = kind
                    rollOffsetZ = 0f
                }
            }
        }

        // Occasionally add a jumpable/slideable obstacle even in the clear lane
        if (obsIdx < seg.obstacles.size && rng.nextFloat() < 0.35f) {
            seg.obstacles[obsIdx].apply {
                active = true
                lane = clearLane
                localZ = -11f
                kind = if (rng.nextBoolean()) ObstacleKind.LOW_BARRIER else ObstacleKind.HIGH_GATE
                rollOffsetZ = 0f
            }
        }

        // Coin arc along clear lane
        val coinLane = clearLane
        for (c in 0 until 5) {
            seg.coins[c].apply {
                active = true
                lane = coinLane
                localZ = -2f - c * 2.4f
                yOffset = 0.75f
            }
        }

        // Power-up spawn chance
        if (rng.nextFloat() < 0.22f) {
            seg.powerUp.apply {
                active = true
                lane = clearLane
                localZ = -8f
                kind = PowerUpKind.entries[rng.nextInt(PowerUpKind.entries.size)]
            }
        }
    }

    private fun pickObstacleKind(env: GameEnvironment): ObstacleKind {
        return when (env) {
            GameEnvironment.METRO_CITY -> when (rng.nextInt(4)) {
                0 -> ObstacleKind.TRAIN
                1 -> ObstacleKind.LOW_BARRIER
                2 -> ObstacleKind.HIGH_GATE
                else -> ObstacleKind.PILLAR
            }
            GameEnvironment.ANCIENT_TEMPLE -> when (rng.nextInt(4)) {
                0 -> ObstacleKind.ROLLING_BOULDER
                1 -> ObstacleKind.FIRE_TRAP
                2 -> ObstacleKind.HIGH_GATE
                else -> ObstacleKind.PILLAR
            }
            GameEnvironment.JUNGLE -> when (rng.nextInt(4)) {
                0 -> ObstacleKind.ROLLING_BOULDER
                1 -> ObstacleKind.LOW_BARRIER
                2 -> ObstacleKind.HIGH_GATE
                else -> ObstacleKind.PILLAR
            }
            GameEnvironment.CYBER_CITY -> when (rng.nextInt(4)) {
                0 -> ObstacleKind.TRAIN
                1 -> ObstacleKind.HIGH_GATE
                2 -> ObstacleKind.LOW_BARRIER
                else -> ObstacleKind.PILLAR
            }
        }
    }

    private fun currentHudState(): RunnerHudState {
        val currentSeg = segments.minByOrNull { abs(it.zPos) }?.segmentId ?: environment.segmentIds.first()
        val powerUpLabel = when {
            hoverboardTimer > 0f -> "HOVERBOARD ${hoverboardTimer.toInt() + 1}s"
            magnetTimer > 0f -> "MAGNET ${magnetTimer.toInt() + 1}s"
            hasShield -> "SHIELD ACTIVE"
            else -> null
        }
        return RunnerHudState(
            isPlaying = isPlaying,
            isPaused = isPaused,
            isGameOver = isGameOver,
            distanceMeters = distanceMetersFloat.toInt(),
            coinsInRun = coinsCollected,
            speedKmh = (speed * 3.4f).toInt(),
            activeSegmentId = currentSeg,
            pooledSegmentsCount = POOL_SIZE,
            activePowerUp = powerUpLabel,
            hasShield = hasShield,
            hoverboardSeconds = hoverboardTimer,
            magnetSeconds = magnetTimer
        )
    }
}
