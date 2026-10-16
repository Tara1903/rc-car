package com.example.roboracer

import com.example.roboracer.bluetooth.protocol.MotorDir
import com.example.roboracer.control.DifferentialDriveMixer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifferentialDriveTest {

    @Test
    fun testStraightForwardThrottle() {
        val mixer = DifferentialDriveMixer()
        val cmd = mixer.mix(throttle = 1.0f, steering = 0.0f)

        assertEquals(MotorDir.FORWARD, cmd.leftDir)
        assertEquals(MotorDir.FORWARD, cmd.rightDir)
        assertEquals(255, cmd.leftSpeed)
        assertEquals(255, cmd.rightSpeed)
    }

    @Test
    fun testStraightReverseThrottle() {
        val mixer = DifferentialDriveMixer()
        val cmd = mixer.mix(throttle = -0.5f, steering = 0.0f)

        assertEquals(MotorDir.REVERSE, cmd.leftDir)
        assertEquals(MotorDir.REVERSE, cmd.rightDir)
        assertEquals(127, cmd.leftSpeed)
        assertEquals(127, cmd.rightSpeed)
    }

    @Test
    fun testSpinInPlaceLeftWhenNotBraking() {
        val mixer = DifferentialDriveMixer()
        // Zero throttle, full left steering (-1.0f), isBraking = false
        val cmd = mixer.mix(throttle = 0.0f, steering = -1.0f, isBraking = false)

        // Spin in place left: left motor reverses, right motor moves forward
        assertEquals(MotorDir.REVERSE, cmd.leftDir)
        assertEquals(MotorDir.FORWARD, cmd.rightDir)
        assertTrue(cmd.leftSpeed > 100)
        assertTrue(cmd.rightSpeed > 100)
    }

    @Test
    fun testSpinInPlaceRightWhenNotBraking() {
        val mixer = DifferentialDriveMixer()
        // Zero throttle, full right steering (+1.0f), isBraking = false
        val cmd = mixer.mix(throttle = 0.0f, steering = 1.0f, isBraking = false)

        // Spin in place right: left motor moves forward, right motor reverses
        assertEquals(MotorDir.FORWARD, cmd.leftDir)
        assertEquals(MotorDir.REVERSE, cmd.rightDir)
        assertTrue(cmd.leftSpeed > 100)
        assertTrue(cmd.rightSpeed > 100)
    }

    @Test
    fun testBrakePriorityBlocksSpinInPlaceLeft() {
        val mixer = DifferentialDriveMixer()
        // Critical safety test: brake is held while steering left
        val cmd = mixer.mix(throttle = 0.0f, steering = -1.0f, isBraking = true)

        // Must remain completely stopped, NOT spin in place
        assertEquals(MotorDir.STOP, cmd.leftDir)
        assertEquals(MotorDir.STOP, cmd.rightDir)
        assertEquals(0, cmd.leftSpeed)
        assertEquals(0, cmd.rightSpeed)
    }

    @Test
    fun testBrakePriorityBlocksSpinInPlaceRight() {
        val mixer = DifferentialDriveMixer()
        // Critical safety test: brake is held while steering right
        val cmd = mixer.mix(throttle = 0.0f, steering = 1.0f, isBraking = true)

        // Must remain completely stopped, NOT spin in place
        assertEquals(MotorDir.STOP, cmd.leftDir)
        assertEquals(MotorDir.STOP, cmd.rightDir)
        assertEquals(0, cmd.leftSpeed)
        assertEquals(0, cmd.rightSpeed)
    }

    @Test
    fun testReverseSteeringCurvature() {
        val mixer = DifferentialDriveMixer()
        // Reverse engaged with slight steering
        val cmd = mixer.mix(throttle = -0.5f, steering = -0.5f, isBraking = true)

        assertEquals(MotorDir.REVERSE, cmd.leftDir)
        assertEquals(MotorDir.REVERSE, cmd.rightDir)
        assertTrue(cmd.leftSpeed > 0)
        assertTrue(cmd.rightSpeed > 0)
    }
}
