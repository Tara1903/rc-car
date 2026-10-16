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
     * 3. Forward-to-Reverse interlock delay (500ms zero-speed window).
     */
    fun resolve(
        input: RawTouchInput,
        currentSpeed: Int,
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
            if (isPreviouslyMovingForward && currentSpeed > 10) {
                // Active braking state: do not engage reverse while car has forward momentum
                throttle = 0.0f
                zeroSpeedStartTime = 0L
            } else {
                // Vehicle is stopped or transitioning to reverse
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
                    // Holding at zero speed for interlock delay
                    throttle = 0.0f
                }
            }
        } else {
            // Idle / Coasting
            throttle = 0.0f
            if (currentSpeed == 0) {
                isPreviouslyMovingForward = false
                zeroSpeedStartTime = 0L
            }
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
