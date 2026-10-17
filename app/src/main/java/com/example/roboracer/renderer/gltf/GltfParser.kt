package com.example.roboracer.renderer.gltf

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * Robust, production-grade parser for glTF 2.0 Binary (.glb) assets.
 * Parses geometry, PBR materials, node transforms, and hierarchical scene graphs.
 * Runs on both Android (via asset InputStream) and JVM host unit tests (via File/InputStream).
 */
object GltfParser {

    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
    private const val CHUNK_TYPE_BIN = 0x004E4942 // "BIN\0"

    private const val COMP_BYTE = 5120
    private const val COMP_UNSIGNED_BYTE = 5121
    private const val COMP_SHORT = 5122
    private const val COMP_UNSIGNED_SHORT = 5123
    private const val COMP_UNSIGNED_INT = 5125
    private const val COMP_FLOAT = 5126

    // OpenGL ES constants (to avoid compile-time dependency on GLES20 during unit tests)
    const val GL_UNSIGNED_SHORT = 0x1403
    const val GL_UNSIGNED_INT = 0x1405

    fun parse(stream: InputStream): GltfModel {
        val bytes = stream.readBytes()
        return parse(bytes)
    }

    fun parse(bytes: ByteArray): GltfModel {
        require(bytes.size >= 12) { "GLB buffer too small (< 12 bytes)" }

        val headerBuf = ByteBuffer.wrap(bytes, 0, 12).order(ByteOrder.LITTLE_ENDIAN)
        val magic = headerBuf.getInt()
        val version = headerBuf.getInt()
        val totalLength = headerBuf.getInt()

        require(magic == GLB_MAGIC) { "Invalid GLB magic: 0x${Integer.toHexString(magic)}" }
        require(version == 2) { "Unsupported glTF version: $version (expected 2)" }

        var offset = 12
        var jsonString: String? = null
        var binBuffer: ByteBuffer? = null

        while (offset < bytes.size && offset < totalLength) {
            val chunkLen = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()
            val chunkType = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()
            offset += 8

            if (chunkType == CHUNK_TYPE_JSON) {
                jsonString = String(bytes, offset, chunkLen, Charsets.UTF_8)
            } else if (chunkType == CHUNK_TYPE_BIN) {
                binBuffer = ByteBuffer.wrap(bytes, offset, chunkLen).slice().order(ByteOrder.LITTLE_ENDIAN)
            }
            offset += chunkLen
        }

        requireNotNull(jsonString) { "Missing JSON chunk in GLB asset" }
        requireNotNull(binBuffer) { "Missing BIN chunk in GLB asset" }

        val gltfJson = JSONObject(jsonString)
        return buildModel(gltfJson, binBuffer)
    }

    private data class BufferViewDef(
        val byteOffset: Int,
        val byteLength: Int,
        val byteStride: Int
    )

    private data class AccessorDef(
        val bufferViewIndex: Int,
        val byteOffset: Int,
        val componentType: Int,
        val count: Int,
        val type: String
    )

    private fun buildModel(json: JSONObject, bin: ByteBuffer): GltfModel {
        // 1. Buffer Views
        val bufferViews = mutableListOf<BufferViewDef>()
        val bvArray = json.optJSONArray("bufferViews") ?: JSONArray()
        for (i in 0 until bvArray.length()) {
            val bvObj = bvArray.getJSONObject(i)
            bufferViews.add(
                BufferViewDef(
                    byteOffset = bvObj.optInt("byteOffset", 0),
                    byteLength = bvObj.getInt("byteLength"),
                    byteStride = bvObj.optInt("byteStride", 0)
                )
            )
        }

        // 2. Accessors
        val accessors = mutableListOf<AccessorDef>()
        val accArray = json.optJSONArray("accessors") ?: JSONArray()
        for (i in 0 until accArray.length()) {
            val accObj = accArray.getJSONObject(i)
            accessors.add(
                AccessorDef(
                    bufferViewIndex = accObj.getInt("bufferView"),
                    byteOffset = accObj.optInt("byteOffset", 0),
                    componentType = accObj.getInt("componentType"),
                    count = accObj.getInt("count"),
                    type = accObj.getString("type")
                )
            )
        }

        // 3. Materials
        val materials = mutableListOf<GltfMaterial>()
        val matArray = json.optJSONArray("materials") ?: JSONArray()
        for (i in 0 until matArray.length()) {
            val matObj = matArray.getJSONObject(i)
            val name = matObj.optString("name", "Material_$i")
            val pbrObj = matObj.optJSONObject("pbrMetallicRoughness")

            val baseColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
            var metallic = 0.0f
            var roughness = 0.5f

            if (pbrObj != null) {
                val bcArray = pbrObj.optJSONArray("baseColorFactor")
                if (bcArray != null && bcArray.length() >= 4) {
                    baseColor[0] = bcArray.getDouble(0).toFloat()
                    baseColor[1] = bcArray.getDouble(1).toFloat()
                    baseColor[2] = bcArray.getDouble(2).toFloat()
                    baseColor[3] = bcArray.getDouble(3).toFloat()
                }
                metallic = pbrObj.optDouble("metallicFactor", 0.0).toFloat()
                roughness = pbrObj.optDouble("roughnessFactor", 0.5).toFloat()
            }

            val emissive = floatArrayOf(0.0f, 0.0f, 0.0f)
            val emArray = matObj.optJSONArray("emissiveFactor")
            if (emArray != null && emArray.length() >= 3) {
                emissive[0] = emArray.getDouble(0).toFloat()
                emissive[1] = emArray.getDouble(1).toFloat()
                emissive[2] = emArray.getDouble(2).toFloat()
            }

            val alphaMode = matObj.optString("alphaMode", "OPAQUE")
            val doubleSided = matObj.optBoolean("doubleSided", false)

            val transmissionObj = matObj.optJSONObject("extensions")?.optJSONObject("KHR_materials_transmission")
            val transmission = transmissionObj?.optDouble("transmissionFactor", 0.0)?.toFloat() ?: 0.0f
            if (transmission > 0.01f && baseColor[3] >= 0.99f) {
                baseColor[3] = (1.0f - transmission * 0.45f).coerceIn(0.1f, 1.0f)
            }

            materials.add(
                GltfMaterial(
                    name = name,
                    baseColorFactor = baseColor,
                    metallicFactor = metallic,
                    roughnessFactor = roughness,
                    emissiveFactor = emissive,
                    isDoubleSided = doubleSided,
                    isTransparent = alphaMode == "BLEND" || baseColor[3] < 0.99f || transmission > 0.01f
                )
            )
        }

        val defaultMaterial = GltfMaterial("Default_Material")

        // 4. Meshes
        val meshes = mutableListOf<GltfMesh>()
        val meshArray = json.optJSONArray("meshes") ?: JSONArray()
        for (m in 0 until meshArray.length()) {
            val meshObj = meshArray.getJSONObject(m)
            val meshName = meshObj.optString("name", "Mesh_$m")
            val primArray = meshObj.getJSONArray("primitives")
            val primList = mutableListOf<GltfPrimitive>()

            for (p in 0 until primArray.length()) {
                val primObj = primArray.getJSONObject(p)
                val attrs = primObj.getJSONObject("attributes")

                // Position buffer
                val posAccIdx = attrs.getInt("POSITION")
                val posAcc = accessors[posAccIdx]
                val posBuffer = extractFloatBuffer(posAcc, bufferViews[posAcc.bufferViewIndex], bin, 3)

                // Normal buffer
                var normBuffer: FloatBuffer? = null
                if (attrs.has("NORMAL")) {
                    val normAcc = accessors[attrs.getInt("NORMAL")]
                    normBuffer = extractFloatBuffer(normAcc, bufferViews[normAcc.bufferViewIndex], bin, 3)
                }

                // Color buffer
                var colorBuffer: FloatBuffer? = null
                if (attrs.has("COLOR_0")) {
                    val colAcc = accessors[attrs.getInt("COLOR_0")]
                    colorBuffer = extractColorBuffer(colAcc, bufferViews[colAcc.bufferViewIndex], bin)
                }

                // Index buffer
                var indexBuffer: java.nio.Buffer? = null
                var indexCount = 0
                var indexType = GL_UNSIGNED_SHORT

                if (primObj.has("indices")) {
                    val idxAcc = accessors[primObj.getInt("indices")]
                    indexCount = idxAcc.count
                    val bv = bufferViews[idxAcc.bufferViewIndex]
                    val startOffset = bv.byteOffset + idxAcc.byteOffset

                    if (idxAcc.componentType == COMP_UNSIGNED_SHORT) {
                        indexType = GL_UNSIGNED_SHORT
                        val sb = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
                        val shortView = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                        shortView.position(startOffset)
                        for (i in 0 until indexCount) {
                            sb.put(shortView.getShort())
                        }
                        sb.position(0)
                        indexBuffer = sb
                    } else if (idxAcc.componentType == COMP_UNSIGNED_INT) {
                        indexType = GL_UNSIGNED_INT
                        val ib = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
                        val intView = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                        intView.position(startOffset)
                        for (i in 0 until indexCount) {
                            ib.put(intView.getInt())
                        }
                        ib.position(0)
                        indexBuffer = ib
                    } else if (idxAcc.componentType == COMP_UNSIGNED_BYTE) {
                        indexType = GL_UNSIGNED_SHORT
                        val sb = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
                        val byteView = bin.duplicate()
                        byteView.position(startOffset)
                        for (i in 0 until indexCount) {
                            sb.put((byteView.get().toInt() and 0xFF).toShort())
                        }
                        sb.position(0)
                        indexBuffer = sb
                    }
                }

                val matIndex = primObj.optInt("material", -1)
                val mat = if (matIndex in materials.indices) materials[matIndex] else defaultMaterial

                primList.add(
                    GltfPrimitive(
                        positionBuffer = posBuffer,
                        normalBuffer = normBuffer,
                        colorBuffer = colorBuffer,
                        indexBuffer = indexBuffer,
                        indexCount = indexCount,
                        indexType = indexType,
                        vertexCount = posAcc.count,
                        material = mat
                    )
                )
            }
            meshes.add(GltfMesh(meshName, primList))
        }

        // 5. Nodes
        val nodes = mutableListOf<GltfNode>()
        val nodeChildrenIndices = mutableListOf<List<Int>>()
        val nodeArray = json.optJSONArray("nodes") ?: JSONArray()

        for (n in 0 until nodeArray.length()) {
            val nodeObj = nodeArray.getJSONObject(n)
            val name = nodeObj.optString("name", "Node_$n")
            val meshIdx = if (nodeObj.has("mesh")) nodeObj.getInt("mesh") else -1
            val mesh = if (meshIdx in meshes.indices) meshes[meshIdx] else null

            val node = GltfNode(name, mesh)

            // Parse Transform
            if (nodeObj.has("matrix")) {
                val mArray = nodeObj.getJSONArray("matrix")
                for (i in 0 until 16) {
                    node.baseLocalMatrix[i] = mArray.getDouble(i).toFloat()
                }
            } else {
                Math3D.setIdentityM(node.baseLocalMatrix)
                if (nodeObj.has("translation")) {
                    val t = nodeObj.getJSONArray("translation")
                    Math3D.translateM(
                        node.baseLocalMatrix, 0,
                        t.getDouble(0).toFloat(),
                        t.getDouble(1).toFloat(),
                        t.getDouble(2).toFloat()
                    )
                }
                if (nodeObj.has("rotation")) {
                    val r = nodeObj.getJSONArray("rotation")
                    val qx = r.getDouble(0).toFloat()
                    val qy = r.getDouble(1).toFloat()
                    val qz = r.getDouble(2).toFloat()
                    val qw = r.getDouble(3).toFloat()
                    val rotMat = FloatArray(16)
                    Math3D.quaternionToMatrix(qx, qy, qz, qw, rotMat)
                    val temp = FloatArray(16)
                    Math3D.multiplyMM(temp, 0, node.baseLocalMatrix, 0, rotMat, 0)
                    System.arraycopy(temp, 0, node.baseLocalMatrix, 0, 16)
                }
                if (nodeObj.has("scale")) {
                    val s = nodeObj.getJSONArray("scale")
                    Math3D.scaleM(
                        node.baseLocalMatrix, 0,
                        s.getDouble(0).toFloat(),
                        s.getDouble(1).toFloat(),
                        s.getDouble(2).toFloat()
                    )
                }
            }

            nodes.add(node)

            val childList = mutableListOf<Int>()
            val cArray = nodeObj.optJSONArray("children")
            if (cArray != null) {
                for (c in 0 until cArray.length()) {
                    childList.add(cArray.getInt(c))
                }
            }
            nodeChildrenIndices.add(childList)
        }

        // Assemble node parent-child hierarchy
        for (i in nodes.indices) {
            val parentNode = nodes[i]
            for (childIdx in nodeChildrenIndices[i]) {
                if (childIdx in nodes.indices) {
                    parentNode.addChild(nodes[childIdx])
                }
            }
        }

        // Determine scene root nodes
        val defaultSceneIdx = json.optInt("scene", 0)
        val scenesArray = json.optJSONArray("scenes")
        val rootNodeIndices = mutableListOf<Int>()

        if (scenesArray != null && defaultSceneIdx in 0 until scenesArray.length()) {
            val scObj = scenesArray.getJSONObject(defaultSceneIdx)
            val scNodes = scObj.optJSONArray("nodes")
            if (scNodes != null) {
                for (i in 0 until scNodes.length()) {
                    rootNodeIndices.add(scNodes.getInt(i))
                }
            }
        }

        val sceneRoot: GltfNode
        if (rootNodeIndices.isNotEmpty()) {
            if (rootNodeIndices.size == 1) {
                sceneRoot = nodes[rootNodeIndices[0]]
            } else {
                sceneRoot = GltfNode("Scene_Root")
                for (rIdx in rootNodeIndices) {
                    if (rIdx in nodes.indices) {
                        sceneRoot.addChild(nodes[rIdx])
                    }
                }
            }
        } else {
            // Find nodes that have no parent
            val topLevel = nodes.filter { it.parent == null }
            if (topLevel.size == 1) {
                sceneRoot = topLevel[0]
            } else {
                sceneRoot = GltfNode("Scene_Root")
                for (tl in topLevel) {
                    sceneRoot.addChild(tl)
                }
            }
        }

        return GltfModel(sceneRoot, nodes, materials)
    }

    private fun extractFloatBuffer(acc: AccessorDef, bv: BufferViewDef, bin: ByteBuffer, components: Int): FloatBuffer {
        val count = acc.count
        val totalFloats = count * components
        val fb = ByteBuffer.allocateDirect(totalFloats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val startOffset = bv.byteOffset + acc.byteOffset
        val stride = if (bv.byteStride > 0) bv.byteStride else components * 4

        val view = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) {
            view.position(startOffset + i * stride)
            for (c in 0 until components) {
                fb.put(view.getFloat())
            }
        }
        fb.position(0)
        return fb
    }

    private fun extractColorBuffer(acc: AccessorDef, bv: BufferViewDef, bin: ByteBuffer): FloatBuffer {
        val count = acc.count
        val fb = ByteBuffer.allocateDirect(count * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val startOffset = bv.byteOffset + acc.byteOffset
        val isVec3 = acc.type == "VEC3"
        val view = bin.duplicate().order(ByteOrder.LITTLE_ENDIAN)

        if (acc.componentType == COMP_FLOAT) {
            val stride = if (bv.byteStride > 0) bv.byteStride else (if (isVec3) 3 else 4) * 4
            for (i in 0 until count) {
                view.position(startOffset + i * stride)
                fb.put(view.getFloat())
                fb.put(view.getFloat())
                fb.put(view.getFloat())
                fb.put(if (isVec3) 1.0f else view.getFloat())
            }
        } else if (acc.componentType == COMP_UNSIGNED_BYTE) {
            val stride = if (bv.byteStride > 0) bv.byteStride else (if (isVec3) 3 else 4)
            for (i in 0 until count) {
                view.position(startOffset + i * stride)
                fb.put((view.get().toInt() and 0xFF) / 255.0f)
                fb.put((view.get().toInt() and 0xFF) / 255.0f)
                fb.put((view.get().toInt() and 0xFF) / 255.0f)
                fb.put(if (isVec3) 1.0f else (view.get().toInt() and 0xFF) / 255.0f)
            }
        }
        fb.position(0)
        return fb
    }
}
