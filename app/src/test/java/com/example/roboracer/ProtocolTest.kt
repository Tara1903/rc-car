package com.example.roboracer

import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.Crc8
import com.example.roboracer.bluetooth.protocol.MotorDir
import com.example.roboracer.bluetooth.protocol.SystemState
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    @Test
    fun testCommandFrameSerialization() {
        val frame = CommandFrame(
            leftSpeed = 200,
            leftDir = MotorDir.FORWARD,
            rightSpeed = 180,
            rightDir = MotorDir.FORWARD,
            sequence = 42
        )

        val bytes = frame.toByteArray()
        assertEquals(11, bytes.size)
        assertEquals(CommandFrame.START_BYTE, bytes[0])
        assertEquals(0x01.toByte(), bytes[1]) // Version
        assertEquals(0x01.toByte(), bytes[2]) // Msg Type Command
        assertEquals(0x05.toByte(), bytes[3]) // Payload length
        assertEquals(200.toByte(), bytes[4])  // Left speed
        assertEquals(MotorDir.FORWARD.value, bytes[5])
        assertEquals(180.toByte(), bytes[6])  // Right speed
        assertEquals(MotorDir.FORWARD.value, bytes[7])
        assertEquals(42.toByte(), bytes[8])   // Sequence
        // CRC must verify over first 9 bytes
        val expectedCrc = Crc8.calculate(bytes, 0, 9)
        assertEquals(expectedCrc, bytes[9])
        assertEquals(CommandFrame.END_BYTE, bytes[10])
    }

    @Test
    fun testTelemetryFrameParsing() {
        // Construct 13-byte telemetry frame
        val buf = ByteArray(13)
        buf[0] = 0xAA.toByte()
        buf[1] = 0x01.toByte() // Version
        buf[2] = 0x10.toByte() // Type Telemetry
        buf[3] = 0x07.toByte() // Length
        buf[4] = 0x01.toByte() // State: RUNNING
        buf[5] = 150.toByte()  // Left actual speed
        buf[6] = 160.toByte()  // Right actual speed
        buf[7] = 0x17.toByte() // Batt MSB (6000 mV = 0x1770)
        buf[8] = 0x70.toByte() // Batt LSB
        buf[9] = 0x00.toByte() // Fault flags
        buf[10] = 105.toByte() // Counter
        buf[11] = Crc8.calculate(buf, 0, 11)
        buf[12] = 0x55.toByte()

        val parsed = TelemetryFrame.parse(buf)
        assertNotNull(parsed)
        assertEquals(SystemState.RUNNING, parsed!!.state)
        assertEquals(150, parsed.leftActualSpeed)
        assertEquals(160, parsed.rightActualSpeed)
        assertEquals(6000, parsed.batteryMilliVolts)
        assertEquals(6.0f, parsed.batteryVolts!!, 0.01f)
        assertEquals(80, parsed.batteryPercentage)
        assertEquals(0, parsed.faultFlags)
        assertEquals(105, parsed.fwCounter)

        // Honest telemetry: real hardware frame has no physical tachometer, so speedKmh is null
        assertNull(parsed.speedKmh)

        // In simulation mode, speedKmh is available
        val simParsed = parsed.copy(isSimulated = true)
        assertNotNull(simParsed.speedKmh)
        assertTrue(simParsed.speedKmh!! > 0)

        // Sub-3500mV ADC reading (uncalibrated, disconnected, or heavy brownout) produces null voltage/percentage
        val lowAdc = parsed.copy(batteryMilliVolts = 1500)
        assertNull(lowAdc.batteryVolts)
        assertNull(lowAdc.batteryPercentage)
    }

    @Test
    fun testCorruptedTelemetryFrameRejection() {
        val buf = ByteArray(13)
        buf[0] = 0xAA.toByte()
        buf[1] = 0x01.toByte()
        buf[2] = 0x10.toByte()
        buf[3] = 0x07.toByte()
        buf[11] = 0x99.toByte() // Wrong CRC
        buf[12] = 0x55.toByte()

        val parsed = TelemetryFrame.parse(buf)
        assertNull(parsed)
    }

    @Test
    fun testSequenceWraparound() {
        val frame = CommandFrame(
            leftSpeed = 255,
            leftDir = MotorDir.FORWARD,
            rightSpeed = 255,
            rightDir = MotorDir.FORWARD,
            sequence = 256
        )
        val bytes = frame.toByteArray()
        assertEquals(0x00.toByte(), bytes[8])
        val expectedCrc = Crc8.calculate(bytes, 0, 9)
        assertEquals(expectedCrc, bytes[9])
    }

    @Test
    fun testTelemetryBatteryInvalidFlag() {
        val buf = ByteArray(13)
        buf[0] = 0xAA.toByte()
        buf[1] = 0x01.toByte()
        buf[2] = 0x10.toByte()
        buf[3] = 0x07.toByte()
        buf[4] = 0x01.toByte() // RUNNING
        buf[5] = 100.toByte()
        buf[6] = 100.toByte()
        buf[7] = 0x17.toByte() // 6000 mV
        buf[8] = 0x70.toByte()
        buf[9] = TelemetryFrame.FAULT_BATT_INVALID.toByte() // 0x10
        buf[10] = 1.toByte()
        buf[11] = Crc8.calculate(buf, 0, 11)
        buf[12] = 0x55.toByte()

        val parsed = TelemetryFrame.parse(buf)
        assertNotNull(parsed)
        assertTrue((parsed!!.faultFlags and TelemetryFrame.FAULT_BATT_INVALID) != 0)
        org.junit.Assert.assertFalse(parsed.isBatteryValid)
        assertNull(parsed.batteryVolts)
        assertNull(parsed.batteryPercentage)
    }
}
