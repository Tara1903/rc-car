package com.example.roboracer.control

data class RawTouchInput(
    val isLeftSteerPressed: Boolean = false,
    val isRightSteerPressed: Boolean = false,
    val isAccelPressed: Boolean = false,
    val isBrakePressed: Boolean = false
)

data class ResolvedControl(
    val normalizedThrottle: Float,   // -1.0f (full reverse) to +1.0f (full forward)
    val normalizedSteering: Float,   // -1.0f (full left) to +1.0f (full right)
    val isBrakeEngaged: Boolean,
    val isReverseReady: Boolean
)
