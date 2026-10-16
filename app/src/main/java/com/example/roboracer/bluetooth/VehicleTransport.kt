package com.example.roboracer.bluetooth

import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DiscoveredDevice(
    val name: String,
    val address: String
)

interface VehicleTransport {
    val connectionStatus: StateFlow<ConnectionStatus>
    val telemetryFlow: StateFlow<TelemetryFrame?>
    val rttLatencyMs: StateFlow<Long>
    val activeDeviceName: StateFlow<String?>

    fun connect(deviceAddress: String, deviceName: String = "ESP32")
    fun disconnect()
    fun sendCommand(command: CommandFrame)
    fun getBondedDevices(): List<DiscoveredDevice>
}
