package com.example.game

import android.opengl.GLES20
import com.example.pipeline.ParsedGltfMesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SkyBackdropProgram {
    private val vsCode = """
        attribute vec2 aPos;
        attribute vec3 aColor;
        varying vec3 vColor;
        void main() {
            vColor = aColor;
            gl_Position = vec4(aPos, 0.999, 1.0);
        }
    """.trimIndent()

    private val fsCode = """
        precision lowp float;
        varying vec3 vColor;
        void main() {
            gl_FragColor = vec4(vColor, 1.0);
        }
    """.trimIndent()

    private val programId: Int
    private val aPos: Int
    private val aColor: Int
    private var currentEnv: GameEnvironment? = null
    private var skyBuffer: FloatBuffer? = null
    private var vertexCount = 0

    init {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vsCode)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fsCode)
        programId = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        aPos = GLES20.glGetAttribLocation(programId, "aPos")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
    }

    fun draw(env: GameEnvironment) {
        if (currentEnv != env || skyBuffer == null) {
            currentEnv = env
            rebuildGeometry(env)
        }
        val buf = skyBuffer ?: return

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(programId)

        buf.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 20, buf)

        buf.position(2)
        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, 20, buf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun rebuildGeometry(env: GameEnvironment) {
        val top: FloatArray
        val mid: FloatArray
        val bot: FloatArray
        val sun: FloatArray
        val sunX: Float
        val sunY: Float

        when (env) {
            GameEnvironment.METRO_CITY -> {
                top = floatArrayOf(0.04f, 0.07f, 0.20f)
                mid = floatArrayOf(0.18f, 0.26f, 0.48f)
                bot = floatArrayOf(0.08f, 0.12f, 0.24f)
                sun = floatArrayOf(1.0f, 0.64f, 0.30f)
                sunX = 0.30f; sunY = 0.28f
            }
            GameEnvironment.ANCIENT_TEMPLE -> {
                top = floatArrayOf(0.16f, 0.22f, 0.38f)
                mid = floatArrayOf(0.84f, 0.48f, 0.20f)
                bot = floatArrayOf(0.45f, 0.18f, 0.05f)
                sun = floatArrayOf(1.0f, 0.88f, 0.42f)
                sunX = 0.0f; sunY = 0.34f
            }
            GameEnvironment.JUNGLE -> {
                top = floatArrayOf(0.08f, 0.36f, 0.50f)
                mid = floatArrayOf(0.36f, 0.72f, 0.56f)
                bot = floatArrayOf(0.08f, 0.24f, 0.18f)
                sun = floatArrayOf(1.0f, 0.95f, 0.62f)
                sunX = -0.25f; sunY = 0.42f
            }
            GameEnvironment.CYBER_CITY -> {
                top = floatArrayOf(0.03f, 0.02f, 0.12f)
                mid = floatArrayOf(0.34f, 0.08f, 0.42f)
                bot = floatArrayOf(0.05f, 0.16f, 0.30f)
                sun = floatArrayOf(0.0f, 0.94f, 1.0f)
                sunX = 0.0f; sunY = 0.30f
            }
        }

        val verts = ArrayList<Float>(128)
        // Upper sky quad (-1..1, 0..1)
        addQuad(verts, -1f, 0f, 1f, 1f, mid, top)
        // Lower sky quad (-1..1, -1..0)
        addQuad(verts, -1f, -1f, 1f, 0f, bot, mid)

        // Crisp glowing sun/moon polygon (12 segments)
        val segs = 12
        val rx = 0.12f
        val ry = 0.09f
        for (i in 0 until segs) {
            val a0 = (2.0 * PI * i / segs).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segs).toFloat()
            addV(verts, sunX, sunY, sun)
            addV(verts, sunX + cos(a0) * rx, sunY + sin(a0) * ry, sun)
            addV(verts, sunX + cos(a1) * rx, sunY + sin(a1) * ry, sun)
        }

        vertexCount = verts.size / 5
        val arr = verts.toFloatArray()
        skyBuffer = ByteBuffer.allocateDirect(arr.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(arr); position(0) }
    }

    private fun addQuad(
        out: ArrayList<Float>,
        x0: Float, y0: Float, x1: Float, y1: Float,
        cBot: FloatArray, cTop: FloatArray
    ) {
        addV(out, x0, y0, cBot); addV(out, x1, y0, cBot); addV(out, x1, y1, cTop)
        addV(out, x0, y0, cBot); addV(out, x1, y1, cTop); addV(out, x0, y1, cTop)
    }

    private fun addV(out: ArrayList<Float>, x: Float, y: Float, c: FloatArray) {
        out.add(x); out.add(y); out.add(c[0]); out.add(c[1]); out.add(c[2])
    }

    private fun compile(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}

class MeshShaderProgram {
    // Vertex-lit + vertex-fogged shader: ultra-fast 60fps even on software/emulator GPUs
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec3 uLightDir;
        uniform vec3 uFogColor;
        uniform float uFogDensity;
        uniform vec3 uCameraPos;
        uniform vec3 uTintColor;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec3 aColor;
        varying vec3 vFinalColor;
        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vec3 norm = normalize(mat3(uModelMatrix) * aNormal);
            float diff = max(dot(norm, uLightDir), 0.0);
            float hemi = norm.y * 0.16 + 0.22;
            vec3 lit = aColor * uTintColor * (0.44 + 0.62 * diff + hemi);
            float dist = length(uCameraPos - worldPos.xyz);
            float fog = clamp(dist * uFogDensity, 0.0, 0.92);
            vFinalColor = mix(lit, uFogColor, fog * fog);
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision lowp float;
        varying vec3 vFinalColor;
        void main() {
            gl_FragColor = vec4(vFinalColor, 1.0);
        }
    """.trimIndent()

    val programId: Int
    val uMVPMatrix: Int
    val uModelMatrix: Int
    val uLightDir: Int
    val uFogColor: Int
    val uFogDensity: Int
    val uCameraPos: Int
    val uTintColor: Int
    val aPosition: Int
    val aNormal: Int
    val aColor: Int

    init {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
        }
        uMVPMatrix = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uLightDir = GLES20.glGetUniformLocation(programId, "uLightDir")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
        uCameraPos = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uTintColor = GLES20.glGetUniformLocation(programId, "uTintColor")
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")
        aColor = GLES20.glGetAttribLocation(programId, "aColor")
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}

class GpuMesh(private val interleavedData: FloatArray) {
    private val strideBytes = 9 * 4
    val vertexCount: Int = interleavedData.size / 9
    private var vboId: Int = 0

    private fun ensureVbo() {
        if (vboId != 0 || vertexCount <= 0) return
        val bb = ByteBuffer.allocateDirect(interleavedData.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer().apply {
            put(interleavedData)
            position(0)
        }
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, interleavedData.size * 4, fb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw(
        shader: MeshShaderProgram,
        mvpMatrix: FloatArray,
        modelMatrix: FloatArray,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f
    ) {
        if (vertexCount <= 0) return
        ensureVbo()

        GLES20.glUniformMatrix4fv(shader.uMVPMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uTintColor, tintR, tintG, tintB)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(shader.aPosition)
        GLES20.glVertexAttribPointer(shader.aPosition, 3, GLES20.GL_FLOAT, false, strideBytes, 0)

        GLES20.glEnableVertexAttribArray(shader.aNormal)
        GLES20.glVertexAttribPointer(shader.aNormal, 3, GLES20.GL_FLOAT, false, strideBytes, 12)

        GLES20.glEnableVertexAttribArray(shader.aColor)
        GLES20.glVertexAttribPointer(shader.aColor, 3, GLES20.GL_FLOAT, false, strideBytes, 24)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    companion object {
        fun fromParsedGltf(parsed: ParsedGltfMesh): GpuMesh {
            val raw = parsed.interleavedTriangles
            if (raw.isEmpty()) return GpuMesh(FloatArray(0))
            val scale = parsed.normalizedScale
            val center = parsed.centerOffset
            val minY = parsed.boundsMin[1]
            val normalized = FloatArray(raw.size)
            var i = 0
            while (i + 8 < raw.size) {
                normalized[i] = (raw[i] - center[0]) * scale
                normalized[i + 1] = (raw[i + 1] - minY) * scale
                normalized[i + 2] = (raw[i + 2] - center[2]) * scale
                normalized[i + 3] = raw[i + 3]
                normalized[i + 4] = raw[i + 4]
                normalized[i + 5] = raw[i + 5]
                normalized[i + 6] = raw[i + 6]
                normalized[i + 7] = raw[i + 7]
                normalized[i + 8] = raw[i + 8]
                i += 9
            }
            return GpuMesh(normalized)
        }
    }
}
