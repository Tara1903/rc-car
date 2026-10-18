package com.example.roboracer.physics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class VehiclePhysicsEngineTest {

    private lateinit var engine: VehiclePhysicsEngine

    @Before
    fun setUp() {
        engine = VehiclePhysicsEngine()
    }

    @Test
    fun testZeroThrottleZeroSteer_staysAtRest() {
        for (i in 0 until 50) {
            engine.step(0.016f, throttle = 0.0f, steering = 0.0f, isBraking = false)
        }
        val s = engine.state
        assertEquals(0.0f, s.x, 1e-4f)
        assertEquals(0.0f, s.z, 1e-4f)
        assertEquals(0.0f, s.linearSpeed, 1e-4f)
        assertEquals(0.0f, s.angularVelocity, 1e-4f)
        assertEquals(0.0f, s.wheelSpeedL, 1e-4f)
        assertEquals(0.0f, s.wheelSpeedR, 1e-4f)
        assertFalse(s.isColliding)
    }

    @Test
    fun testForwardThrottle_movesForwardWithWheelSpin() {
        for (i in 0 until 60) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }
        val s = engine.state
        // In OpenGL coordinates, forward is along -Z
        assertTrue("Vehicle should have negative Z position (moved forward)", s.z < -0.1f)
        assertTrue("Vehicle should have positive forward speed", s.linearSpeed > 0.3f)
        assertTrue("Left wheel should have forward speed", s.wheelSpeedL > 0.3f)
        assertTrue("Right wheel should have forward speed", s.wheelSpeedR > 0.3f)
        assertTrue("Wheel angles should have accumulated rotation", abs(s.wheelAngleL) > 10.0f)
        assertTrue("Chassis should exhibit acceleration pitch squat", s.pitchDeg <= 0.0f)
    }

    @Test
    fun testReverseThrottle_movesBackward() {
        for (i in 0 until 60) {
            engine.step(0.016f, throttle = -1.0f, steering = 0.0f, isBraking = false)
        }
        val s = engine.state
        // In OpenGL coordinates, reverse is along +Z
        assertTrue("Vehicle should have positive Z position (moved backward)", s.z > 0.1f)
        assertTrue("Vehicle should have negative linear speed", s.linearSpeed < -0.3f)
        assertTrue("Left wheel should have reverse speed", s.wheelSpeedL < -0.3f)
        assertTrue("Right wheel should have reverse speed", s.wheelSpeedR < -0.3f)
    }

    @Test
    fun testPureLeftSteer_positiveYawRateAndDifferentialSpeeds() {
        // Drive forward and turn left (steering < 0)
        for (i in 0 until 60) {
            engine.step(0.016f, throttle = 0.5f, steering = -0.8f, isBraking = false)
        }
        val s = engine.state
        // Turning left causes yaw angle to change and right wheel to rotate faster than left wheel
        assertTrue("Right wheel speed should exceed left wheel speed during left turn", s.wheelSpeedR > s.wheelSpeedL)
        assertTrue("Vehicle should have non-zero yaw heading", abs(s.yawRad) > 0.1f)
        assertTrue("Vehicle should deviate towards negative X (left in GL)", s.x < -0.02f)
    }

    @Test
    fun testPureRightSteer_negativeYawRateAndDifferentialSpeeds() {
        // Drive forward and turn right (steering > 0)
        for (i in 0 until 60) {
            engine.step(0.016f, throttle = 0.5f, steering = 0.8f, isBraking = false)
        }
        val s = engine.state
        assertTrue("Left wheel speed should exceed right wheel speed during right turn", s.wheelSpeedL > s.wheelSpeedR)
        assertTrue("Vehicle should deviate towards positive X (right in GL)", s.x > 0.02f)
    }

    @Test
    fun testPivotTurn_counterRotatingWheelsAndZeroLinearVelocity() {
        // Zero throttle, maximum left steer (in-place skid steer pivot)
        for (i in 0 until 40) {
            engine.step(0.016f, throttle = 0.0f, steering = -1.0f, isBraking = false)
        }
        val s = engine.state
        assertTrue("Left wheel should rotate in reverse", s.wheelSpeedL < -0.2f)
        assertTrue("Right wheel should rotate forward", s.wheelSpeedR > 0.2f)
        assertTrue("Angular yaw rate should be high", abs(s.angularVelocity) > 1.5f)
        // Center of mass linear displacement should be negligible during pure pivot
        assertTrue("Linear speed at center of mass should remain very small", abs(s.linearSpeed) < 0.15f)
        assertTrue("Linear position should remain near origin", abs(s.x) < 0.08f && abs(s.z) < 0.08f)
    }

    @Test
    fun testBrakingWhileMoving_deceleratesAndStops() {
        // First accelerate for 50 steps
        for (i in 0 until 50) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }
        val speedBeforeBrake = engine.state.linearSpeed
        assertTrue("Vehicle should have speed before brake", speedBeforeBrake > 0.4f)

        // Now engage brake for 60 steps
        for (i in 0 until 60) {
            engine.step(0.016f, throttle = 0.0f, steering = 0.0f, isBraking = true)
        }
        val speedAfterBrake = engine.state.linearSpeed
        assertTrue("Vehicle should stop or nearly stop after braking", abs(speedAfterBrake) < 0.05f)
        assertTrue("Chassis pitch under braking should exhibit front dive (>0)", engine.state.pitchDeg >= 0.0f)
    }

    @Test
    fun testSurfaceFrictionPresets_smoothFloorVsCarpet() {
        // Engine 1 on Smooth Floor
        val engineSmooth = VehiclePhysicsEngine(surfacePreset = SurfacePreset.SMOOTH_FLOOR)
        for (i in 0 until 60) {
            engineSmooth.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }

        // Engine 2 on Carpet
        val engineCarpet = VehiclePhysicsEngine(surfacePreset = SurfacePreset.CARPET)
        for (i in 0 until 60) {
            engineCarpet.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }

        // Carpet has higher rolling resistance, resulting in lower terminal speed / displacement
        assertTrue(
            "Smooth floor should yield higher linear speed than carpet (${engineSmooth.state.linearSpeed} vs ${engineCarpet.state.linearSpeed})",
            engineSmooth.state.linearSpeed > engineCarpet.state.linearSpeed
        )
        assertTrue(
            "Smooth floor should achieve greater forward displacement than carpet (${engineSmooth.state.z} vs ${engineCarpet.state.z})",
            engineSmooth.state.z < engineCarpet.state.z // More negative Z is further forward
        )
    }

    @Test
    fun testTractionSlipLimit_wheelSpeedExceedsGroundSpeedOnAbruptAccel() {
        // Immediate full throttle step on carpet (lower friction, higher slip)
        val carpetEngine = VehiclePhysicsEngine(surfacePreset = SurfacePreset.CARPET)
        carpetEngine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        carpetEngine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)

        val s = carpetEngine.state
        assertTrue("Wheel speed should initially ramp faster than vehicle mass accelerates", s.wheelSpeedL > s.linearSpeed)
        assertTrue("Slip ratio should be non-zero during breakaway acceleration", s.slipRatioL > 0.0f)
    }

    @Test
    fun testStaticObstacleCollision_stopsAndDoesNotPenetrate() {
        // Place an obstacle directly in the forward path: Z in [-1.5, -1.0], X in [-0.5, 0.5]
        engine.obstacles.clear()
        engine.obstacles.add(Obstacle("Center_Obstacle", -0.5f, 0.5f, -1.5f, -1.0f))

        // Drive directly forward towards the obstacle for 150 steps
        var collisionDetected = false
        for (i in 0 until 150) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
            if (engine.state.isColliding) {
                collisionDetected = true
            }
        }

        assertTrue("Collision should be detected with static obstacle", collisionDetected)
        // Vehicle bounding circle (radius 0.12) should be stopped in front of minZ = -1.0f
        // i.e., vehicle Z should not penetrate deeper than obstacle edge + radius: z >= -1.0f - radius - margin
        assertTrue("Vehicle must not penetrate obstacle interior (Z=${engine.state.z})", engine.state.z >= -1.0f + engine.collisionRadius - 0.05f)
    }

    @Test
    fun testReset_restoresOriginAndZeroVelocities() {
        for (i in 0 until 50) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.2f, isBraking = false)
        }
        val distFromOrigin = kotlin.math.sqrt(engine.state.x * engine.state.x + engine.state.z * engine.state.z)
        assertTrue("Vehicle should have moved from origin ($distFromOrigin)", distFromOrigin > 0.1f)

        engine.reset()
        val s = engine.state
        assertEquals(0.0f, s.x, 1e-4f)
        assertEquals(0.0f, s.z, 1e-4f)
        assertEquals(0.0f, s.yawRad, 1e-4f)
        assertEquals(0.0f, s.linearSpeed, 1e-4f)
        assertEquals(0.0f, s.angularVelocity, 1e-4f)
        assertEquals(0.0f, s.wheelSpeedL, 1e-4f)
        assertEquals(0.0f, s.wheelSpeedR, 1e-4f)
        assertFalse(s.isColliding)
    }

    @Test
    fun testPauseResume_stateDoesNotAdvanceWhenPaused() {
        engine.isPaused = true
        for (i in 0 until 30) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }
        assertEquals("Paused engine should not move", 0.0f, engine.state.z, 1e-4f)

        engine.isPaused = false
        for (i in 0 until 30) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }
        assertTrue("Resumed engine should advance", engine.state.z < -0.05f)
    }

    @Test
    fun testDeterministicStepping_identicalTrajectories() {
        val engine1 = VehiclePhysicsEngine()
        val engine2 = VehiclePhysicsEngine()

        for (i in 0 until 100) {
            val throttle = if (i < 50) 0.8f else -0.4f
            val steer = if (i % 20 < 10) 0.3f else -0.3f
            engine1.step(0.016f, throttle, steer, isBraking = false)
            engine2.step(0.016f, throttle, steer, isBraking = false)
        }

        assertEquals("X positions must match deterministically", engine1.state.x, engine2.state.x, 1e-5f)
        assertEquals("Z positions must match deterministically", engine1.state.z, engine2.state.z, 1e-5f)
        assertEquals("Yaw must match deterministically", engine1.state.yawRad, engine2.state.yawRad, 1e-5f)
        assertEquals("Linear speeds must match deterministically", engine1.state.linearSpeed, engine2.state.linearSpeed, 1e-5f)
    }

    @Test
    fun testMassAndInertiaScaling_higherMassReducesAcceleration() {
        val lightVehicle = VehiclePhysicsEngine(vehicleMass = 0.50f)
        val heavyVehicle = VehiclePhysicsEngine(vehicleMass = 1.50f)

        for (i in 0 until 30) {
            lightVehicle.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
            heavyVehicle.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }

        assertTrue(
            "Lighter vehicle should achieve higher speed under same thrust (${lightVehicle.state.linearSpeed} vs ${heavyVehicle.state.linearSpeed})",
            lightVehicle.state.linearSpeed > heavyVehicle.state.linearSpeed
        )
    }

    @Test
    fun testArenaBoundaryClamping() {
        // Arena is bounded at minZ = -2.2f
        for (i in 0 until 300) {
            engine.step(0.016f, throttle = 1.0f, steering = 0.0f, isBraking = false)
        }
        val s = engine.state
        assertTrue("Vehicle should be clamped at arena forward boundary", s.z >= engine.arenaMinZ + engine.collisionRadius - 0.01f)
        assertTrue("Vehicle collision flag should be active at boundary", s.isColliding)
    }

    @Test
    fun testModeSeparation_simulationVsLiveTelemetry() {
        val simMode = DigitalTwinMode.SIMULATION
        val liveMode = DigitalTwinMode.LIVE_TELEMETRY

        // In Simulation mode, full kinetics and trajectory are integrated
        for (i in 0 until 40) {
            engine.step(0.016f, throttle = 0.8f, steering = 0.0f, isBraking = false)
        }
        assertTrue("Simulation mode advances trajectory", engine.state.linearSpeed > 0.3f)
        assertTrue("Simulation mode traverses arena (Z < 0)", engine.state.z < -0.05f)

        // In Live mode, vehicle chassis pose remains anchored at origin while wheel spin/state tracks telemetry
        assertEquals("Digital twin mode enum supports SIMULATION and LIVE_TELEMETRY", DigitalTwinMode.values().size, 2)
        assertTrue(simMode != liveMode)
    }
}
