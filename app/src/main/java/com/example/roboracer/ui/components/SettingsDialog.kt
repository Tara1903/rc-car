package com.example.roboracer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roboracer.bluetooth.ConnectionStatus
import com.example.roboracer.bluetooth.DiscoveredDevice
import com.example.roboracer.bluetooth.protocol.CommandFrame
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import com.example.roboracer.control.DifferentialDriveMixer
import com.example.roboracer.theme.AccentCyan
import com.example.roboracer.theme.AccentGreen
import com.example.roboracer.theme.AccentRed
import com.example.roboracer.theme.DarkBackground
import com.example.roboracer.theme.DarkBackgroundSurface
import com.example.roboracer.theme.DarkPanelSurface
import com.example.roboracer.theme.NeonYellow
import com.example.roboracer.theme.NeonYellowBright
import com.example.roboracer.theme.TextMuted
import com.example.roboracer.theme.TextPrimary
import com.example.roboracer.theme.TextSecondary
import com.example.roboracer.ui.effects.cyberpunkBorder

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    connectionStatus: ConnectionStatus,
    activeDeviceName: String?,
    pairedDevices: List<DiscoveredDevice>,
    isSimulatorMode: Boolean,
    onToggleSimulatorMode: (Boolean) -> Unit,
    onConnectDevice: (DiscoveredDevice) -> Unit,
    onDisconnect: () -> Unit,
    mixer: DifferentialDriveMixer,
    latestTelemetry: TelemetryFrame?,
    lastCommand: CommandFrame
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .width(540.dp)
                .height(380.dp)
                .cyberpunkBorder(
                    borderColor = NeonYellow,
                    glowRadius = 12.dp,
                    backgroundColor = DarkBackgroundSurface,
                    cornerCut = 16.dp
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VEHICLE CONFIG & LINK // HUD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeonYellowBright
                        )
                        Text(
                            text = "Prakti ESP32 SPP & Calibrations",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .cyberpunkBorder(
                                borderColor = AccentRed.copy(alpha = 0.8f),
                                glowRadius = 4.dp,
                                cornerCut = 4.dp
                            )
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: Simulator Mode Toggle
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkPanelSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, NeonYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isSimulatorMode) "MODE: VIRTUAL SIMULATOR" else "MODE: PHYSICAL BLUETOOTH SPP",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSimulatorMode) AccentCyan else NeonYellow
                                )
                                Text(
                                    text = if (isSimulatorMode) "Simulates TT motors & telemetry in-app" else "Transmits 11-byte RFCOMM frames to ESP32",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = isSimulatorMode,
                                onCheckedChange = onToggleSimulatorMode,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentCyan,
                                    checkedTrackColor = DarkBackground,
                                    uncheckedThumbColor = NeonYellow,
                                    uncheckedTrackColor = DarkBackground
                                )
                            )
                        }
                    }

                    // Item 2: Bluetooth Devices List
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET BLUETOOTH DEVICES",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                if (connectionStatus == ConnectionStatus.CONNECTED) {
                                    Text(
                                        text = "DISCONNECT",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = AccentRed,
                                        modifier = Modifier.clickable(onClick = onDisconnect)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    items(pairedDevices) { device ->
                        val isConnected = connectionStatus == ConnectionStatus.CONNECTED && activeDeviceName?.contains(device.name) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isConnected) DarkPanelSurface else DarkBackground)
                                .border(
                                    1.dp,
                                    if (isConnected) AccentGreen else NeonYellow.copy(alpha = 0.25f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onConnectDevice(device) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = device.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isConnected) AccentGreen else TextPrimary
                                )
                                Text(
                                    text = device.address,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = if (isConnected) "CONNECTED" else "CONNECT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (isConnected) AccentGreen else NeonYellow
                            )
                        }
                    }

                    // Item 3: Vehicle Tuning Sliders
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkPanelSurface, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "CALIBRATION & TRIMS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeonYellow
                            )

                            // Max Speed Limit Slider
                            var maxSpd by remember { mutableFloatStateOf(mixer.maxSpeedLimit) }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("MAX SPEED LIMIT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                                    Text("${(maxSpd * 100).toInt()}%", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NeonYellowBright)
                                }
                                Slider(
                                    value = maxSpd,
                                    onValueChange = {
                                        maxSpd = it
                                        mixer.maxSpeedLimit = it
                                    },
                                    valueRange = 0.4f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = NeonYellow, activeTrackColor = NeonYellow)
                                )
                            }

                            // Motor Trim Slider
                            var trim by remember { mutableFloatStateOf(mixer.trimOffset) }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("STEERING TRIM (L / R BALANCE)", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        when {
                                            trim > 0.02f -> "R +${(trim * 100).toInt()}%"
                                            trim < -0.02f -> "L +${(-trim * 100).toInt()}%"
                                            else -> "CENTER"
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = AccentCyan
                                    )
                                }
                                Slider(
                                    value = trim,
                                    onValueChange = {
                                        trim = it
                                        mixer.trimOffset = it
                                    },
                                    valueRange = -0.2f..0.2f,
                                    colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                                )
                            }
                        }
                    }

                    // Item 4: Raw Protocol Frame Diagnostics
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "RAW PROTOCOL TELEMETRY DEBUG",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val cmdBytes = lastCommand.toByteArray()
                            val hexCmd = cmdBytes.joinToString(" ") { String.format("%02X", it) }
                            Text(
                                text = "TX (11B): $hexCmd",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NeonYellow
                            )
                            Text(
                                text = "RX (13B): State=${latestTelemetry?.state ?: "N/A"} Batt=${latestTelemetry?.batteryMilliVolts ?: 0}mV Faults=0x${String.format("%02X", latestTelemetry?.faultFlags ?: 0)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = AccentCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
