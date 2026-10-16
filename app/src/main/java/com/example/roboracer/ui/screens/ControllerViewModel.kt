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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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

    private var controlEngine = VehicleControlEngine(sppTransport, viewModelScope)

    val rawTouchInput: StateFlow<RawTouchInput> get() = controlEngine.rawTouchInput
    val resolvedControl: StateFlow<ResolvedControl> get() = controlEngine.resolvedControl
    val lastSentCommand: StateFlow<CommandFrame> get() = controlEngine.lastSentCommand
    val isEmergencyStopped: StateFlow<Boolean> get() = controlEngine.isEmergencyStopped
    val mixer get() = controlEngine.mixer

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    init {
        controlEngine.start()
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        controlEngine.stop()
        currentTransport.value.disconnect()

        val newTransport = if (enabled) mockTransport else sppTransport
        currentTransport.value = newTransport

        controlEngine = VehicleControlEngine(newTransport, viewModelScope)
        controlEngine.start()

        if (enabled) {
            newTransport.connect("00:11:22:33:44:55", "ROBO-RACER-ESP32 (Simulated)")
        }
    }

    fun getBondedDevices(): List<DiscoveredDevice> {
        return currentTransport.value.getBondedDevices()
    }

    fun connectDevice(device: DiscoveredDevice) {
        currentTransport.value.connect(device.address, device.name)
    }

    fun disconnectDevice() {
        currentTransport.value.disconnect()
    }

    fun onLeftSteerTouch(pressed: Boolean) {
        controlEngine.updateSteering(left = pressed, right = rawTouchInput.value.isRightSteerPressed)
    }

    fun onRightSteerTouch(pressed: Boolean) {
        controlEngine.updateSteering(left = rawTouchInput.value.isLeftSteerPressed, right = pressed)
    }

    fun onBrakeTouch(pressed: Boolean) {
        controlEngine.updateDrive(accel = rawTouchInput.value.isAccelPressed, brake = pressed)
    }

    fun onAccelTouch(pressed: Boolean) {
        controlEngine.updateDrive(accel = pressed, brake = rawTouchInput.value.isBrakePressed)
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
        controlEngine.stop()
        currentTransport.value.disconnect()
    }
}
