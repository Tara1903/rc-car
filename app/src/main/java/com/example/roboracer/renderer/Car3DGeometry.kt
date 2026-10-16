package com.example.roboracer.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Geometry definitions for OpenGL ES 2.0/3.0 rendering of the Robo Racer 4WD vehicle.
 */
class Mesh3D(
    val vertexBuffer: FloatBuffer, // pos(3) + normal(3) + color(4) = 10 floats per vertex
    val indexBuffer: ShortBuffer,
    val indexCount: Int,
    val translation: FloatArray = floatArrayOf(0f, 0f, 0f)
) {
    companion object {
        const val STRIDE = (3 + 3 + 4) * 4 // 40 bytes per vertex

        fun createBox(
            w: Float, h: Float, d: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f,
            tx: Float = 0f, ty: Float = 0f, tz: Float = 0f
        ): Mesh3D {
            val hw = w / 2f; val hh = h / 2f; val hd = d / 2f
            val rawVerts = mutableListOf<Float>()
            val rawIdxs = mutableListOf<Short>()

            fun addFace(p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, n: FloatArray) {
                val base = (rawVerts.size / 10).toShort()
                for (p in listOf(p1, p2, p3, p4)) {
                    rawVerts.addAll(listOf(p[0] + tx, p[1] + ty, p[2] + tz, n[0], n[1], n[2], r, g, b, a))
                }
                rawIdxs.addAll(listOf(base, (base + 1).toShort(), (base + 2).toShort(), base, (base + 2).toShort(), (base + 3).toShort()))
            }

            // 6 faces with proper normals
            addFace(floatArrayOf(-hw, -hh, hd), floatArrayOf(hw, -hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(-hw, hh, hd), floatArrayOf(0f, 0f, 1f))
            addFace(floatArrayOf(hw, -hh, -hd), floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(hw, hh, -hd), floatArrayOf(0f, 0f, -1f))
            addFace(floatArrayOf(-hw, hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(hw, hh, -hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(0f, 1f, 0f))
            addFace(floatArrayOf(-hw, -hh, -hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, -hh, hd), floatArrayOf(-hw, -hh, hd), floatArrayOf(0f, -1f, 0f))
            addFace(floatArrayOf(hw, -hh, hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, hh, -hd), floatArrayOf(hw, hh, hd), floatArrayOf(1f, 0f, 0f))
            addFace(floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, -hh, hd), floatArrayOf(-hw, hh, hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(-1f, 0f, 0f))

            val vBuf = ByteBuffer.allocateDirect(rawVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            rawVerts.forEach { vBuf.put(it) }
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(rawIdxs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            rawIdxs.forEach { iBuf.put(it) }
            iBuf.position(0)

            return Mesh3D(vBuf, iBuf, rawIdxs.size, floatArrayOf(tx, ty, tz))
        }

        fun createCylinder(
            radius: Float, height: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f,
            tx: Float = 0f, ty: Float = 0f, tz: Float = 0f,
            segments: Int = 12
        ): Mesh3D {
            val rawVerts = mutableListOf<Float>()
            val rawIdxs = mutableListOf<Short>()
            val hh = height / 2f

            for (i in 0 until segments) {
                val t1 = (i * 2 * Math.PI / segments).toFloat()
                val t2 = ((i + 1) * 2 * Math.PI / segments).toFloat()
                val x1 = radius * cos(t1); val z1 = radius * sin(t1)
                val x2 = radius * cos(t2); val z2 = radius * sin(t2)

                val n1x = cos(t1); val n1z = sin(t1)
                val n2x = cos(t2); val n2z = sin(t2)

                val base = (rawVerts.size / 10).toShort()
                rawVerts.addAll(listOf(x1 + tx, -hh + ty, z1 + tz, n1x, 0f, n1z, r, g, b, a))
                rawVerts.addAll(listOf(x2 + tx, -hh + ty, z2 + tz, n2x, 0f, n2z, r, g, b, a))
                rawVerts.addAll(listOf(x2 + tx, hh + ty, z2 + tz, n2x, 0f, n2z, r, g, b, a))
                rawVerts.addAll(listOf(x1 + tx, hh + ty, z1 + tz, n1x, 0f, n1z, r, g, b, a))
                rawIdxs.addAll(listOf(base, (base + 1).toShort(), (base + 2).toShort(), base, (base + 2).toShort(), (base + 3).toShort()))
            }

            val vBuf = ByteBuffer.allocateDirect(rawVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            rawVerts.forEach { vBuf.put(it) }
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(rawIdxs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            rawIdxs.forEach { iBuf.put(it) }
            iBuf.position(0)

            return Mesh3D(vBuf, iBuf, rawIdxs.size, floatArrayOf(tx, ty, tz))
        }

        fun createWheel(radius: Float, width: Float, isLeft: Boolean): Mesh3D {
            val rawVerts = mutableListOf<Float>()
            val rawIdxs = mutableListOf<Short>()
            val segments = 20
            val hw = width / 2f
            val outwardX = if (isLeft) -hw else hw
            val inwardX = if (isLeft) hw else -hw

            for (i in 0 until segments) {
                val t1 = (i * 2 * Math.PI / segments).toFloat()
                val t2 = ((i + 1) * 2 * Math.PI / segments).toFloat()

                val z1 = radius * cos(t1); val y1 = radius * sin(t1)
                val z2 = radius * cos(t2); val y2 = radius * sin(t2)

                // 1. Tire Tread (Rubber Black)
                val base = (rawVerts.size / 10).toShort()
                val treadColor = if (i % 2 == 0) listOf(0.10f, 0.11f, 0.13f, 1f) else listOf(0.14f, 0.15f, 0.18f, 1f)
                rawVerts.addAll(listOf(-hw, y1, z1, 0f, y1 / radius, z1 / radius, treadColor[0], treadColor[1], treadColor[2], 1f))
                rawVerts.addAll(listOf(hw, y1, z1, 0f, y1 / radius, z1 / radius, treadColor[0], treadColor[1], treadColor[2], 1f))
                rawVerts.addAll(listOf(hw, y2, z2, 0f, y2 / radius, z2 / radius, treadColor[0], treadColor[1], treadColor[2], 1f))
                rawVerts.addAll(listOf(-hw, y2, z2, 0f, y2 / radius, z2 / radius, treadColor[0], treadColor[1], treadColor[2], 1f))
                rawIdxs.addAll(listOf(base, (base + 1).toShort(), (base + 2).toShort(), base, (base + 2).toShort(), (base + 3).toShort()))

                // 2. Yellow Rim Outer Face
                val rimR = radius * 0.70f
                val rz1 = rimR * cos(t1); val ry1 = rimR * sin(t1)
                val rz2 = rimR * cos(t2); val ry2 = rimR * sin(t2)
                val normalX = if (isLeft) -1f else 1f

                val rBase = (rawVerts.size / 10).toShort()
                rawVerts.addAll(listOf(outwardX, 0f, 0f, normalX, 0f, 0f, 1.0f, 0.88f, 0.0f, 1f))
                rawVerts.addAll(listOf(outwardX, ry1, rz1, normalX, 0f, 0f, 1.0f, 0.88f, 0.0f, 1f))
                rawVerts.addAll(listOf(outwardX, ry2, rz2, normalX, 0f, 0f, 1.0f, 0.88f, 0.0f, 1f))
                rawIdxs.addAll(listOf(rBase, (rBase + 1).toShort(), (rBase + 2).toShort()))

                // 3. Black Center Hub
                val hubR = radius * 0.25f
                val hz1 = hubR * cos(t1); val hy1 = hubR * sin(t1)
                val hz2 = hubR * cos(t2); val hy2 = hubR * sin(t2)
                val hBase = (rawVerts.size / 10).toShort()
                rawVerts.addAll(listOf(outwardX * 1.02f, 0f, 0f, normalX, 0f, 0f, 0.05f, 0.05f, 0.05f, 1f))
                rawVerts.addAll(listOf(outwardX * 1.02f, hy1, hz1, normalX, 0f, 0f, 0.05f, 0.05f, 0.05f, 1f))
                rawVerts.addAll(listOf(outwardX * 1.02f, hy2, hz2, normalX, 0f, 0f, 0.05f, 0.05f, 0.05f, 1f))
                rawIdxs.addAll(listOf(hBase, (hBase + 1).toShort(), (hBase + 2).toShort()))
            }

            val vBuf = ByteBuffer.allocateDirect(rawVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            rawVerts.forEach { vBuf.put(it) }
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(rawIdxs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            rawIdxs.forEach { iBuf.put(it) }
            iBuf.position(0)

            return Mesh3D(vBuf, iBuf, rawIdxs.size)
        }

        fun createPerspectiveGrid(width: Float, depth: Float, lines: Int): Mesh3D {
            val rawVerts = mutableListOf<Float>()
            val rawIdxs = mutableListOf<Short>()
            val hw = width / 2f
            val hd = depth / 2f
            val y = -0.32f

            fun addGridLine(x1: Float, z1: Float, x2: Float, z2: Float, r: Float, g: Float, b: Float, a: Float) {
                val base = (rawVerts.size / 10).toShort()
                val thickness = 0.008f
                rawVerts.addAll(listOf(x1 - thickness, y, z1, 0f, 1f, 0f, r, g, b, a))
                rawVerts.addAll(listOf(x1 + thickness, y, z1, 0f, 1f, 0f, r, g, b, a))
                rawVerts.addAll(listOf(x2 + thickness, y, z2, 0f, 1f, 0f, r, g, b, a))
                rawVerts.addAll(listOf(x2 - thickness, y, z2, 0f, 1f, 0f, r, g, b, a))
                rawIdxs.addAll(listOf(base, (base + 1).toShort(), (base + 2).toShort(), base, (base + 2).toShort(), (base + 3).toShort()))
            }

            for (i in -lines..lines) {
                val ratio = i.toFloat() / lines
                val x = ratio * hw
                // Longitudinal fan lines
                val alpha = (1.0f - kotlin.math.abs(ratio) * 0.4f) * 0.45f
                addGridLine(x * 0.2f, -hd, x * 1.5f, hd, 1.0f, 0.90f, 0.0f, alpha)

                // Transverse lines
                val z = ratio * hd
                val zAlpha = (1.0f - kotlin.math.abs(ratio) * 0.5f) * 0.4f
                addGridLine(-hw, z, hw, z, 1.0f, 0.90f, 0.0f, zAlpha)
            }

            val vBuf = ByteBuffer.allocateDirect(rawVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            rawVerts.forEach { vBuf.put(it) }
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(rawIdxs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            rawIdxs.forEach { iBuf.put(it) }
            iBuf.position(0)

            return Mesh3D(vBuf, iBuf, rawIdxs.size)
        }

        fun createUnderglowQuad(radius: Float): Mesh3D {
            val rawVerts = mutableListOf<Float>()
            val rawIdxs = mutableListOf<Short>()
            val segments = 24
            val y = -0.30f

            val centerBase = 0.toShort()
            rawVerts.addAll(listOf(0f, y, 0f, 0f, 1f, 0f, 1.0f, 0.9f, 0.0f, 0.5f))

            for (i in 0..segments) {
                val rad = (i * 2 * Math.PI / segments).toFloat()
                val x = radius * cos(rad)
                val z = radius * sin(rad)
                rawVerts.addAll(listOf(x, y, z, 0f, 1f, 0f, 1.0f, 0.9f, 0.0f, 0.0f))
            }

            for (i in 1..segments) {
                rawIdxs.addAll(listOf(centerBase, i.toShort(), (i + 1).toShort()))
            }

            val vBuf = ByteBuffer.allocateDirect(rawVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            rawVerts.forEach { vBuf.put(it) }
            vBuf.position(0)

            val iBuf = ByteBuffer.allocateDirect(rawIdxs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
            rawIdxs.forEach { iBuf.put(it) }
            iBuf.position(0)

            return Mesh3D(vBuf, iBuf, rawIdxs.size)
        }
    }
}

/**
 * Complete Robo Racer 4WD 3D Car Model container.
 */
class RoboRacer3DModel {
    // Static Body Parts (Parented to chassis pitch & roll)
    val bodyMeshes = mutableListOf<Mesh3D>()

    // Independent rotating wheels (Parented to individual wheel rotation matrices)
    val wheelRL = Mesh3D.createWheel(radius = 0.32f, width = 0.16f, isLeft = true)
    val wheelRR = Mesh3D.createWheel(radius = 0.32f, width = 0.16f, isLeft = false)
    val wheelFL = Mesh3D.createWheel(radius = 0.32f, width = 0.16f, isLeft = true)
    val wheelFR = Mesh3D.createWheel(radius = 0.32f, width = 0.16f, isLeft = false)

    // Wheel positions relative to chassis center:
    val wheelRLPos = floatArrayOf(-0.64f, 0.0f, 0.50f)
    val wheelRRPos = floatArrayOf(0.64f, 0.0f, 0.50f)
    val wheelFLPos = floatArrayOf(-0.64f, 0.0f, -0.50f)
    val wheelFRPos = floatArrayOf(0.64f, 0.0f, -0.50f)

    // Ground Environment
    val groundGrid = Mesh3D.createPerspectiveGrid(width = 8.0f, depth = 8.0f, lines = 16)
    val underglowQuad = Mesh3D.createUnderglowQuad(radius = 1.1f)

    init {
        buildRobotCarChassis()
    }

    private fun buildRobotCarChassis() {
        // 1. Lower Acrylic Chassis Plate (Yellow transparent acrylic)
        bodyMeshes.add(
            Mesh3D.createBox(
                w = 1.05f, h = 0.03f, d = 1.35f,
                r = 0.95f, g = 0.78f, b = 0.0f, a = 0.85f,
                tx = 0.0f, ty = -0.05f, tz = 0.0f
            )
        )

        // 2. Brass Standoff Pillars (4x Golden cylinders)
        val standoffInsetX = 0.42f
        val standoffInsetZ = 0.52f
        val standoffH = 0.28f
        val standoffY = 0.10f
        for (sx in listOf(-standoffInsetX, standoffInsetX)) {
            for (sz in listOf(-standoffInsetZ, standoffInsetZ)) {
                bodyMeshes.add(
                    Mesh3D.createCylinder(
                        radius = 0.024f, height = standoffH,
                        r = 1.0f, g = 0.84f, b = 0.0f, a = 1.0f,
                        tx = sx, ty = standoffY, tz = sz
                    )
                )
            }
        }

        // 3. Upper Acrylic Deck Plate (Transparent neon yellow)
        bodyMeshes.add(
            Mesh3D.createBox(
                w = 1.05f, h = 0.03f, d = 1.35f,
                r = 1.0f, g = 0.88f, b = 0.1f, a = 0.90f,
                tx = 0.0f, ty = 0.25f, tz = 0.0f
            )
        )

        // 4. TT Gear Motor Housings (4x Yellow DC gear motors under deck)
        val motorW = 0.14f; val motorH = 0.18f; val motorD = 0.32f
        bodyMeshes.add(Mesh3D.createBox(motorW, motorH, motorD, 1.0f, 0.70f, 0.0f, 1f, tx = -0.46f, ty = -0.01f, tz = 0.50f))
        bodyMeshes.add(Mesh3D.createBox(motorW, motorH, motorD, 1.0f, 0.70f, 0.0f, 1f, tx = 0.46f, ty = -0.01f, tz = 0.50f))
        bodyMeshes.add(Mesh3D.createBox(motorW, motorH, motorD, 1.0f, 0.70f, 0.0f, 1f, tx = -0.46f, ty = -0.01f, tz = -0.50f))
        bodyMeshes.add(Mesh3D.createBox(motorW, motorH, motorD, 1.0f, 0.70f, 0.0f, 1f, tx = 0.46f, ty = -0.01f, tz = -0.50f))

        // 5. Dual Tayal L298N Motor Drivers with Red Aluminum Fin Heatsinks (Rear Top Deck)
        for (dx in listOf(-0.24f, 0.24f)) {
            // Red PCB
            bodyMeshes.add(Mesh3D.createBox(0.32f, 0.02f, 0.34f, 0.72f, 0.11f, 0.11f, 1f, tx = dx, ty = 0.27f, tz = 0.35f))
            // Red Aluminum Heatsink Base
            bodyMeshes.add(Mesh3D.createBox(0.20f, 0.14f, 0.16f, 0.88f, 0.18f, 0.18f, 1f, tx = dx, ty = 0.35f, tz = 0.35f))
            // Green screw terminal blocks
            bodyMeshes.add(Mesh3D.createBox(0.12f, 0.08f, 0.06f, 0.18f, 0.55f, 0.20f, 1f, tx = dx - 0.08f, ty = 0.31f, tz = 0.48f))
            bodyMeshes.add(Mesh3D.createBox(0.12f, 0.08f, 0.06f, 0.18f, 0.55f, 0.20f, 1f, tx = dx + 0.08f, ty = 0.31f, tz = 0.48f))
        }

        // 6. amiciSolar XL6019 Buck-Boost Converter (Center Top Deck)
        // Blue PCB
        bodyMeshes.add(Mesh3D.createBox(0.40f, 0.02f, 0.24f, 0.05f, 0.28f, 0.63f, 1f, tx = 0.0f, ty = 0.27f, tz = 0.02f))
        // Copper Toroidal Inductor Ring
        bodyMeshes.add(Mesh3D.createCylinder(0.06f, 0.08f, 0.72f, 0.45f, 0.20f, 1f, tx = -0.10f, ty = 0.32f, tz = 0.02f))
        // Blue Trimmer Potentiometer with Gold Tuning Screw
        bodyMeshes.add(Mesh3D.createBox(0.08f, 0.10f, 0.08f, 0.10f, 0.46f, 0.82f, 1f, tx = 0.10f, ty = 0.32f, tz = 0.02f))
        bodyMeshes.add(Mesh3D.createCylinder(0.016f, 0.02f, 1.0f, 0.84f, 0.0f, 1f, tx = 0.10f, ty = 0.38f, tz = 0.02f))

        // 7. Prakti ESP32 30-Pin Dev Board (Forward Top Deck)
        // Black PCB
        bodyMeshes.add(Mesh3D.createBox(0.52f, 0.02f, 0.28f, 0.10f, 0.10f, 0.10f, 1f, tx = 0.0f, ty = 0.27f, tz = -0.38f))
        // Silver Metal ESP-WROOM-32 RF Shield
        bodyMeshes.add(Mesh3D.createBox(0.24f, 0.04f, 0.20f, 0.88f, 0.88f, 0.90f, 1f, tx = -0.06f, ty = 0.30f, tz = -0.38f))
        // CP2102 USB-to-UART chip (Small black square)
        bodyMeshes.add(Mesh3D.createBox(0.07f, 0.02f, 0.07f, 0.02f, 0.02f, 0.02f, 1f, tx = 0.14f, ty = 0.29f, tz = -0.38f))
        // Micro-USB connector (Silver jack)
        bodyMeshes.add(Mesh3D.createBox(0.06f, 0.04f, 0.09f, 0.75f, 0.75f, 0.78f, 1f, tx = 0.25f, ty = 0.29f, tz = -0.38f))
        // 2x 15-pin Header Rows (Gold header pins)
        bodyMeshes.add(Mesh3D.createBox(0.50f, 0.05f, 0.02f, 1.0f, 0.84f, 0.0f, 1f, tx = 0.0f, ty = 0.30f, tz = -0.26f))
        bodyMeshes.add(Mesh3D.createBox(0.50f, 0.05f, 0.02f, 1.0f, 0.84f, 0.0f, 1f, tx = 0.0f, ty = 0.30f, tz = -0.50f))

        // 8. Rear LED Tail Lights
        bodyMeshes.add(Mesh3D.createBox(0.18f, 0.04f, 0.02f, 1.0f, 0.10f, 0.10f, 1f, tx = -0.30f, ty = 0.25f, tz = 0.68f))
        bodyMeshes.add(Mesh3D.createBox(0.18f, 0.04f, 0.02f, 1.0f, 0.10f, 0.10f, 1f, tx = 0.30f, ty = 0.25f, tz = 0.68f))
    }
}
