package com.example.roboracer.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.roboracer.bluetooth.BluetoothSppTransport
import com.example.roboracer.bluetooth.ConnectionStatus
import com.example.roboracer.bluetooth.DiscoveredDevice
import com.example.roboracer.bluetooth.MockVehicleTransport
import com.example.roboracer.bluetooth.VehicleTransport
import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import com.example.roboracer.control.RawTouchInput
import com.example.roboracer.control.ResolvedControl
import com.example.roboracer.control.VehicleControlEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val sppTransport = BluetoothSppTransport(application, viewModelScope)
    private val mockTransport = MockVehicleTransport(viewModelScope)

    private val _isSimulatorMode = MutableStateFlow(false)
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private val currentTransport = MutableStateFlow<VehicleTransport>(sppTransport)

    val connectionStatus: StateFlow<ConnectionStatus> = currentTransport.flatMapLatest {
        it.connectionStatus
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionStatus.DISCONNECTED)

    val activeDeviceName: StateFlow<String?> = currentTransport.flatMapLatest {
        it.activeDeviceName
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val rttLatencyMs: StateFlow<Long> = currentTransport.flatMapLatest {
        it.rttLatencyMs
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val telemetryFlow: StateFlow<TelemetryFrame?> = currentTransport.flatMapLatest {
        it.telemetryFlow
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Stable wrapper flows: the UI subscribes to these once and they always reflect the
    // current engine's output, even after setSimulatorMode() swaps the engine.
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

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private var controlEngine = VehicleControlEngine(sppTransport, viewModelScope)
    private var engineBridgeJob: Job? = null

    // mixer always refers to the current engine — safe because callers only read it
    // from UI thread after the switch completes (SettingsDialog reads it lazily)
    val mixer get() = controlEngine.mixer

    init {
        controlEngine.start()
        attachEngineBridge(controlEngine)
    }

    // Collects all four engine flows into the stable wrapper flows so the UI
    // continues working without re-subscribing when the engine is replaced.
    private fun attachEngineBridge(engine: VehicleControlEngine) {
        engineBridgeJob?.cancel()
        engineBridgeJob = viewModelScope.launch {
            launch { engine.rawTouchInput.collect { _rawTouchInput.value = it } }
            launch { engine.resolvedControl.collect { _resolvedControl.value = it } }
            launch { engine.lastSentCommand.collect { _lastSentCommand.value = it } }
            launch { engine.isEmergencyStopped.collect { _isEmergencyStopped.value = it } }
        }
    }

    fun openSettings() { _isSettingsOpen.value = true }
    fun closeSettings() { _isSettingsOpen.value = false }

    fun setSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        engineBridgeJob?.cancel()
        controlEngine.stop()
        currentTransport.value.disconnect()

        val newTransport = if (enabled) mockTransport else sppTransport
        currentTransport.value = newTransport

        controlEngine = VehicleControlEngine(newTransport, viewModelScope)
        controlEngine.start()
        attachEngineBridge(controlEngine)

        // Reset wrapper state to neutral on mode switch
        _rawTouchInput.value = RawTouchInput()
        _resolvedControl.value = ResolvedControl(0f, 0f, isBrakeEngaged = false, isReverseReady = false)
        _isEmergencyStopped.value = false

        if (enabled) {
            newTransport.connect("00:11:22:33:44:55", "ROBO-RACER-ESP32 (Simulated)")
        }
    }

    fun getBondedDevices(): List<DiscoveredDevice> = currentTransport.value.getBondedDevices()

    fun connectDevice(device: DiscoveredDevice) {
        currentTransport.value.connect(device.address, device.name)
    }

    fun disconnectDevice() {
        currentTransport.value.disconnect()
    }

    fun onLeftSteerTouch(pressed: Boolean) {
        controlEngine.updateSteering(left = pressed, right = _rawTouchInput.value.isRightSteerPressed)
    }

    fun onRightSteerTouch(pressed: Boolean) {
        controlEngine.updateSteering(left = _rawTouchInput.value.isLeftSteerPressed, right = pressed)
    }

    fun onBrakeTouch(pressed: Boolean) {
        controlEngine.updateDrive(accel = _rawTouchInput.value.isAccelPressed, brake = pressed)
    }

    fun onAccelTouch(pressed: Boolean) {
        controlEngine.updateDrive(accel = pressed, brake = _rawTouchInput.value.isBrakePressed)
    }

    fun emergencyStop() {
        controlEngine.emergencyStop()
    }

    fun resetEmergencyStop() {
        controlEngine.resetEmergencyStop()
    }

    fun neutralize() {
        controlEngine.neutralize()
    }

    override fun onCleared() {
        super.onCleared()
        engineBridgeJob?.cancel()
        controlEngine.stop()
        currentTransport.value.disconnect()
    }
}
