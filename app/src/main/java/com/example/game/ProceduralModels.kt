package com.example.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class MeshBuilder {
    private val data = ArrayList<Float>(1024)

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ): MeshBuilder {
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r, g, b)
        quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r, g, b)
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.9f, g * 0.9f, b * 0.9f)
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.9f, g * 0.9f, b * 0.9f)
        quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, (r * 1.08f).coerceAtMost(1f), (g * 1.08f).coerceAtMost(1f), (b * 1.08f).coerceAtMost(1f))
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.7f, g * 0.7f, b * 0.7f)
        return this
    }

    fun addCylinderY(
        cx: Float, cy: Float, cz: Float,
        radiusTop: Float, radiusBottom: Float, height: Float,
        segments: Int, r: Float, g: Float, b: Float
    ): MeshBuilder {
        val y0 = cy - height * 0.5f
        val y1 = cy + height * 0.5f
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val nx = (c0 + c1) * 0.5f
            val nz = (s0 + s1) * 0.5f

            quad(
                c0 * radiusBottom + cx, y0, s0 * radiusBottom + cz,
                c1 * radiusBottom + cx, y0, s1 * radiusBottom + cz,
                c1 * radiusTop + cx, y1, s1 * radiusTop + cz,
                c0 * radiusTop + cx, y1, s0 * radiusTop + cz,
                nx, 0.15f, nz, r, g, b
            )
            tri(cx, y1, cz, c0 * radiusTop + cx, y1, s0 * radiusTop + cz, c1 * radiusTop + cx, y1, s1 * radiusTop + cz, 0f, 1f, 0f, r, g, b)
        }
        return this
    }

    fun addSphere(
        cx: Float, cy: Float, cz: Float,
        radius: Float, rings: Int, sectors: Int,
        r: Float, g: Float, b: Float
    ): MeshBuilder {
        for (i in 0 until rings) {
            val lat0 = (PI * (-0.5 + i.toDouble() / rings)).toFloat()
            val lat1 = (PI * (-0.5 + (i + 1).toDouble() / rings)).toFloat()
            val y0 = sin(lat0); val yr0 = cos(lat0)
            val y1 = sin(lat1); val yr1 = cos(lat1)

            for (j in 0 until sectors) {
                val lng0 = (2 * PI * j / sectors).toFloat()
                val lng1 = (2 * PI * (j + 1) / sectors).toFloat()
                val x00 = cos(lng0) * yr0; val z00 = sin(lng0) * yr0
                val x10 = cos(lng1) * yr0; val z10 = sin(lng1) * yr0
                val x11 = cos(lng1) * yr1; val z11 = sin(lng1) * yr1
                val x01 = cos(lng0) * yr1; val z01 = sin(lng0) * yr1

                quad(
                    cx + x00 * radius, cy + y0 * radius, cz + z00 * radius,
                    cx + x10 * radius, cy + y0 * radius, cz + z10 * radius,
                    cx + x11 * radius, cy + y1 * radius, cz + z11 * radius,
                    cx + x01 * radius, cy + y1 * radius, cz + z01 * radius,
                    (x00 + x11) * 0.5f, (y0 + y1) * 0.5f, (z00 + z11) * 0.5f,
                    r, g, b
                )
            }
        }
        return this
    }

    private fun quad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        tri(x0, y0, z0, x1, y1, z1, x2, y2, z2, nx, ny, nz, r, g, b)
        tri(x0, y0, z0, x2, y2, z2, x3, y3, z3, nx, ny, nz, r, g, b)
    }

    private fun tri(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float
    ) {
        v(x0, y0, z0, nx, ny, nz, r, g, b)
        v(x1, y1, z1, nx, ny, nz, r, g, b)
        v(x2, y2, z2, nx, ny, nz, r, g, b)
    }

    private fun v(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
    }

    fun build(): GpuMesh = GpuMesh(data.toFloatArray())
}

class ProceduralModelBank {
    val trackTileMetro: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, -0.20f, 0f, 8.8f, 0.4f, 16f, 0.14f, 0.17f, 0.24f)
            // Gravel ballast strips
            .addBox(-2.2f, 0.01f, 0f, 1.7f, 0.04f, 16f, 0.22f, 0.24f, 0.29f)
            .addBox(0f, 0.01f, 0f, 1.7f, 0.04f, 16f, 0.22f, 0.24f, 0.29f)
            .addBox(2.2f, 0.01f, 0f, 1.7f, 0.04f, 16f, 0.22f, 0.24f, 0.29f)
            // Railway sleepers across all 3 tracks
            .apply {
                for (i in -4..3) {
                    val tz = i * 2.0f + 1.0f
                    addBox(-2.2f, 0.03f, tz, 1.55f, 0.05f, 0.34f, 0.36f, 0.24f, 0.16f)
                    addBox(0f, 0.03f, tz, 1.55f, 0.05f, 0.34f, 0.36f, 0.24f, 0.16f)
                    addBox(2.2f, 0.03f, tz, 1.55f, 0.05f, 0.34f, 0.36f, 0.24f, 0.16f)
                }
            }
            // Steel rails
            .addBox(-2.85f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            .addBox(-1.55f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            .addBox(-0.65f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            .addBox(0.65f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            .addBox(1.55f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            .addBox(2.85f, 0.06f, 0f, 0.10f, 0.09f, 16f, 0.72f, 0.80f, 0.92f)
            // Glowing platform curbs & walkways
            .addBox(-4.15f, 0.22f, 0f, 0.55f, 0.48f, 16f, 0.20f, 0.25f, 0.34f)
            .addBox(4.15f, 0.22f, 0f, 0.55f, 0.48f, 16f, 0.20f, 0.25f, 0.34f)
            .addBox(-3.86f, 0.46f, 0f, 0.10f, 0.06f, 16f, 0.0f, 0.90f, 1.0f)
            .addBox(3.86f, 0.46f, 0f, 0.10f, 0.06f, 16f, 0.0f, 0.90f, 1.0f)
            .build()
    }

    val trackTileTemple: GpuMesh by lazy {
        MeshBuilder()
            // Molten abyss glow beneath bridge
            .addBox(0f, -2.5f, 0f, 26f, 0.2f, 16f, 0.96f, 0.38f, 0.06f)
            // Stone bridge deck
            .addBox(0f, -0.22f, 0f, 8.4f, 0.45f, 16f, 0.46f, 0.37f, 0.25f)
            // Carved lane slabs
            .apply {
                for (i in -3..3) {
                    val tz = i * 2.2f
                    addBox(-2.2f, 0.02f, tz, 1.75f, 0.06f, 1.95f, 0.58f, 0.48f, 0.34f)
                    addBox(0f, 0.02f, tz, 1.75f, 0.06f, 1.95f, 0.62f, 0.51f, 0.36f)
                    addBox(2.2f, 0.02f, tz, 1.75f, 0.06f, 1.95f, 0.58f, 0.48f, 0.34f)
                }
            }
            // Mossy stone side parapets
            .addBox(-4.0f, 0.42f, 0f, 0.55f, 0.85f, 16f, 0.34f, 0.48f, 0.22f)
            .addBox(4.0f, 0.42f, 0f, 0.55f, 0.85f, 16f, 0.34f, 0.48f, 0.22f)
            .build()
    }

    val trackTileJungle: GpuMesh by lazy {
        MeshBuilder()
            // Turquoise river water flanking the wooden bridge
            .addBox(-9.5f, -0.75f, 0f, 11f, 0.12f, 16f, 0.06f, 0.62f, 0.72f)
            .addBox(9.5f, -0.75f, 0f, 11f, 0.12f, 16f, 0.06f, 0.62f, 0.72f)
            // Bridge stringer beams
            .addBox(-3.1f, -0.15f, 0f, 0.35f, 0.30f, 16f, 0.32f, 0.21f, 0.12f)
            .addBox(0f, -0.15f, 0f, 0.35f, 0.30f, 16f, 0.32f, 0.21f, 0.12f)
            .addBox(3.1f, -0.15f, 0f, 0.35f, 0.30f, 16f, 0.32f, 0.21f, 0.12f)
            // Wooden bridge planks
            .apply {
                for (i in -5..5) {
                    addBox(0f, 0.02f, i * 1.4f, 7.6f, 0.08f, 1.1f, 0.52f, 0.35f, 0.19f)
                }
            }
            // Rope bridge side rails
            .addBox(-3.85f, 0.65f, 0f, 0.14f, 0.12f, 16f, 0.72f, 0.56f, 0.32f)
            .addBox(3.85f, 0.65f, 0f, 0.14f, 0.12f, 16f, 0.72f, 0.56f, 0.32f)
            .build()
    }

    val trackTileCyber: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, -0.18f, 0f, 8.6f, 0.36f, 16f, 0.07f, 0.08f, 0.16f)
            // Glowing neon maglev guide rails
            .addBox(-2.2f, 0.02f, 0f, 0.16f, 0.06f, 16f, 0.0f, 0.94f, 1.0f)
            .addBox(0f, 0.02f, 0f, 0.16f, 0.06f, 16f, 1.0f, 0.22f, 0.64f)
            .addBox(2.2f, 0.02f, 0f, 0.16f, 0.06f, 16f, 0.0f, 0.94f, 1.0f)
            .apply {
                for (i in -3..3) {
                    addBox(0f, 0.01f, i * 2.2f, 7.6f, 0.03f, 0.12f, 0.18f, 0.35f, 0.75f)
                }
            }
            .addBox(-4.1f, 0.24f, 0f, 0.36f, 0.52f, 16f, 0.98f, 0.18f, 0.60f)
            .addBox(4.1f, 0.24f, 0f, 0.36f, 0.52f, 16f, 0.0f, 0.92f, 1.0f)
            .build()
    }

    // Overhead environment arches spanning the 3 running lanes
    val metroArchMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(-4.35f, 3.1f, 0f, 0.45f, 6.2f, 0.55f, 0.24f, 0.30f, 0.42f)
            .addBox(4.35f, 3.1f, 0f, 0.45f, 6.2f, 0.55f, 0.24f, 0.30f, 0.42f)
            .addBox(0f, 6.0f, 0f, 9.2f, 0.55f, 0.60f, 0.26f, 0.32f, 0.46f)
            .addBox(0f, 5.75f, 0.32f, 7.2f, 0.18f, 0.08f, 0.0f, 0.92f, 1.0f)
            .addBox(-4.1f, 4.6f, 0f, 0.14f, 1.2f, 0.14f, 1.0f, 0.72f, 0.15f)
            .addBox(4.1f, 4.6f, 0f, 0.14f, 1.2f, 0.14f, 1.0f, 0.72f, 0.15f)
            .build()
    }

    val templeArchMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(-4.3f, 2.9f, 0f, 0.95f, 5.8f, 1.1f, 0.58f, 0.48f, 0.35f)
            .addBox(4.3f, 2.9f, 0f, 0.95f, 5.8f, 1.1f, 0.58f, 0.48f, 0.35f)
            .addBox(0f, 5.9f, 0f, 9.8f, 1.0f, 1.3f, 0.52f, 0.42f, 0.30f)
            // Glowing fire braziers atop side pillars
            .addSphere(-4.3f, 6.1f, 0f, 0.45f, 5, 7, 1.0f, 0.56f, 0.08f)
            .addSphere(4.3f, 6.1f, 0f, 0.45f, 5, 7, 1.0f, 0.56f, 0.08f)
            .build()
    }

    val cyberRingArchMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(-4.3f, 3.0f, 0f, 0.35f, 6.0f, 0.45f, 0.0f, 0.92f, 1.0f)
            .addBox(4.3f, 3.0f, 0f, 0.35f, 6.0f, 0.45f, 1.0f, 0.22f, 0.64f)
            .addBox(0f, 5.9f, 0f, 9.0f, 0.38f, 0.48f, 0.55f, 0.30f, 1.0f)
            .addBox(0f, 5.9f, 0.25f, 7.6f, 0.14f, 0.08f, 0.0f, 0.96f, 1.0f)
            .build()
    }

    val streetLampMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 2.4f, 0f, 0.09f, 0.13f, 4.8f, 6, 0.28f, 0.34f, 0.42f)
            .addBox(0.5f, 4.75f, 0f, 1.1f, 0.12f, 0.18f, 0.28f, 0.34f, 0.42f)
            .addBox(0.95f, 4.64f, 0f, 0.45f, 0.10f, 0.24f, 1.0f, 0.88f, 0.45f)
            .build()
    }

    val metroTrainMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 1.38f, 0f, 1.78f, 2.38f, 7.8f, 0.78f, 0.84f, 0.92f)
            // Cyan body stripe
            .addBox(0f, 1.35f, 0f, 1.83f, 0.32f, 7.84f, 0.0f, 0.88f, 1.0f)
            // Illuminated cabin windows
            .addBox(-0.90f, 1.85f, 0f, 0.04f, 0.55f, 6.4f, 0.12f, 0.25f, 0.38f)
            .addBox(0.90f, 1.85f, 0f, 0.04f, 0.55f, 6.4f, 0.12f, 0.25f, 0.38f)
            // Windshield & headlights
            .addBox(0f, 1.88f, 3.92f, 1.38f, 0.78f, 0.10f, 0.08f, 0.14f, 0.24f)
            .addBox(-0.56f, 0.84f, 3.92f, 0.34f, 0.22f, 0.10f, 1.0f, 0.94f, 0.55f)
            .addBox(0.56f, 0.84f, 3.92f, 0.34f, 0.22f, 0.10f, 1.0f, 0.94f, 0.55f)
            // Roof unit
            .addBox(0f, 2.65f, 0f, 1.32f, 0.22f, 6.8f, 0.32f, 0.38f, 0.48f)
            .build()
    }

    val lowBarrierMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(-0.7f, 0.48f, 0f, 0.18f, 0.95f, 0.22f, 0.25f, 0.28f, 0.34f)
            .addBox(0.7f, 0.48f, 0f, 0.18f, 0.95f, 0.22f, 0.25f, 0.28f, 0.34f)
            .addBox(0f, 0.78f, 0f, 1.75f, 0.38f, 0.20f, 1.0f, 0.56f, 0.08f)
            .addBox(0f, 0.78f, 0.11f, 0.55f, 0.36f, 0.04f, 0.12f, 0.12f, 0.14f)
            .build()
    }

    val highGateMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(-0.82f, 1.35f, 0f, 0.20f, 2.7f, 0.24f, 0.82f, 0.20f, 0.24f)
            .addBox(0.82f, 1.35f, 0f, 0.20f, 2.7f, 0.24f, 0.82f, 0.20f, 0.24f)
            .addBox(0f, 2.05f, 0f, 1.85f, 1.1f, 0.26f, 0.98f, 0.72f, 0.10f)
            .build()
    }

    val rollingBoulderMesh: GpuMesh by lazy {
        MeshBuilder()
            .addSphere(0f, 0.92f, 0f, 0.92f, 8, 10, 0.54f, 0.48f, 0.39f)
            .build()
    }

    val fireTrapMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 0.22f, 0f, 0.65f, 0.75f, 0.44f, 8, 0.36f, 0.30f, 0.24f)
            .addCylinderY(0f, 0.65f, 0f, 0.15f, 0.48f, 0.65f, 8, 1.0f, 0.42f, 0.06f)
            .build()
    }

    val pillarMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 1.75f, 0f, 0.58f, 0.68f, 3.5f, 8, 0.66f, 0.57f, 0.43f)
            .addBox(0f, 3.65f, 0f, 1.55f, 0.38f, 1.55f, 0.56f, 0.48f, 0.36f)
            .addBox(0f, 0.20f, 0f, 1.50f, 0.40f, 1.50f, 0.52f, 0.44f, 0.32f)
            .build()
    }

    val cityBuildingMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 6.0f, 0f, 3.8f, 12.0f, 4.2f, 0.14f, 0.19f, 0.30f)
            .addBox(0f, 12.4f, 0f, 2.4f, 1.2f, 2.6f, 0.20f, 0.28f, 0.42f)
            // Lit window bands
            .addBox(0f, 4.0f, 2.13f, 3.0f, 0.65f, 0.06f, 0.0f, 0.85f, 1.0f)
            .addBox(0f, 6.2f, 2.13f, 3.0f, 0.65f, 0.06f, 1.0f, 0.74f, 0.20f)
            .addBox(0f, 8.4f, 2.13f, 3.0f, 0.65f, 0.06f, 0.0f, 0.85f, 1.0f)
            .addBox(0f, 10.6f, 2.13f, 3.0f, 0.65f, 0.06f, 1.0f, 0.74f, 0.20f)
            .build()
    }

    val cyberBuildingMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 7.5f, 0f, 4.0f, 15.0f, 4.4f, 0.08f, 0.09f, 0.18f)
            .addBox(0f, 15.5f, 0f, 0.4f, 2.6f, 0.4f, 0.0f, 0.94f, 1.0f)
            // Holographic billboard panels
            .addBox(0f, 6.5f, 2.25f, 3.2f, 4.4f, 0.10f, 1.0f, 0.20f, 0.64f)
            .addBox(0f, 11.8f, 2.25f, 2.6f, 2.4f, 0.10f, 0.0f, 0.92f, 1.0f)
            .build()
    }

    val jungleTreeMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 2.6f, 0f, 0.30f, 0.56f, 5.2f, 7, 0.44f, 0.28f, 0.15f)
            .addSphere(0f, 5.4f, 0f, 2.1f, 6, 8, 0.12f, 0.68f, 0.26f)
            .addSphere(1.1f, 4.7f, 0.5f, 1.45f, 5, 7, 0.16f, 0.78f, 0.32f)
            .addSphere(-1.0f, 4.9f, -0.4f, 1.35f, 5, 7, 0.10f, 0.62f, 0.22f)
            .build()
    }

    val coinMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 0f, 0f, 0.40f, 0.40f, 0.12f, 12, 1.0f, 0.82f, 0.12f)
            .build()
    }

    val powerUpOrbMesh: GpuMesh by lazy {
        MeshBuilder()
            .addSphere(0f, 0f, 0f, 0.46f, 8, 10, 0.0f, 0.94f, 1.0f)
            .build()
    }

    val hoverboardMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 0.12f, 0f, 0.72f, 0.10f, 1.72f, 0.0f, 0.92f, 1.0f)
            .addBox(0f, 0.18f, 0f, 0.42f, 0.06f, 1.20f, 1.0f, 0.26f, 0.68f)
            .build()
    }

    val shadowDiscMesh: GpuMesh by lazy {
        MeshBuilder()
            .addCylinderY(0f, 0.03f, 0f, 0.56f, 0.56f, 0.02f, 10, 0.04f, 0.05f, 0.08f)
            .build()
    }

    val torsoMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 0f, 0f, 0.58f, 0.72f, 0.34f, 1f, 1f, 1f)
            .build()
    }

    val headMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, 0f, 0f, 0.42f, 0.42f, 0.42f, 0.96f, 0.82f, 0.72f)
            .addBox(0f, 0.05f, -0.22f, 0.38f, 0.14f, 0.05f, 0.05f, 0.92f, 1.0f)
            .build()
    }

    val limbMesh: GpuMesh by lazy {
        MeshBuilder()
            .addBox(0f, -0.28f, 0f, 0.20f, 0.58f, 0.20f, 1f, 1f, 1f)
            .build()
    }
}
