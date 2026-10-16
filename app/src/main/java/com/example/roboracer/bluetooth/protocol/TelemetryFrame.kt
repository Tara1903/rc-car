package com.example.roboracer.bluetooth.protocol

enum class SystemState(val value: Int) {
    IDLE(0),
    RUNNING(1),
    FAILSAFE(2),
    LOW_BATTERY(3);

    companion object {
        fun fromInt(v: Int): SystemState = when (v) {
            1 -> RUNNING
            2 -> FAILSAFE
            3 -> LOW_BATTERY
            else -> IDLE
        }
    }
}

data class TelemetryFrame(
    val state: SystemState,
    val leftActualSpeed: Int,
    val rightActualSpeed: Int,
    val batteryMilliVolts: Int,
    val faultFlags: Int,
    val fwCounter: Int,
    val isSimulated: Boolean = false,
    val hasPhysicalSpeedSensor: Boolean = false
) {
    val isFailsafe: Boolean get() = (faultFlags and FAULT_WATCHDOG_TIMEOUT) != 0 || state == SystemState.FAILSAFE
    val isLowBattery: Boolean get() = (faultFlags and FAULT_LOW_BATTERY) != 0 || state == SystemState.LOW_BATTERY
    val isBatteryInvalid: Boolean get() = (faultFlags and FAULT_BATT_INVALID) != 0 || batteryMilliVolts < 6000 || batteryMilliVolts > 13500
    val isBatteryValid: Boolean get() = !isBatteryInvalid

    // Battery voltage in Volts. Returns null if reading is uncalibrated, disconnected, or out of range.
    val batteryVolts: Float?
        get() = if (isBatteryValid) batteryMilliVolts / 1000.0f else null

    // Battery percentage estimate for 3S Li-ion (9.6V empty to 12.6V full).
    // Returns null if uncalibrated, disconnected, or invalid.
    val batteryPercentage: Int?
        get() = when {
            !isBatteryValid -> null
            batteryMilliVolts >= 12600 -> 100
            batteryMilliVolts <= 9600 -> 0
            else -> {
                val pct = ((batteryMilliVolts - 9600) * 100) / (12600 - 9600)
                pct.coerceIn(0, 100)
            }
        }

    // Speed in km/h. On real hardware without a physical wheel encoder / tachometer,
    // this returns null (rendered as "-- KM/H") to avoid displaying invented PWM-derived speed.
    // In simulator mode, simulated ground speed is displayed for UI demonstration.
    val speedKmh: Int?
        get() = when {
            hasPhysicalSpeedSensor -> {
                // Reserved for physical optical/Hall-effect tachometer integration
                null
            }
            isSimulated -> {
                val maxPwm = maxOf(leftActualSpeed, rightActualSpeed)
                ((maxPwm * 35) / 255)
            }
            else -> null
        }

    companion object {
        const val START_BYTE: Byte = 0xAA.toByte()
        const val END_BYTE: Byte = 0x55.toByte()
        const val PROTOCOL_VERSION: Byte = 0x01.toByte()
        const val MSG_TYPE_TELEMETRY: Byte = 0x10.toByte()
        const val PAYLOAD_LENGTH: Byte = 0x07.toByte()
        const val FRAME_SIZE = 13

        // Fault Bitmask Constants
        const val FAULT_WATCHDOG_TIMEOUT = 0x01
        const val FAULT_LOW_BATTERY = 0x02
        const val FAULT_CRC_ERROR = 0x04
        const val FAULT_MOTOR_STALL = 0x08
        const val FAULT_BATT_INVALID = 0x10

        fun parse(data: ByteArray, offset: Int = 0): TelemetryFrame? {
            if (data.size - offset < FRAME_SIZE) return null

            if (data[offset] != START_BYTE ||
                data[offset + 1] != PROTOCOL_VERSION ||
                data[offset + 2] != MSG_TYPE_TELEMETRY ||
                data[offset + 3] != PAYLOAD_LENGTH ||
                data[offset + 12] != END_BYTE
            ) {
                return null
            }

            // Verify CRC-8
            val expectedCrc = Crc8.calculate(data, offset, 11)
            val actualCrc = data[offset + 11]
            if (expectedCrc != actualCrc) {
                return null
            }

            val stateVal = data[offset + 4].toInt() and 0xFF
            val leftAct = data[offset + 5].toInt() and 0xFF
            val rightAct = data[offset + 6].toInt() and 0xFF
            val msb = data[offset + 7].toInt() and 0xFF
            val lsb = data[offset + 8].toInt() and 0xFF
            val battMv = (msb shl 8) or lsb
            val faults = data[offset + 9].toInt() and 0xFF
            val counter = data[offset + 10].toInt() and 0xFF

            return TelemetryFrame(
                state = SystemState.fromInt(stateVal),
                leftActualSpeed = leftAct,
                rightActualSpeed = rightAct,
                batteryMilliVolts = battMv,
                faultFlags = faults,
                fwCounter = counter
            )
        }
    }
}
