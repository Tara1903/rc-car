package com.example.roboracer

import com.example.roboracer.bluetooth.protocol.Crc8
import org.junit.Assert.assertEquals
import org.junit.Test

class Crc8Test {

    @Test
    fun testEmptyData() {
        val empty = byteArrayOf()
        val crc = Crc8.calculate(empty)
        assertEquals(0x00.toByte(), crc)
    }

    @Test
    fun testKnownVector() {
        // Known SMBus / ATM CRC-8 vector: "123456789" (ASCII bytes 0x31..0x39) -> CRC = 0xF4
        val ascii123456789 = "123456789".toByteArray(Charsets.US_ASCII)
        val crc = Crc8.calculate(ascii123456789)
        assertEquals(0xF4.toByte(), crc)
    }

    @Test
    fun testSingleByte() {
        val single = byteArrayOf(0x01)
        val crc = Crc8.calculate(single)
        assertEquals(0x07.toByte(), crc)
    }
}
