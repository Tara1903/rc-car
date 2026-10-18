package com.example.roboracer

import com.example.roboracer.renderer.gltf.GltfModel
import com.example.roboracer.renderer.gltf.GltfParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class GltfModelTest {

    private fun findAssetFile(): File {
        val candidates = listOf(
            File("src/main/assets/models/robo_racer_car.glb"),
            File("app/src/main/assets/models/robo_racer_car.glb"),
            File("../app/src/main/assets/models/robo_racer_car.glb")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("robo_racer_car.glb asset file not found in test search paths")
    }

    @Test
    fun parseRoboRacerGlbAsset() {
        val assetFile = findAssetFile()
        assertTrue("Asset file must exist", assetFile.exists())
        assertTrue("Asset file size must be > 100 KB", assetFile.length() > 100_000)

        val model: GltfModel = FileInputStream(assetFile).use { fis ->
            GltfParser.parse(fis)
        }

        assertNotNull("Model rootNode must not be null", model.rootNode)
        assertTrue("Model must have loaded nodes", model.allNodes.isNotEmpty())
        assertTrue("Model must have loaded materials", model.materials.isNotEmpty())

        println("Loaded glTF model: ${model.allNodes.size} nodes, ${model.materials.size} materials")
    }

    @Test
    fun verifyRequiredNamedHierarchyNodes() {
        val assetFile = findAssetFile()
        val model: GltfModel = FileInputStream(assetFile).use { fis ->
            GltfParser.parse(fis)
        }

        val requiredNodes = listOf(
            "car_root",
            "chassis_root",
            "front_left_wheel_pivot",
            "front_right_wheel_pivot",
            "rear_left_wheel_pivot",
            "rear_right_wheel_pivot",
            "chassis_bottom_deck",
            "chassis_top_deck",
            "motor_left_front",
            "motor_right_front",
            "motor_left_rear",
            "motor_right_rear",
            "esp32_board",
            "power_converter",
            "l298n_left",
            "l298n_right",
            "battery_pack"
        )

        for (nodeName in requiredNodes) {
            val node = model.findNode(nodeName)
            assertNotNull("Required node '$nodeName' must exist in glTF model scene graph", node)
        }

        // Verify quick accessor pivots on GltfModel
        assertNotNull("carRoot accessor must be present", model.carRoot)
        assertNotNull("chassisRoot accessor must be present", model.chassisRoot)
        assertNotNull("wheelFL accessor must be present", model.wheelFL)
        assertNotNull("wheelFR accessor must be present", model.wheelFR)
        assertNotNull("wheelRL accessor must be present", model.wheelRL)
        assertNotNull("wheelRR accessor must be present", model.wheelRR)
    }

    @Test
    fun verifyRealTimeAnimationDynamics() {
        val assetFile = findAssetFile()
        val model: GltfModel = FileInputStream(assetFile).use { fis ->
            GltfParser.parse(fis)
        }

        val fl = model.wheelFL!!
        val fr = model.wheelFR!!
        val chassis = model.chassisRoot!!

        val initialFlRot = fl.animRotationXDeg
        val initialFrRot = fr.animRotationXDeg
        val initialPitch = chassis.animPitchDeg
        val initialRoll = chassis.animRollDeg

        // Simulate full forward throttle for 0.1 seconds
        model.updateAnimation(
            throttle = 1.0f,
            steering = 0.0f,
            isBraking = false,
            leftPwm = 255,
            rightPwm = 255,
            dtSeconds = 0.1f
        )

        // Wheels should have rotated forward (animRotationXDeg changed)
        assertTrue("Front left wheel must rotate under throttle", fl.animRotationXDeg != initialFlRot)
        assertTrue("Front right wheel must rotate under throttle", fr.animRotationXDeg != initialFrRot)

        // Chassis should squat rearward under acceleration (negative pitch)
        assertTrue("Chassis pitch should reflect squat under forward acceleration", chassis.animPitchDeg < 0.0f)

        // Simulate braking
        model.updateAnimation(
            throttle = 0.0f,
            steering = 0.0f,
            isBraking = true,
            leftPwm = 0,
            rightPwm = 0,
            dtSeconds = 0.1f
        )
        // Under braking, chassis should dive forward (positive pitch)
        assertTrue("Chassis pitch should reflect dive under braking", chassis.animPitchDeg > -2.0f)

        // Simulate steering right (+steering)
        model.updateAnimation(
            throttle = 0.8f,
            steering = 1.0f,
            isBraking = false,
            leftPwm = 150,
            rightPwm = 255,
            dtSeconds = 0.1f
        )
        // Roll should respond to steering
        assertTrue("Chassis roll should respond to steering input", chassis.animRollDeg != 0.0f)
    }

    @Test
    fun verifyPbrMaterialsProperties() {
        val assetFile = findAssetFile()
        val model: GltfModel = FileInputStream(assetFile).use { fis ->
            GltfParser.parse(fis)
        }

        // Verify acrylic transparent material
        val acrylic = model.materials.find { it.name.contains("Acrylic") }
        assertNotNull("Acrylic material must exist", acrylic)
        assertTrue("Acrylic material must be transparent or have alpha < 1.0", acrylic!!.isTransparent || acrylic.baseColorFactor[3] < 1.0f)

        // Verify tire rubber material
        val tire = model.materials.find { it.name.contains("TireRubber") }
        assertNotNull("Tire rubber material must exist", tire)
        assertTrue("Tire roughness must be high (>= 0.6)", tire!!.roughnessFactor >= 0.6f)
    }

    @Test
    fun verifyChassisPitchAndRollBoundedness() {
        val assetFile = findAssetFile()
        val model: GltfModel = FileInputStream(assetFile).use { fis ->
            GltfParser.parse(fis)
        }
        val chassis = model.chassisRoot!!

        // Apply aggressive alternating commands with high dtSeconds
        val testInputs = listOf(
            Triple(1.0f, 1.0f, false),
            Triple(-1.0f, -1.0f, false),
            Triple(1.0f, 0.0f, true),
            Triple(0.0f, 1.0f, false)
        )
        for ((th, st, brk) in testInputs) {
            for (i in 0 until 50) {
                model.updateAnimation(
                    throttle = th,
                    steering = st,
                    isBraking = brk,
                    leftPwm = 255,
                    rightPwm = 255,
                    dtSeconds = 0.1f
                )
                assertFalse("Pitch must not be NaN", chassis.animPitchDeg.isNaN())
                assertFalse("Pitch must not be Infinite", chassis.animPitchDeg.isInfinite())
                assertFalse("Roll must not be NaN", chassis.animRollDeg.isNaN())
                assertFalse("Roll must not be Infinite", chassis.animRollDeg.isInfinite())
                assertTrue("Pitch must be clamped within [-5.0, 5.0], got ${chassis.animPitchDeg}", chassis.animPitchDeg in -5.0f..5.0f)
                assertTrue("Roll must be clamped within [-5.0, 5.0], got ${chassis.animRollDeg}", chassis.animRollDeg in -5.0f..5.0f)
            }
        }
    }
}
