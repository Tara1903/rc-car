package com.example.roboracer

import com.example.roboracer.control.RawTouchInput
import com.example.roboracer.control.SafetyInterlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyInterlockTest {

    @Test
    fun testBrakePriorityOverAccelerate() {
        val interlock = SafetyInterlock()
        // Both Brake and Accelerate pressed simultaneously
        val input = RawTouchInput(
            isAccelPressed = true,
            isBrakePressed = true
        )

        val resolved = interlock.resolve(input, currentSpeed = 100, currentTimeMs = 1000L)

        // Safety policy: Brake wins unconditionally; throttle must NOT be positive
        assertTrue(resolved.isBrakeEngaged)
        assertTrue(resolved.normalizedThrottle <= 0.0f)
    }

    @Test
    fun testSteeringCancellationOnDualTouch() {
        val interlock = SafetyInterlock()
        // Both Left and Right steering pressed simultaneously
        val input = RawTouchInput(
            isLeftSteerPressed = true,
            isRightSteerPressed = true
        )

        val resolved = interlock.resolve(input, currentSpeed = 0, currentTimeMs = 1000L)
        // Must resolve to neutral steering (0.0)
        assertEquals(0.0f, resolved.normalizedSteering, 0.001f)
    }

    @Test
    fun testLeftPlusBrakeProducesBrakeEngaged() {
        val interlock = SafetyInterlock()
        val input = RawTouchInput(
            isLeftSteerPressed = true,
            isBrakePressed = true
        )

        val resolved = interlock.resolve(input, currentSpeed = 50, currentTimeMs = 1000L)
        assertTrue(resolved.isBrakeEngaged)
        assertEquals(0.0f, resolved.normalizedThrottle, 0.001f)
        assertEquals(-1.0f, resolved.normalizedSteering, 0.001f)
    }

    @Test
    fun testRightPlusBrakeProducesBrakeEngaged() {
        val interlock = SafetyInterlock()
        val input = RawTouchInput(
            isRightSteerPressed = true,
            isBrakePressed = true
        )

        val resolved = interlock.resolve(input, currentSpeed = 50, currentTimeMs = 1000L)
        assertTrue(resolved.isBrakeEngaged)
        assertEquals(0.0f, resolved.normalizedThrottle, 0.001f)
        assertEquals(1.0f, resolved.normalizedSteering, 0.001f)
    }

    @Test
    fun testAllFourZonesPressedSimultaneously() {
        val interlock = SafetyInterlock()
        // Full conflict chaos: all 4 zones pressed
        val input = RawTouchInput(
            isLeftSteerPressed = true,
            isRightSteerPressed = true,
            isAccelPressed = true,
            isBrakePressed = true
        )

        val resolved = interlock.resolve(input, currentSpeed = 80, currentTimeMs = 1000L)
        // Brake overrides Accel -> throttle <= 0, brake engaged
        assertTrue(resolved.isBrakeEngaged)
        assertEquals(0.0f, resolved.normalizedThrottle, 0.001f)
        // Dual steering cancels -> neutral
        assertEquals(0.0f, resolved.normalizedSteering, 0.001f)
    }

    @Test
    fun testAllZonesReleasedReturnsToIdle() {
        val interlock = SafetyInterlock()
        val input = RawTouchInput()

        val resolved = interlock.resolve(input, currentSpeed = 0, currentTimeMs = 1000L)
        assertFalse(resolved.isBrakeEngaged)
        assertFalse(resolved.isReverseReady)
        assertEquals(0.0f, resolved.normalizedThrottle, 0.001f)
        assertEquals(0.0f, resolved.normalizedSteering, 0.001f)
    }

    @Test
    fun testForwardToReverseInterlockDelay() {
        val interlock = SafetyInterlock()

        // 1. Driving forward
        val forwardInput = RawTouchInput(isAccelPressed = true)
        val fwdResolved = interlock.resolve(forwardInput, currentSpeed = 150, currentTimeMs = 1000L)
        assertEquals(1.0f, fwdResolved.normalizedThrottle, 0.001f)

        // 2. Press Brake while still moving forward (speed = 100)
        val brakeInput = RawTouchInput(isBrakePressed = true)
        val brake1 = interlock.resolve(brakeInput, currentSpeed = 100, currentTimeMs = 1200L)
        assertEquals(0.0f, brake1.normalizedThrottle, 0.001f)
        assertFalse(brake1.isReverseReady)

        // 3. Car comes to a stop (speed = 0) at t = 1500L
        val brakeStopped = interlock.resolve(brakeInput, currentSpeed = 0, currentTimeMs = 1500L)
        assertEquals(0.0f, brakeStopped.normalizedThrottle, 0.001f)
        assertFalse(brakeStopped.isReverseReady)

        // 4. Holding Brake for 300ms (t = 1800L, delay < 500ms)
        val brakeDwell = interlock.resolve(brakeInput, currentSpeed = 0, currentTimeMs = 1800L)
        assertEquals(0.0f, brakeDwell.normalizedThrottle, 0.001f)
        assertFalse(brakeDwell.isReverseReady)

        // 5. Holding Brake for 550ms (t = 2050L, delay >= 500ms)
        val reverseEngaged = interlock.resolve(brakeInput, currentSpeed = 0, currentTimeMs = 2050L)
        assertTrue(reverseEngaged.isReverseReady)
        // Reverse speed capped at -0.50f (50%)
        assertEquals(-0.50f, reverseEngaged.normalizedThrottle, 0.001f)
    }
}
