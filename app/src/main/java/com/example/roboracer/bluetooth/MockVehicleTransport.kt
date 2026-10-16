package com.example.roboracer.bluetooth

import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.MotorDir
import com.example.roboracer.bluetooth.protocol.SystemState
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MockVehicleTransport(
    private val scope: CoroutineScope
) : VehicleTransport {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _telemetryFlow = MutableStateFlow<TelemetryFrame?>(null)
    override val telemetryFlow: StateFlow<TelemetryFrame?> = _telemetryFlow.asStateFlow()

    private val _rttLatencyMs = MutableStateFlow(12L)
    override val rttLatencyMs: StateFlow<Long> = _rttLatencyMs.asStateFlow()

    private val _activeDeviceName = MutableStateFlow<String?>(null)
    override val activeDeviceName: StateFlow<String?> = _activeDeviceName.asStateFlow()

    private var simJob: Job? = null
    private var lastCmd: CommandFrame? = null
    private var simLeftSpeed = 0
    private var simRightSpeed = 0
    private var simBatteryMv = 11850 // 11.85V

    override fun getBondedDevices(): List<DiscoveredDevice> = listOf(
        DiscoveredDevice("ROBO-RACER-ESP32 (Simulated)", "00:11:22:33:44:55"),
        DiscoveredDevice("Prakti ESP32 DevKit (Lab)", "AA:BB:CC:DD:EE:FF")
    )

    override fun connect(deviceAddress: String, deviceName: String) {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _activeDeviceName.value = "$deviceName (SIM)"

        scope.launch {
            delay(400) // Simulate connection handshake
            _connectionStatus.value = ConnectionStatus.CONNECTED
            startSimLoop()
        }
    }

    override fun disconnect() {
        simJob?.cancel()
        simJob = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _activeDeviceName.value = null
        _telemetryFlow.value = null
    }

    override fun sendCommand(command: CommandFrame) {
        lastCmd = command
    }

    private fun startSimLoop() {
        simJob?.cancel()
        simJob = scope.launch {
            var counter = 0
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(50) // 20 Hz telemetry
                counter++

                val cmd = lastCmd
                if (cmd != null) {
                    val targetLeft = if (cmd.leftDir == MotorDir.STOP) 0 else cmd.leftSpeed
                    val targetRight = if (cmd.rightDir == MotorDir.STOP) 0 else cmd.rightSpeed

                    // Inertia simulation
                    simLeftSpeed += ((targetLeft - simLeftSpeed) * 0.3f).toInt()
                    simRightSpeed += ((targetRight - simRightSpeed) * 0.3f).toInt()

                    // Voltage sag under motor load
                    val loadSag = ((simLeftSpeed + simRightSpeed) / 510.0f * 350.0f).toInt()
                    val nominalBatt = 11800 - (counter / 20) // Very slow natural discharge
                    simBatteryMv = (nominalBatt - loadSag).coerceIn(9800, 12600)
                }

                val state = if (simLeftSpeed > 0 || simRightSpeed > 0) SystemState.RUNNING else SystemState.IDLE

                _telemetryFlow.value = TelemetryFrame(
                    state = state,
                    leftActualSpeed = simLeftSpeed,
                    rightActualSpeed = simRightSpeed,
                    batteryMilliVolts = simBatteryMv,
                    faultFlags = 0,
                    fwCounter = counter and 0xFF
                )
                _rttLatencyMs.value = (10..18).random().toLong()
            }
        }
    }
}
