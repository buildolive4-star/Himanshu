package com.example.game

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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelInspectorRenderer : GLSurfaceView.Renderer {
    private lateinit var shader: MeshShaderProgram
    private lateinit var fallbackBank: ProceduralModelBank

    @Volatile
    var parsedMesh: ParsedGltfMesh? = null
        set(value) {
            field = value
            needsUpload = true
        }

    @Volatile
    private var needsUpload = false
    private var uploadedGpuMesh: GpuMesh? = null

    @Volatile
    var yawDeg = 35f
    @Volatile
    var pitchDeg = 18f
    @Volatile
    var zoomDist = 4.2f

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        shader = MeshShaderProgram()
        fallbackBank = ProceduralModelBank()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 48f, ratio, 0.1f, 50f)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (needsUpload) {
            needsUpload = false
            uploadedGpuMesh = parsedMesh?.let { GpuMesh.fromParsedGltf(it) }
        }

        GLES20.glClearColor(0.05f, 0.08f, 0.14f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(shader.programId)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 1.4f, zoomDist,
            0f, 0.9f, 0f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUniform3f(shader.uLightDir, 0.6f, 0.9f, 0.7f)
        GLES20.glUniform3f(shader.uFogColor, 0.05f, 0.08f, 0.14f)
        GLES20.glUniform1f(shader.uFogDensity, 0.01f)
        GLES20.glUniform3f(shader.uCameraPos, 0f, 1.4f, zoomDist)

        yawDeg += 0.35f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, pitchDeg.coerceIn(-45f, 65f), 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        val mesh = uploadedGpuMesh ?: fallbackBank.metroTrainMesh
        mesh.draw(shader, mvpMatrix, modelMatrix, 1f, 1f, 1f)
    }
}

@Composable
fun ModelInspectorGLView(
    parsedMesh: ParsedGltfMesh?,
    zoomDist: Float = 4.2f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { ModelInspectorRenderer() }

    LaunchedEffect(parsedMesh) {
        renderer.parsedMesh = parsedMesh
    }
    LaunchedEffect(zoomDist) {
        renderer.zoomDist = zoomDist
    }

    val glView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(glView) {
        glView.onResume()
        onDispose { glView.onPause() }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                renderer.yawDeg += dragAmount.x * 0.45f
                renderer.pitchDeg = (renderer.pitchDeg + dragAmount.y * 0.35f).coerceIn(-45f, 65f)
            }
        }
    )
}
