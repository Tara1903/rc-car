package com.example.roboracer.tools

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a valid glTF 2.0 binary (.glb) file for the Robo Racer 4WD chassis.
 * Contains nodes for the chassis decks, wheels, TT motors, Prakti ESP32 board,
 * dual L298N drivers, and XL6019 converter.
 */
object GlbModelBuilder {

    data class Vertex(
        val x: Float, val y: Float, val z: Float,
        val nx: Float, val ny: Float, val nz: Float,
        val r: Float, val g: Float, val b: Float, val a: Float = 1.0f
    )

    data class MeshData(
        val name: String,
        val vertices: List<Vertex>,
        val indices: List<Int>,
        val translation: FloatArray = floatArrayOf(0f, 0f, 0f)
    )

    fun createBox(
        name: String,
        w: Float, h: Float, d: Float,
        r: Float, g: Float, b: Float, a: Float = 1f,
        tx: Float = 0f, ty: Float = 0f, tz: Float = 0f
    ): MeshData {
        val hw = w / 2f; val hh = h / 2f; val hd = d / 2f
        val verts = mutableListOf<Vertex>()
        val idxs = mutableListOf<Int>()

        fun addFace(p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, norm: FloatArray) {
            val base = verts.size
            verts.add(Vertex(p1[0], p1[1], p1[2], norm[0], norm[1], norm[2], r, g, b, a))
            verts.add(Vertex(p2[0], p2[1], p2[2], norm[0], norm[1], norm[2], r, g, b, a))
            verts.add(Vertex(p3[0], p3[1], p3[2], norm[0], norm[1], norm[2], r, g, b, a))
            verts.add(Vertex(p4[0], p4[1], p4[2], norm[0], norm[1], norm[2], r, g, b, a))
            idxs.addAll(listOf(base, base + 1, base + 2, base, base + 2, base + 3))
        }

        // +Z Front
        addFace(floatArrayOf(-hw, -hh, hd), floatArrayOf(hw, -hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(-hw, hh, hd), floatArrayOf(0f, 0f, 1f))
        // -Z Back
        addFace(floatArrayOf(hw, -hh, -hd), floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(hw, hh, -hd), floatArrayOf(0f, 0f, -1f))
        // +Y Top
        addFace(floatArrayOf(-hw, hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(hw, hh, -hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(0f, 1f, 0f))
        // -Y Bottom
        addFace(floatArrayOf(-hw, -hh, -hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, -hh, hd), floatArrayOf(-hw, -hh, hd), floatArrayOf(0f, -1f, 0f))
        // +X Right
        addFace(floatArrayOf(hw, -hh, hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, hh, -hd), floatArrayOf(hw, hh, hd), floatArrayOf(1f, 0f, 0f))
        // -X Left
        addFace(floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, -hh, hd), floatArrayOf(-hw, hh, hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(-1f, 0f, 0f))

        return MeshData(name, verts, idxs, floatArrayOf(tx, ty, tz))
    }

    fun createWheel(name: String, radius: Float, width: Float, tx: Float, ty: Float, tz: Float): MeshData {
        val verts = mutableListOf<Vertex>()
        val idxs = mutableListOf<Int>()
        val segments = 16
        val hw = width / 2f

        // Outer rubber tire cylinder
        for (i in 0 until segments) {
            val theta1 = (i * 2 * Math.PI / segments).toFloat()
            val theta2 = ((i + 1) * 2 * Math.PI / segments).toFloat()

            val z1 = radius * Math.cos(theta1.toDouble()).toFloat()
            val y1 = radius * Math.sin(theta1.toDouble()).toFloat()
            val z2 = radius * Math.cos(theta2.toDouble()).toFloat()
            val y2 = radius * Math.sin(theta2.toDouble()).toFloat()

            val base = verts.size
            // Outer tire surface (dark charcoal rubber)
            verts.add(Vertex(-hw, y1, z1, 0f, y1 / radius, z1 / radius, 0.12f, 0.12f, 0.14f))
            verts.add(Vertex(hw, y1, z1, 0f, y1 / radius, z1 / radius, 0.12f, 0.12f, 0.14f))
            verts.add(Vertex(hw, y2, z2, 0f, y2 / radius, z2 / radius, 0.12f, 0.12f, 0.14f))
            verts.add(Vertex(-hw, y2, z2, 0f, y2 / radius, z2 / radius, 0.12f, 0.12f, 0.14f))
            idxs.addAll(listOf(base, base + 1, base + 2, base, base + 2, base + 3))

            // Yellow wheel rim face
            val rimR = radius * 0.65f
            val rz1 = rimR * Math.cos(theta1.toDouble()).toFloat()
            val ry1 = rimR * Math.sin(theta1.toDouble()).toFloat()
            val rz2 = rimR * Math.cos(theta2.toDouble()).toFloat()
            val ry2 = rimR * Math.sin(theta2.toDouble()).toFloat()

            val rimBase = verts.size
            // Outer rim disc
            verts.add(Vertex(hw + 0.001f, 0f, 0f, 1f, 0f, 0f, 1.0f, 0.85f, 0.0f))
            verts.add(Vertex(hw + 0.001f, ry1, rz1, 1f, 0f, 0f, 1.0f, 0.85f, 0.0f))
            verts.add(Vertex(hw + 0.001f, ry2, rz2, 1f, 0f, 0f, 1.0f, 0.85f, 0.0f))
            idxs.addAll(listOf(rimBase, rimBase + 1, rimBase + 2))
        }

        return MeshData(name, verts, idxs, floatArrayOf(tx, ty, tz))
    }

    fun buildRobotCarGlb(): ByteArray {
        val meshes = mutableListOf<MeshData>()

        // 1. Lower Acrylic Deck (Translucent Yellow)
        meshes.add(createBox("Chassis_Lower", 0.14f, 0.003f, 0.18f, 1.0f, 0.85f, 0.0f, 0.9f, 0f, 0f, 0f))

        // 2. Upper Acrylic Deck (Translucent Yellow)
        meshes.add(createBox("Chassis_Upper", 0.14f, 0.003f, 0.16f, 1.0f, 0.85f, 0.0f, 0.9f, 0f, 0.035f, -0.01f))

        // 3. Standoff Brass Pillars (4x)
        meshes.add(createBox("Standoff_FL", 0.006f, 0.035f, 0.006f, 0.85f, 0.7f, 0.2f, 1f, -0.055f, 0.0175f, 0.06f))
        meshes.add(createBox("Standoff_FR", 0.006f, 0.035f, 0.006f, 0.85f, 0.7f, 0.2f, 1f, 0.055f, 0.0175f, 0.06f))
        meshes.add(createBox("Standoff_RL", 0.006f, 0.035f, 0.006f, 0.85f, 0.7f, 0.2f, 1f, -0.055f, 0.0175f, -0.06f))
        meshes.add(createBox("Standoff_RR", 0.006f, 0.035f, 0.006f, 0.85f, 0.7f, 0.2f, 1f, 0.055f, 0.0175f, -0.06f))

        // 4. Four TT Gear Motors (Yellow body with white shaft)
        meshes.add(createBox("Motor_FL", 0.022f, 0.018f, 0.060f, 1.0f, 0.75f, 0.0f, 1f, -0.055f, -0.012f, 0.055f))
        meshes.add(createBox("Motor_FR", 0.022f, 0.018f, 0.060f, 1.0f, 0.75f, 0.0f, 1f, 0.055f, -0.012f, 0.055f))
        meshes.add(createBox("Motor_RL", 0.022f, 0.018f, 0.060f, 1.0f, 0.75f, 0.0f, 1f, -0.055f, -0.012f, -0.055f))
        meshes.add(createBox("Motor_RR", 0.022f, 0.018f, 0.060f, 1.0f, 0.75f, 0.0f, 1f, 0.055f, -0.012f, -0.055f))

        // 5. Four Independent Wheels with Black Tires and Yellow Rims
        val wheelRadius = 0.033f
        val wheelWidth = 0.026f
        meshes.add(createWheel("Wheel_FL", wheelRadius, wheelWidth, -0.088f, -0.005f, 0.055f))
        meshes.add(createWheel("Wheel_FR", wheelRadius, wheelWidth, 0.088f, -0.005f, 0.055f))
        meshes.add(createWheel("Wheel_RL", wheelRadius, wheelWidth, -0.088f, -0.005f, -0.055f))
        meshes.add(createWheel("Wheel_RR", wheelRadius, wheelWidth, 0.088f, -0.005f, -0.055f))

        // 6. Prakti ESP32 30-Pin Dev Board on Top Deck
        // Black PCB
        meshes.add(createBox("ESP32_PCB", 0.028f, 0.002f, 0.052f, 0.1f, 0.1f, 0.12f, 1f, 0f, 0.038f, -0.03f))
        // Silver Metal RF Shield (ESP-WROOM-32)
        meshes.add(createBox("ESP32_Shield", 0.018f, 0.003f, 0.018f, 0.85f, 0.85f, 0.88f, 1f, 0f, 0.040f, -0.025f))
        // CP2102 IC
        meshes.add(createBox("ESP32_CP2102", 0.006f, 0.001f, 0.006f, 0.05f, 0.05f, 0.05f, 1f, 0f, 0.040f, -0.045f))

        // 7. Dual Tayal L298N Motor Drivers
        // Driver 1: Left (Red heatsink)
        meshes.add(createBox("L298N_Left_PCB", 0.043f, 0.002f, 0.043f, 0.7f, 0.1f, 0.1f, 1f, -0.035f, 0.038f, 0.035f))
        meshes.add(createBox("L298N_Left_Heatsink", 0.024f, 0.018f, 0.012f, 0.8f, 0.15f, 0.15f, 1f, -0.035f, 0.048f, 0.035f))
        // Driver 2: Right (Red heatsink)
        meshes.add(createBox("L298N_Right_PCB", 0.043f, 0.002f, 0.043f, 0.7f, 0.1f, 0.1f, 1f, 0.035f, 0.038f, 0.035f))
        meshes.add(createBox("L298N_Right_Heatsink", 0.024f, 0.018f, 0.012f, 0.8f, 0.15f, 0.15f, 1f, 0.035f, 0.048f, 0.035f))

        // 8. amiciSolar XL6019 Buck-Boost Module (Center)
        meshes.add(createBox("XL6019_PCB", 0.048f, 0.002f, 0.025f, 0.05f, 0.25f, 0.65f, 1f, 0f, 0.038f, 0.015f))
        // Toroidal Copper Inductor
        meshes.add(createBox("XL6019_Inductor", 0.012f, 0.008f, 0.012f, 0.72f, 0.45f, 0.20f, 1f, -0.012f, 0.043f, 0.015f))
        // Blue Potentiometer
        meshes.add(createBox("XL6019_Pot", 0.008f, 0.010f, 0.005f, 0.1f, 0.4f, 0.9f, 1f, 0.012f, 0.044f, 0.015f))

        // Serialize all geometry into Binary glTF (GLB)
        return serializeToGlb(meshes)
    }

    private fun serializeToGlb(meshes: List<MeshData>): ByteArray {
        val binStream = ByteArrayOutputStream()

        var totalVerts = 0
        var totalIdxs = 0
        for (m in meshes) {
            totalVerts += m.vertices.size
            totalIdxs += m.indices.size
        }

        // We will build one combined vertex buffer and index buffer for all primitives
        val posBuf = ByteBuffer.allocate(totalVerts * 12).order(ByteOrder.LITTLE_ENDIAN)
        val normBuf = ByteBuffer.allocate(totalVerts * 12).order(ByteOrder.LITTLE_ENDIAN)
        val colBuf = ByteBuffer.allocate(totalVerts * 16).order(ByteOrder.LITTLE_ENDIAN)
        val idxBuf = ByteBuffer.allocate(totalIdxs * 2).order(ByteOrder.LITTLE_ENDIAN) // uint16 indices

        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        val meshOffsets = mutableListOf<Triple<Int, Int, Int>>() // vertexStart, indexStart, indexCount
        var vOffset = 0
        var iOffset = 0

        for (m in meshes) {
            val vStart = vOffset
            val iStart = iOffset

            for (v in m.vertices) {
                val gx = v.x + m.translation[0]
                val gy = v.y + m.translation[1]
                val gz = v.z + m.translation[2]

                minX = minOf(minX, gx); minY = minOf(minY, gy); minZ = minOf(minZ, gz)
                maxX = maxOf(maxX, gx); maxY = maxOf(maxY, gy); maxZ = maxOf(maxZ, gz)

                posBuf.putFloat(gx); posBuf.putFloat(gy); posBuf.putFloat(gz)
                normBuf.putFloat(v.nx); normBuf.putFloat(v.ny); normBuf.putFloat(v.nz)
                colBuf.putFloat(v.r); colBuf.putFloat(v.g); colBuf.putFloat(v.b); colBuf.putFloat(v.a)
                vOffset++
            }

            for (idx in m.indices) {
                idxBuf.putShort((vStart + idx).toShort())
                iOffset++
            }
            meshOffsets.add(Triple(vStart, iStart, m.indices.size))
        }

        val posBytes = posBuf.array()
        val normBytes = normBuf.array()
        val colBytes = colBuf.array()
        val idxBytes = idxBuf.array()

        val posOffset = 0
        val normOffset = posOffset + posBytes.size
        val colOffset = normOffset + normBytes.size
        val idxOffset = colOffset + colBytes.size
        val totalBinSize = idxOffset + idxBytes.size

        // Build JSON Chunk
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\"asset\":{\"version\":\"2.0\",\"generator\":\"RoboRacer-GLB-Engine\"},")
        jsonBuilder.append("\"scene\":0,\"scenes\":[{\"nodes\":[")
        jsonBuilder.append((0 until meshes.size).joinToString(","))
        jsonBuilder.append("]}],\"nodes\":[")

        for (i in meshes.indices) {
            if (i > 0) jsonBuilder.append(",")
            jsonBuilder.append("{\"name\":\"${meshes[i].name}\",\"mesh\":$i}")
        }
        jsonBuilder.append("],\"meshes\":[")

        for (i in meshes.indices) {
            if (i > 0) jsonBuilder.append(",")
            // Accessor indices: 0 = POS, 1 = NORM, 2 = COLOR, 3+i = INDICES
            jsonBuilder.append("{\"name\":\"${meshes[i].name}\",\"primitives\":[{\"attributes\":{\"POSITION\":0,\"NORMAL\":1,\"COLOR_0\":2},\"indices\":${3 + i}}]}")
        }
        jsonBuilder.append("],\"accessors\":[")
        // Accessor 0: POSITION
        jsonBuilder.append("{\"bufferView\":0,\"componentType\":5126,\"count\":$totalVerts,\"type\":\"VEC3\",\"min\":[$minX,$minY,$minZ],\"max\":[$maxX,$maxY,$maxZ]},")
        // Accessor 1: NORMAL
        jsonBuilder.append("{\"bufferView\":1,\"componentType\":5126,\"count\":$totalVerts,\"type\":\"VEC3\"},")
        // Accessor 2: COLOR_0
        jsonBuilder.append("{\"bufferView\":2,\"componentType\":5126,\"count\":$totalVerts,\"type\":\"VEC4\"}")

        // Accessors for indices of each mesh
        for (i in meshes.indices) {
            val count = meshOffsets[i].third
            val byteOffset = meshOffsets[i].second * 2
            jsonBuilder.append(",{\"bufferView\":3,\"byteOffset\":$byteOffset,\"componentType\":5123,\"count\":$count,\"type\":\"SCALAR\"}")
        }

        jsonBuilder.append("],\"bufferViews\":[")
        // BufferView 0: POSITION
        jsonBuilder.append("{\"buffer\":0,\"byteOffset\":$posOffset,\"byteLength\":${posBytes.size},\"target\":34962},")
        // BufferView 1: NORMAL
        jsonBuilder.append("{\"buffer\":0,\"byteOffset\":$normOffset,\"byteLength\":${normBytes.size},\"target\":34962},")
        // BufferView 2: COLOR_0
        jsonBuilder.append("{\"buffer\":0,\"byteOffset\":$colOffset,\"byteLength\":${colBytes.size},\"target\":34962},")
        // BufferView 3: INDICES
        jsonBuilder.append("{\"buffer\":0,\"byteOffset\":$idxOffset,\"byteLength\":${idxBytes.size},\"target\":34963}")

        jsonBuilder.append("],\"buffers\":[{\"byteLength\":$totalBinSize}]}")

        val jsonBytes = jsonBuilder.toString().toByteArray(Charsets.UTF_8)
        val jsonPadding = (4 - (jsonBytes.size % 4)) % 4
        val paddedJsonLength = jsonBytes.size + jsonPadding

        val binPadding = (4 - (totalBinSize % 4)) % 4
        val paddedBinLength = totalBinSize + binPadding

        val totalGlbLength = 12 + 8 + paddedJsonLength + 8 + paddedBinLength

        val glbBuf = ByteBuffer.allocate(totalGlbLength).order(ByteOrder.LITTLE_ENDIAN)
        // 12-byte header
        glbBuf.putInt(0x46546C67) // "glTF"
        glbBuf.putInt(2)          // version 2
        glbBuf.putInt(totalGlbLength)

        // JSON Chunk Header
        glbBuf.putInt(paddedJsonLength)
        glbBuf.putInt(0x4E4F534A) // "JSON"
        glbBuf.put(jsonBytes)
        for (p in 0 until jsonPadding) glbBuf.put(0x20.toByte()) // pad with space

        // BIN Chunk Header
        glbBuf.putInt(paddedBinLength)
        glbBuf.putInt(0x004E4942) // "BIN\0"
        glbBuf.put(posBytes)
        glbBuf.put(normBytes)
        glbBuf.put(colBytes)
        glbBuf.put(idxBytes)
        for (p in 0 until binPadding) glbBuf.put(0x00.toByte())

        return glbBuf.array()
    }
}
