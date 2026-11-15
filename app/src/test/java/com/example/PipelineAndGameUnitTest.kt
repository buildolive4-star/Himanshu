package com.example

import com.example.data.sketchfab.SearchCategory
import com.example.data.sketchfab.SketchfabLicenseDto
import com.example.data.sketchfab.SketchfabModelDto
import com.example.data.sketchfab.SketchfabOEmbedClient
import com.example.data.sketchfab.SketchfabOEmbedResponse
import com.example.data.sketchfab.SketchfabUserDto
import com.example.game.GameEnvironment
import com.example.game.RunnerWorldGenerator
import com.example.pipeline.AssetLicenseManager
import com.example.pipeline.GltfMeshParser
import com.example.pipeline.LicensePolicyDecision
import com.example.pipeline.ModelScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PipelineAndGameUnitTest {

    @Test
    fun licenseManager_enforcesCommercialRules() {
        val ccBy = AssetLicenseManager.evaluate(
            SketchfabLicenseDto(slug = "by", label = "CC Attribution"),
            "Metro Train", "artist1", "https://sketchfab.com/1"
        )
        assertTrue(ccBy.decision.isPermitted)
        assertEquals(LicensePolicyDecision.CC_BY_ATTRIBUTION, ccBy.decision)

        val ccByNc = AssetLicenseManager.evaluate(
            SketchfabLicenseDto(slug = "by-nc", label = "CC Attribution-NonCommercial"),
            "Metro Train", "artist1", "https://sketchfab.com/1"
        )
        assertFalse(ccByNc.decision.isPermitted)
        assertEquals(LicensePolicyDecision.REJECTED_NON_COMMERCIAL, ccByNc.decision)

        val ccByNd = AssetLicenseManager.evaluate(
            SketchfabLicenseDto(slug = "by-nd", label = "CC Attribution-NoDerivs"),
            "Metro Train", "artist1", "https://sketchfab.com/1"
        )
        assertFalse(ccByNd.decision.isPermitted)
        assertEquals(LicensePolicyDecision.REJECTED_NO_DERIVATIVES, ccByNd.decision)
    }

    @Test
    fun scoringEngine_computes100PointBreakdown() {
        val model = SketchfabModelDto(
            uid = "abc123",
            name = "Realistic Metro Train",
            viewerUrl = "https://sketchfab.com/3d-models/abc123",
            faceCount = 8500,
            vertexCount = 4400,
            animationCount = 0,
            isDownloadable = true,
            likeCount = 240,
            staffpickedAt = "2026-01-01",
            user = SketchfabUserDto(username = "raildev"),
            license = SketchfabLicenseDto(slug = "cc0", label = "CC0 Public Domain")
        )
        val evaluated = ModelScoringEngine.evaluateCandidate(
            model, SearchCategory.METRO_CITY, "realistic metro train"
        )
        assertTrue(evaluated.score.totalScore in 70..100)
        assertEquals(15, evaluated.score.licenseSuitability)
        assertEquals(null, evaluated.rejectionReason)
    }

    @Test
    fun runnerWorld_updatesLanesJumpAndPoolsSegments() {
        val world = RunnerWorldGenerator()
        world.environment = GameEnvironment.ANCIENT_TEMPLE
        world.resetWorld(startImmediately = true)
        assertEquals(RunnerWorldGenerator.POOL_SIZE, world.segments.size)

        world.moveLeft()
        assertEquals(0, world.targetLane)
        world.moveLeft()
        assertEquals(0, world.targetLane)
        world.moveRight()
        assertEquals(1, world.targetLane)

        world.jump()
        val hudAfterJump = world.update(0.04f)
        assertTrue(world.playerY > 0f)
        assertTrue(hudAfterJump.isPlaying)
    }

    @Test
    fun gltfParser_parsesEmbeddedTriangleMesh() {
        // Build 3 vertices (36 bytes) + 3 unsigned short indices (6 bytes)
        val bin = ByteBuffer.allocate(42).order(ByteOrder.LITTLE_ENDIAN)
        // v0
        bin.putFloat(0f); bin.putFloat(0f); bin.putFloat(0f)
        // v1
        bin.putFloat(1f); bin.putFloat(0f); bin.putFloat(0f)
        // v2
        bin.putFloat(0f); bin.putFloat(1f); bin.putFloat(0f)
        // indices
        bin.putShort(0); bin.putShort(1); bin.putShort(2)
        val b64 = Base64.getEncoder().encodeToString(bin.array())

        val gltfJson = """
            {
              "asset": {"version": "2.0"},
              "buffers": [{"uri": "data:application/octet-stream;base64,$b64", "byteLength": 42}],
              "bufferViews": [
                {"buffer": 0, "byteOffset": 0, "byteLength": 36},
                {"buffer": 0, "byteOffset": 36, "byteLength": 6}
              ],
              "accessors": [
                {"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"},
                {"bufferView": 1, "componentType": 5123, "count": 3, "type": "SCALAR"}
              ],
              "meshes": [
                {"primitives": [{"attributes": {"POSITION": 0}, "indices": 1}]}
              ]
            }
        """.trimIndent()

        val tempFile = File.createTempFile("test_tri", ".gltf")
        tempFile.writeText(gltfJson)
        try {
            val parsed = GltfMeshParser.parseFromFile(tempFile).getOrNull()
            assertNotNull(parsed)
            assertEquals(3, parsed!!.vertexCount)
            assertEquals(1, parsed.triangleCount)
            assertEquals(27, parsed.interleavedTriangles.size)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun oEmbed_extractsIframeUrlAndNormalizesModelUrl() {
        val resp = SketchfabOEmbedResponse(
            title = "Maison d'artiste",
            authorName = "Klaas Nienhuis",
            html = """<iframe width="640" height="360" src="https://sketchfab.com/models/dGUrytaktlDeNudCEGKk31oTJY/embed" frameborder="0"></iframe>"""
        )
        assertEquals(
            "https://sketchfab.com/models/dGUrytaktlDeNudCEGKk31oTJY/embed",
            resp.extractIframeSrc()
        )
        assertEquals(
            "https://sketchfab.com/models/abc123",
            SketchfabOEmbedClient.normalizeModelUrl("abc123")
        )
        assertEquals(
            "https://sketchfab.com/james/collections/fire-hydrants",
            SketchfabOEmbedClient.normalizeModelUrl("https://sketchfab.com/james/collections/fire-hydrants")
        )
    }
}
