package com.example.roboracer.control

class SafetyInterlock(
    private val reverseInterlockDelayMs: Long = 500L,
    private val maxReverseSpeedRatio: Float = 0.50f
) {
    private var isPreviouslyMovingForward = false
    private var zeroSpeedStartTime = 0L

    /**
     * Resolves raw multi-touch inputs into safe normalized throttle & steering.
     * Enforces:
     * 1. Brake overrides Accelerate (Brake priority).
     * 2. Left + Right steering cancellation (Neutral).
     * 3. Forward-to-Reverse interlock delay (500ms zero-throttle electrical settling window).
     *
     * Note on Physical Ground Speed:
     * In an open-loop 4WD robot without physical wheel encoders or tachometers,
     * telemetry actual_speed reflects commanded PWM duty cycle, not ground speed.
     * The 500ms dwell window provides an electrical and mechanical settling period
     * to prevent instantaneous H-bridge polarity inversion, but does NOT represent
     * proof that the physical vehicle has achieved zero ground velocity.
     */
    fun resolve(
        input: RawTouchInput,
        currentSpeed: Int = 0,
        currentTimeMs: Long = System.currentTimeMillis()
    ): ResolvedControl {
        // 1. Steering Conflict Resolution
        val steering = when {
            input.isLeftSteerPressed && input.isRightSteerPressed -> 0.0f
            input.isLeftSteerPressed -> -1.0f
            input.isRightSteerPressed -> 1.0f
            else -> 0.0f
        }

        // 2. Drive Conflict Resolution
        // Safety Rule: Brake ALWAYS overrides Accelerate
        val isBraking = input.isBrakePressed
        val isAccelerating = input.isAccelPressed && !isBraking

        var throttle = 0.0f
        var reverseReady = false

        if (isAccelerating) {
            throttle = 1.0f
            isPreviouslyMovingForward = true
            zeroSpeedStartTime = 0L
        } else if (isBraking) {
            if (currentSpeed > 0) {
                isPreviouslyMovingForward = true
                zeroSpeedStartTime = 0L
                throttle = 0.0f
                reverseReady = false
            } else if (isPreviouslyMovingForward) {
                // Car has reached zero speed; enforce electrical/mechanical dwell window
                if (zeroSpeedStartTime == 0L) {
                    zeroSpeedStartTime = currentTimeMs
                }

                val dwellTime = currentTimeMs - zeroSpeedStartTime
                if (dwellTime >= reverseInterlockDelayMs) {
                    // Safe to reverse (capped at maxReverseSpeedRatio)
                    throttle = -maxReverseSpeedRatio
                    reverseReady = true
                    isPreviouslyMovingForward = false
                } else {
                    // Holding at zero throttle during electrical settling window
                    throttle = 0.0f
                    reverseReady = false
                }
            } else {
                // Not previously moving forward (starting from neutral or continuing reverse)
                throttle = -maxReverseSpeedRatio
                reverseReady = true
            }
        } else {
            // Idle / Coasting (no pedals pressed)
            throttle = 0.0f
            if (currentSpeed == 0) {
                isPreviouslyMovingForward = false
            }
            zeroSpeedStartTime = 0L
        }

        return ResolvedControl(
            normalizedThrottle = throttle,
            normalizedSteering = steering,
            isBrakeEngaged = isBraking,
            isReverseReady = reverseReady
        )
    }

    fun reset() {
        isPreviouslyMovingForward = false
        zeroSpeedStartTime = 0L
    }
}
