package com.example.roboracer.bluetooth.protocol

enum class MotorDir(val value: Byte) {
    STOP(0x00),
    FORWARD(0x01),
    REVERSE(0x02);

    companion object {
        fun fromByte(b: Byte): MotorDir = when (b.toInt() and 0xFF) {
            1 -> FORWARD
            2 -> REVERSE
            else -> STOP
        }
    }
}

data class CommandFrame(
    val leftSpeed: Int,       // 0 - 255
    val leftDir: MotorDir,
    val rightSpeed: Int,      // 0 - 255
    val rightDir: MotorDir,
    val sequence: Int         // 0 - 255
) {
    companion object {
        const val START_BYTE: Byte = 0xAA.toByte()
        const val END_BYTE: Byte = 0x55.toByte()
        const val PROTOCOL_VERSION: Byte = 0x01.toByte()
        const val MSG_TYPE_COMMAND: Byte = 0x01.toByte()
        const val PAYLOAD_LENGTH: Byte = 0x05.toByte()
        const val FRAME_SIZE = 11

        fun emergencyStop(sequence: Int = 0): CommandFrame {
            return CommandFrame(
                leftSpeed = 0,
                leftDir = MotorDir.STOP,
                rightSpeed = 0,
                rightDir = MotorDir.STOP,
                sequence = sequence
            )
        }
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteArray(FRAME_SIZE)
        buffer[0] = START_BYTE
        buffer[1] = PROTOCOL_VERSION
        buffer[2] = MSG_TYPE_COMMAND
        buffer[3] = PAYLOAD_LENGTH
        buffer[4] = (leftSpeed.coerceIn(0, 255)).toByte()
        buffer[5] = leftDir.value
        buffer[6] = (rightSpeed.coerceIn(0, 255)).toByte()
        buffer[7] = rightDir.value
        buffer[8] = (sequence and 0xFF).toByte()
        buffer[9] = Crc8.calculate(buffer, 0, 9)
        buffer[10] = END_BYTE
        return buffer
    }
}
