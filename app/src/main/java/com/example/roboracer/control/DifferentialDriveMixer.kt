package com.example.roboracer.control

import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.MotorDir
import kotlin.math.abs

class DifferentialDriveMixer(
    var steeringSensitivity: Float = 0.75f,
    var maxSpeedLimit: Float = 1.0f,
    var trimOffset: Float = 0.0f // -0.2 (more left) to +0.2 (more right)
) {
    fun mix(
        throttle: Float,      // -1.0f (rev) to +1.0f (fwd)
        steering: Float,      // -1.0f (left) to +1.0f (right)
        sequence: Int = 0,
        isBraking: Boolean = false
    ): CommandFrame {
        val t = throttle.coerceIn(-1.0f, 1.0f) * maxSpeedLimit
        val s = steering.coerceIn(-1.0f, 1.0f) * steeringSensitivity

        // Arcade / Differential drive mixing
        var leftRaw: Float
        var rightRaw: Float

        if (abs(t) < 0.05f) {
            if (isBraking) {
                // Active braking priority: steering must NOT cause spin-in-place while brake is held
                leftRaw = 0.0f
                rightRaw = 0.0f
            } else {
                // Spin-in-place (zero throttle, purely turning without brake)
                leftRaw = s
                rightRaw = -s
            }
        } else {
            // Driving curve
            leftRaw = t + (s * abs(t))
            rightRaw = t - (s * abs(t))
        }

        // Apply trim
        if (trimOffset > 0) {
            leftRaw *= (1.0f - trimOffset)
        } else if (trimOffset < 0) {
            rightRaw *= (1.0f + trimOffset)
        }

        // Clamp to [-1.0, 1.0]
        leftRaw = leftRaw.coerceIn(-1.0f, 1.0f)
        rightRaw = rightRaw.coerceIn(-1.0f, 1.0f)

        // Convert to speed and direction
        val leftDir = when {
            leftRaw > 0.05f -> MotorDir.FORWARD
            leftRaw < -0.05f -> MotorDir.REVERSE
            else -> MotorDir.STOP
        }
        val rightDir = when {
            rightRaw > 0.05f -> MotorDir.FORWARD
            rightRaw < -0.05f -> MotorDir.REVERSE
            else -> MotorDir.STOP
        }

        val leftSpeed = (abs(leftRaw) * 255).toInt().coerceIn(0, 255)
        val rightSpeed = (abs(rightRaw) * 255).toInt().coerceIn(0, 255)

        return CommandFrame(
            leftSpeed = if (leftDir == MotorDir.STOP) 0 else leftSpeed,
            leftDir = leftDir,
            rightSpeed = if (rightDir == MotorDir.STOP) 0 else rightSpeed,
            rightDir = rightDir,
            sequence = sequence
        )
    }
}
