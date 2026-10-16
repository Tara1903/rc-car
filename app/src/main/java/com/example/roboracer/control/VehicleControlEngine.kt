package com.example.roboracer.control

import com.example.roboracer.bluetooth.ConnectionStatus
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
    private val scope: CoroutineScope,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Default
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

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private var loopJob: Job? = null
    private var connectionMonitorJob: Job? = null
    private var sequenceNumber = 0
    private var reconnectNeutralLatched = false

    fun start() {
        if (loopJob?.isActive == true) return

        connectionMonitorJob?.cancel()
        connectionMonitorJob = scope.launch(dispatcher) {
            var lastStatus = transport.connectionStatus.value
            transport.connectionStatus.collect { status ->
                if (status != ConnectionStatus.CONNECTED) {
                    interlock.reset()
                    reconnectNeutralLatched = true
                } else if (lastStatus != ConnectionStatus.CONNECTED && status == ConnectionStatus.CONNECTED) {
                    interlock.reset()
                    reconnectNeutralLatched = true
                }
                lastStatus = status
            }
        }

        loopJob = scope.launch(dispatcher) {
            while (isActive) {
                step()
                delay(33) // ~30 Hz command loop
            }
        }
    }

    fun stop() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = null
        loopJob?.cancel()
        loopJob = null
        interlock.reset()
        val stopCmd = CommandFrame.emergencyStop(sequenceNumber++)
        transport.sendCommand(stopCmd)
        _lastSentCommand.value = stopCmd
    }

    fun emergencyStop() {
        _isEmergencyStopped.value = true
        _rawTouchInput.value = RawTouchInput()
        interlock.reset()
        val stopCmd = CommandFrame.emergencyStop(sequenceNumber++)
        transport.sendCommand(stopCmd)
        _lastSentCommand.value = stopCmd
        _resolvedControl.value = ResolvedControl(0f, 0f, isBrakeEngaged = true, isReverseReady = false)
    }

    fun resetEmergencyStop() {
        _isEmergencyStopped.value = false
        _rawTouchInput.value = RawTouchInput()
        interlock.reset()
        // Intentional safe re-arm: require physical release of all touch zones to neutral
        reconnectNeutralLatched = true
    }

    /**
     * Immediately clears all active touch inputs, resets interlocks, latches neutral release requirement,
     * and dispatches a zero-motion stop command. Invoked on app backgrounding (ON_PAUSE / ON_STOP).
     */
    fun neutralize() {
        _rawTouchInput.value = RawTouchInput()
        interlock.reset()
        reconnectNeutralLatched = true
        val stopCmd = CommandFrame.emergencyStop(sequenceNumber++ and 0xFF)
        transport.sendCommand(stopCmd)
        _lastSentCommand.value = stopCmd
        _resolvedControl.value = ResolvedControl(0f, 0f, isBrakeEngaged = true, isReverseReady = false)
    }

    fun updateSteering(left: Boolean, right: Boolean) {
        if (_isEmergencyStopped.value) return
        _rawTouchInput.value = _rawTouchInput.value.copy(
            isLeftSteerPressed = left,
            isRightSteerPressed = right
        )
    }

    fun updateDrive(accel: Boolean, brake: Boolean) {
        if (_isEmergencyStopped.value) return
        _rawTouchInput.value = _rawTouchInput.value.copy(
            isAccelPressed = accel,
            isBrakePressed = brake
        )
    }

    private fun step() {
        // Latched Emergency Stop enforcement
        if (_isEmergencyStopped.value) {
            val stopCmd = CommandFrame.emergencyStop(sequenceNumber++ and 0xFF)
            transport.sendCommand(stopCmd)
            _lastSentCommand.value = stopCmd
            _resolvedControl.value = ResolvedControl(0f, 0f, isBrakeEngaged = true, isReverseReady = false)
            return
        }

        var currentInput = _rawTouchInput.value

        // Reconnection / Re-arm neutral latch enforcement:
        // Must observe an all-neutral (untouched) state before allowing any active drive command.
        if (reconnectNeutralLatched) {
            if (!currentInput.isAccelPressed && !currentInput.isBrakePressed &&
                !currentInput.isLeftSteerPressed && !currentInput.isRightSteerPressed
            ) {
                // User has released all controls to neutral; fresh touch can now be accepted
                reconnectNeutralLatched = false
            } else {
                // Clamp inputs to neutral until user releases
                currentInput = RawTouchInput()
            }
        }

        // 1. Resolve inputs with safety interlock (pure timer-based settling dwell)
        val resolved = interlock.resolve(currentInput)
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
