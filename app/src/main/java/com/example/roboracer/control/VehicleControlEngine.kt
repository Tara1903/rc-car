package com.example.roboracer.control

import com.example.roboracer.bluetooth.VehicleTransport
import com.example.roboracer.bluetooth.protocol.CommandFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VehicleControlEngine(
    private val transport: VehicleTransport,
    private val scope: CoroutineScope
) {
    private val interlock = SafetyInterlock()
    val mixer = DifferentialDriveMixer()

    private val _rawTouchInput = MutableStateFlow(RawTouchInput())
    val rawTouchInput: StateFlow<RawTouchInput> = _rawTouchInput.asStateFlow()

    private val _resolvedControl = MutableStateFlow(
        ResolvedControl(0f, 0f, isBrakeEngaged = false, isReverseReady = false)
    )
    val resolvedControl: StateFlow<ResolvedControl> = _resolvedControl.asStateFlow()

    private val _lastSentCommand = MutableStateFlow(CommandFrame.emergencyStop())
    val lastSentCommand: StateFlow<CommandFrame> = _lastSentCommand.asStateFlow()

    private var loopJob: Job? = null
    private var sequenceNumber = 0

    fun start() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                step()
                delay(33) // ~30 Hz command loop
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        interlock.reset()
        val stopCmd = CommandFrame.emergencyStop(sequenceNumber++)
        transport.sendCommand(stopCmd)
        _lastSentCommand.value = stopCmd
    }

    fun updateSteering(left: Boolean, right: Boolean) {
        _rawTouchInput.value = _rawTouchInput.value.copy(
            isLeftSteerPressed = left,
            isRightSteerPressed = right
        )
    }

    fun updateDrive(accel: Boolean, brake: Boolean) {
        _rawTouchInput.value = _rawTouchInput.value.copy(
            isAccelPressed = accel,
            isBrakePressed = brake
        )
    }

    private fun step() {
        val currentInput = _rawTouchInput.value
        val latestTelemetry = transport.telemetryFlow.value
        val currentSpeed = maxOf(
            latestTelemetry?.leftActualSpeed ?: 0,
            latestTelemetry?.rightActualSpeed ?: 0
        )

        // 1. Resolve inputs with safety interlock
        val resolved = interlock.resolve(currentInput, currentSpeed)
        _resolvedControl.value = resolved

        // 2. Mix to motor command
        val cmd = mixer.mix(
            throttle = resolved.normalizedThrottle,
            steering = resolved.normalizedSteering,
            sequence = sequenceNumber++ and 0xFF,
            isBraking = resolved.isBrakeEngaged
        )

        // 3. Dispatch to vehicle transport
        transport.sendCommand(cmd)
        _lastSentCommand.value = cmd
    }
}
