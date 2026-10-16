package com.example.roboracer

import com.example.roboracer.tools.GlbModelBuilder
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GlbGeneratorTest {

    @Test
    fun generateAndValidateRobotCarGlb() {
        val glbBytes = GlbModelBuilder.buildRobotCarGlb()
        assertTrue("GLB file size must be > 1000 bytes", glbBytes.size > 1000)

        // Verify GLB Header magic
        val buf = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.getInt()
        val version = buf.getInt()
        val length = buf.getInt()

        org.junit.Assert.assertEquals(0x46546C67, magic) // "glTF"
        org.junit.Assert.assertEquals(2, version)
        org.junit.Assert.assertEquals(glbBytes.size, length)

        // Write to assets/models directory
        val assetsDir = File("src/main/assets/models")
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }
        val targetFile = File(assetsDir, "robo_racer_car.glb")
        targetFile.writeBytes(glbBytes)

        assertTrue("Asset file must exist", targetFile.exists())
        println("Generated verified GLB asset at: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
    }
}
