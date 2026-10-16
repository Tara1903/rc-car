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

        // Write to build directory to avoid mutating tracked git repository assets during normal test runs
        val outputDir = File("build/outputs/test_models")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val targetFile = File(outputDir, "robo_racer_car_test.glb")
        targetFile.writeBytes(glbBytes)

        assertTrue("Generated test GLB file must exist", targetFile.exists())
        println("Generated verified GLB asset at: ${targetFile.absolutePath} (${targetFile.length()} bytes)")

        // Validate checked-in asset file if present
        val checkedInAsset = File("src/main/assets/models/robo_racer_car.glb")
        if (checkedInAsset.exists()) {
            val assetBytes = checkedInAsset.readBytes()
            assertTrue("Checked-in GLB file size must be > 1000 bytes", assetBytes.size > 1000)
            val assetBuf = ByteBuffer.wrap(assetBytes).order(ByteOrder.LITTLE_ENDIAN)
            org.junit.Assert.assertEquals(0x46546C67, assetBuf.getInt())
            org.junit.Assert.assertEquals(2, assetBuf.getInt())
        }
    }
}
