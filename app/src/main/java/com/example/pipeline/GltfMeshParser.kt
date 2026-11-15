package com.example.pipeline

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ParsedGltfMesh(
    val meshName: String,
    val vertexCount: Int,
    val triangleCount: Int,
    val submeshCount: Int,
    val hasAnimations: Boolean,
    val boundsMin: FloatArray,
    val boundsMax: FloatArray,
    /**
     * Interleaved triangle vertex buffer ready for OpenGL ES 2.0 glDrawArrays:
     * [px, py, pz, nx, ny, nz, r, g, b] per vertex (9 floats per vertex).
     */
    val interleavedTriangles: FloatArray
) {
    val normalizedScale: Float
        get() {
            val dx = boundsMax[0] - boundsMin[0]
            val dy = boundsMax[1] - boundsMin[1]
            val dz = boundsMax[2] - boundsMin[2]
            val maxDim = max(dx, max(dy, dz)).coerceAtLeast(0.001f)
            return 2.0f / maxDim
        }

    val centerOffset: FloatArray
        get() = floatArrayOf(
            (boundsMin[0] + boundsMax[0]) * 0.5f,
            (boundsMin[1] + boundsMax[1]) * 0.5f,
            (boundsMin[2] + boundsMax[2]) * 0.5f
        )
}

object GltfMeshParser {

    private const val MAX_PREVIEW_TRIANGLES = 24_000

    fun parseFromFile(file: File): Result<ParsedGltfMesh> = runCatching {
        if (!file.exists()) error("Model file does not exist: ${file.name}")
        if (file.extension.equals("glb", ignoreCase = true)) {
            parseGlb(file)
        } else {
            parseGltf(file)
        }
    }

    private fun parseGlb(file: File): ParsedGltfMesh {
        val bytes = file.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        if (magic != 0x46546C67) error("Invalid GLB magic header")
        buf.int // version
        buf.int // total length

        var jsonText: String? = null
        var binChunk: ByteArray? = null

        while (buf.remaining() >= 8) {
            val chunkLen = buf.int
            val chunkType = buf.int
            if (chunkLen < 0 || chunkLen > buf.remaining()) break
            val chunkBytes = ByteArray(chunkLen)
            buf.get(chunkBytes)
            when (chunkType) {
                0x4E4F534A -> jsonText = String(chunkBytes, Charsets.UTF_8)
                0x004E4942 -> binChunk = chunkBytes
            }
        }

        val root = JSONObject(jsonText ?: error("Missing JSON chunk in GLB"))
        val buffers = listOfNotNull(binChunk)
        return buildMeshFromJson(root, buffers, file.nameWithoutExtension)
    }

    private fun parseGltf(file: File): ParsedGltfMesh {
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val buffersJson = root.optJSONArray("buffers") ?: JSONArray()
        val buffers = mutableListOf<ByteArray>()
        for (i in 0 until buffersJson.length()) {
            val bObj = buffersJson.getJSONObject(i)
            val uri = bObj.optString("uri", "")
            val data = when {
                uri.startsWith("data:") && uri.contains(";base64,") -> {
                    val b64 = uri.substringAfter(";base64,")
                    decodeBase64(b64)
                }
                uri.isNotBlank() -> {
                    val binFile = File(file.parentFile, uri)
                    if (!binFile.exists()) error("Missing referenced binary buffer: $uri")
                    binFile.readBytes()
                }
                else -> ByteArray(0)
            }
            buffers.add(data)
        }
        return buildMeshFromJson(root, buffers, file.parentFile?.name ?: file.nameWithoutExtension)
    }

    private fun buildMeshFromJson(
        root: JSONObject,
        buffers: List<ByteArray>,
        fallbackName: String
    ): ParsedGltfMesh {
        val accessors = root.optJSONArray("accessors") ?: JSONArray()
        val bufferViews = root.optJSONArray("bufferViews") ?: JSONArray()
        val meshes = root.optJSONArray("meshes") ?: JSONArray()
        val materials = root.optJSONArray("materials") ?: JSONArray()
        val animations = root.optJSONArray("animations")

        val floatsOut = ArrayList<Float>(1024)
        var totalVertices = 0
        var totalTriangles = 0
        var submeshCount = 0

        val minB = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        val maxB = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)

        val meshCount = meshes.length()
        for (mIdx in 0 until meshCount) {
            val meshObj = meshes.getJSONObject(mIdx)
            val primitives = meshObj.optJSONArray("primitives") ?: continue
            for (pIdx in 0 until primitives.length()) {
                submeshCount++
                val prim = primitives.getJSONObject(pIdx)
                val mode = prim.optInt("mode", 4)
                if (mode != 4) continue // TRIANGLES only

                val attrs = prim.optJSONObject("attributes") ?: continue
                val posAccIdx = attrs.optInt("POSITION", -1)
                if (posAccIdx < 0) continue

                val positions = readVec3Accessor(posAccIdx, accessors, bufferViews, buffers)
                val normAccIdx = attrs.optInt("NORMAL", -1)
                val normals = if (normAccIdx >= 0) {
                    readVec3Accessor(normAccIdx, accessors, bufferViews, buffers)
                } else null

                val matIdx = prim.optInt("material", -1)
                val color = readBaseColor(matIdx, materials)

                val vertCountInPrim = positions.size / 3
                totalVertices += vertCountInPrim

                for (v in 0 until vertCountInPrim) {
                    val x = positions[v * 3]
                    val y = positions[v * 3 + 1]
                    val z = positions[v * 3 + 2]
                    minB[0] = min(minB[0], x); minB[1] = min(minB[1], y); minB[2] = min(minB[2], z)
                    maxB[0] = max(maxB[0], x); maxB[1] = max(maxB[1], y); maxB[2] = max(maxB[2], z)
                }

                val idxAccIdx = prim.optInt("indices", -1)
                val indices = if (idxAccIdx >= 0) {
                    readIndicesAccessor(idxAccIdx, accessors, bufferViews, buffers)
                } else {
                    IntArray(vertCountInPrim) { it }
                }

                val triCount = indices.size / 3
                totalTriangles += triCount
                val step = max(1, triCount / max(1, MAX_PREVIEW_TRIANGLES - (floatsOut.size / 27)))

                var t = 0
                while (t + 2 < indices.size && (floatsOut.size / 27) < MAX_PREVIEW_TRIANGLES) {
                    val i0 = indices[t]
                    val i1 = indices[t + 1]
                    val i2 = indices[t + 2]
                    if (i0 * 3 + 2 < positions.size && i1 * 3 + 2 < positions.size && i2 * 3 + 2 < positions.size) {
                        val ax = positions[i0 * 3]; val ay = positions[i0 * 3 + 1]; val az = positions[i0 * 3 + 2]
                        val bx = positions[i1 * 3]; val by = positions[i1 * 3 + 1]; val bz = positions[i1 * 3 + 2]
                        val cx = positions[i2 * 3]; val cy = positions[i2 * 3 + 1]; val cz = positions[i2 * 3 + 2]

                        var fnx = 0f; var fny = 1f; var fnz = 0f
                        if (normals == null) {
                            val ux = bx - ax; val uy = by - ay; val uz = bz - az
                            val vx = cx - ax; val vy = cy - ay; val vz = cz - az
                            val nx = uy * vz - uz * vy
                            val ny = uz * vx - ux * vz
                            val nz = ux * vy - uy * vx
                            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                            fnx = nx / len; fny = ny / len; fnz = nz / len
                        }

                        appendVertex(floatsOut, ax, ay, az, normals, i0, fnx, fny, fnz, color)
                        appendVertex(floatsOut, bx, by, bz, normals, i1, fnx, fny, fnz, color)
                        appendVertex(floatsOut, cx, cy, cz, normals, i2, fnx, fny, fnz, color)
                    }
                    t += 3 * step
                }
            }
        }

        if (minB[0] == Float.MAX_VALUE) {
            minB[0] = -0.5f; minB[1] = -0.5f; minB[2] = -0.5f
            maxB[0] = 0.5f; maxB[1] = 0.5f; maxB[2] = 0.5f
        }

        return ParsedGltfMesh(
            meshName = fallbackName,
            vertexCount = totalVertices,
            triangleCount = totalTriangles,
            submeshCount = submeshCount,
            hasAnimations = animations != null && animations.length() > 0,
            boundsMin = minB,
            boundsMax = maxB,
            interleavedTriangles = floatsOut.toFloatArray()
        )
    }

    private fun appendVertex(
        out: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        normals: FloatArray?, idx: Int,
        fnx: Float, fny: Float, fnz: Float,
        color: FloatArray
    ) {
        out.add(x); out.add(y); out.add(z)
        if (normals != null && idx * 3 + 2 < normals.size) {
            out.add(normals[idx * 3]); out.add(normals[idx * 3 + 1]); out.add(normals[idx * 3 + 2])
        } else {
            out.add(fnx); out.add(fny); out.add(fnz)
        }
        out.add(color[0]); out.add(color[1]); out.add(color[2])
    }

    private fun readBaseColor(matIdx: Int, materials: JSONArray): FloatArray {
        if (matIdx < 0 || matIdx >= materials.length()) return floatArrayOf(0.75f, 0.82f, 0.90f)
        val mat = materials.getJSONObject(matIdx)
        val pbr = mat.optJSONObject("pbrMetallicRoughness")
        val factor = pbr?.optJSONArray("baseColorFactor")
        return if (factor != null && factor.length() >= 3) {
            floatArrayOf(
                factor.optDouble(0, 0.8).toFloat(),
                factor.optDouble(1, 0.8).toFloat(),
                factor.optDouble(2, 0.8).toFloat()
            )
        } else {
            floatArrayOf(0.72f, 0.80f, 0.88f)
        }
    }

    private fun readVec3Accessor(
        accIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        buffers: List<ByteArray>
    ): FloatArray {
        val acc = accessors.getJSONObject(accIdx)
        val count = acc.getInt("count")
        val bvIdx = acc.getInt("bufferView")
        val accOffset = acc.optInt("byteOffset", 0)
        val bv = bufferViews.getJSONObject(bvIdx)
        val bufIdx = bv.optInt("buffer", 0)
        val bvOffset = bv.optInt("byteOffset", 0)
        val stride = bv.optInt("byteStride", 12).coerceAtLeast(12)
        val bytes = buffers[bufIdx]
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(count * 3)
        for (i in 0 until count) {
            val pos = bvOffset + accOffset + i * stride
            if (pos + 12 <= bytes.size) {
                out[i * 3] = bb.getFloat(pos)
                out[i * 3 + 1] = bb.getFloat(pos + 4)
                out[i * 3 + 2] = bb.getFloat(pos + 8)
            }
        }
        return out
    }

    private fun readIndicesAccessor(
        accIdx: Int,
        accessors: JSONArray,
        bufferViews: JSONArray,
        buffers: List<ByteArray>
    ): IntArray {
        val acc = accessors.getJSONObject(accIdx)
        val count = acc.getInt("count")
        val componentType = acc.getInt("componentType")
        val bvIdx = acc.getInt("bufferView")
        val accOffset = acc.optInt("byteOffset", 0)
        val bv = bufferViews.getJSONObject(bvIdx)
        val bufIdx = bv.optInt("buffer", 0)
        val bvOffset = bv.optInt("byteOffset", 0)
        val bytes = buffers[bufIdx]
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val out = IntArray(count)

        val elemSize = when (componentType) {
            5121 -> 1 // UNSIGNED_BYTE
            5123 -> 2 // UNSIGNED_SHORT
            5125 -> 4 // UNSIGNED_INT
            else -> 2
        }
        val stride = bv.optInt("byteStride", elemSize).coerceAtLeast(elemSize)

        for (i in 0 until count) {
            val pos = bvOffset + accOffset + i * stride
            if (pos + elemSize <= bytes.size) {
                out[i] = when (componentType) {
                    5121 -> bytes[pos].toInt() and 0xFF
                    5123 -> bb.getShort(pos).toInt() and 0xFFFF
                    5125 -> bb.getInt(pos)
                    else -> 0
                }
            }
        }
        return out
    }

    private fun decodeBase64(input: String): ByteArray {
        val clean = input.filter { !it.isWhitespace() }
        if (clean.isEmpty()) return ByteArray(0)
        val table = IntArray(256) { -1 }
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        for (i in chars.indices) table[chars[i].code] = i
        val padding = clean.count { it == '=' }
        val outLen = (clean.length * 3) / 4 - padding
        val out = ByteArray(outLen.coerceAtLeast(0))
        var outIdx = 0
        var i = 0
        while (i + 3 < clean.length) {
            val b0 = table[clean[i].code.coerceIn(0, 255)]
            val b1 = table[clean[i + 1].code.coerceIn(0, 255)]
            val b2 = if (clean[i + 2] == '=') 0 else table[clean[i + 2].code.coerceIn(0, 255)]
            val b3 = if (clean[i + 3] == '=') 0 else table[clean[i + 3].code.coerceIn(0, 255)]
            val triple = (b0 shl 18) or (b1 shl 12) or (b2 shl 6) or b3
            if (outIdx < out.size) out[outIdx++] = ((triple shr 16) and 0xFF).toByte()
            if (outIdx < out.size) out[outIdx++] = ((triple shr 8) and 0xFF).toByte()
            if (outIdx < out.size) out[outIdx++] = (triple and 0xFF).toByte()
            i += 4
        }
        return out
    }
}
