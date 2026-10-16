package com.example.roboracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roboracer.theme.DarkBackground
import com.example.roboracer.ui.components.CarViewport
import com.example.roboracer.ui.components.DrivePedals
import com.example.roboracer.ui.components.HeaderBar
import com.example.roboracer.ui.components.SettingsDialog
import com.example.roboracer.ui.components.SteeringPad

@Composable
fun ControllerScreen(
    viewModel: ControllerViewModel = viewModel()
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val activeDeviceName by viewModel.activeDeviceName.collectAsState()
    val latencyMs by viewModel.rttLatencyMs.collectAsState()
    val telemetry by viewModel.telemetryFlow.collectAsState()

    val rawTouchInput by viewModel.rawTouchInput.collectAsState()
    val resolvedControl by viewModel.resolvedControl.collectAsState()
    val lastSentCommand by viewModel.lastSentCommand.collectAsState()

    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsState()
    val isEmergencyStopped by viewModel.isEmergencyStopped.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // TOP-LEVEL HUD CONTAINER
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP HEADER BAR
            HeaderBar(
                connectionStatus = connectionStatus,
                deviceName = activeDeviceName,
                latencyMs = latencyMs,
                telemetry = telemetry,
                isEmergencyStopped = isEmergencyStopped,
                onEmergencyStop = { viewModel.emergencyStop() },
                onResetEmergencyStop = { viewModel.resetEmergencyStop() },
                onSettingsClick = { viewModel.openSettings() },
                onMenuClick = { viewModel.openSettings() }
            )

            // 2. MAIN COCKPIT AREA (Steering Pads + Center 3D Car Viewport)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Background Center 3D / Isometric Car Viewport
                CarViewport(
                    resolvedControl = resolvedControl,
                    telemetry = telemetry,
                    modifier = Modifier.fillMaxSize()
                )

                // Left & Right Steering Panels Overlaid at edges
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT FULL-SIDE STEERING TOUCH ZONE
                    SteeringPad(
                        isLeft = true,
                        isPressed = rawTouchInput.isLeftSteerPressed,
                        onPressedChange = { viewModel.onLeftSteerTouch(it) }
                    )

                    // Spacer between steering panels
                    Box(modifier = Modifier.weight(1f))

                    // RIGHT FULL-SIDE STEERING TOUCH ZONE
                    SteeringPad(
                        isLeft = false,
                        isPressed = rawTouchInput.isRightSteerPressed,
                        onPressedChange = { viewModel.onRightSteerTouch(it) }
                    )
                }

                // 3. BOTTOM DRIVE PEDALS (Brake/Reverse and Accelerate)
                DrivePedals(
                    isBrakePressed = rawTouchInput.isBrakePressed,
                    onBrakeChange = { viewModel.onBrakeTouch(it) },
                    isAccelPressed = rawTouchInput.isAccelPressed,
                    onAccelChange = { viewModel.onAccelTouch(it) },
                    isReverseReady = resolvedControl.isReverseReady,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 148.dp, end = 148.dp, bottom = 4.dp)
                )
            }
        }

        // 4. SETTINGS & BLUETOOTH CONNECTION MODAL
        SettingsDialog(
            isOpen = isSettingsOpen,
            onDismiss = { viewModel.closeSettings() },
            connectionStatus = connectionStatus,
            activeDeviceName = activeDeviceName,
            pairedDevices = viewModel.getBondedDevices(),
            isSimulatorMode = isSimulatorMode,
            onToggleSimulatorMode = { viewModel.setSimulatorMode(it) },
            onConnectDevice = { viewModel.connectDevice(it) },
            onDisconnect = { viewModel.disconnectDevice() },
            mixer = viewModel.mixer,
            latestTelemetry = telemetry,
            lastCommand = lastSentCommand
        )
    }
}
