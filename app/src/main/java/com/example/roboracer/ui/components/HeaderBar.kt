package com.example.roboracer.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboracer.bluetooth.ConnectionStatus
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import com.example.roboracer.theme.AccentCyan
import com.example.roboracer.theme.AccentGreen
import com.example.roboracer.theme.AccentRed
import com.example.roboracer.theme.DarkPanelSurface
import com.example.roboracer.theme.NeonYellow
import com.example.roboracer.theme.NeonYellowBright
import com.example.roboracer.theme.TextMuted
import com.example.roboracer.theme.TextPrimary
import com.example.roboracer.theme.TextSecondary
import com.example.roboracer.ui.effects.cyberpunkBorder

@Composable
fun HeaderBar(
    connectionStatus: ConnectionStatus,
    deviceName: String?,
    latencyMs: Long,
    telemetry: TelemetryFrame?,
    isEmergencyStopped: Boolean = false,
    onEmergencyStop: () -> Unit = {},
    onResetEmergencyStop: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: Branding & Menu Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hamburger Menu Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .cyberpunkBorder(
                        borderColor = NeonYellow.copy(alpha = 0.6f),
                        glowRadius = 4.dp,
                        cornerCut = 6.dp
                    )
                    .clickable(onClick = onMenuClick),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(width = 18.dp, height = 2.dp).background(NeonYellow))
                    Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(NeonYellow))
                    Box(modifier = Modifier.size(width = 18.dp, height = 2.dp).background(NeonYellow))
                }
            }

            // ROBO RACER Brand Logo
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ROBO",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "RACER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = NeonYellowBright
                    )
                }
                Text(
                    text = "ESP32 4WD CONTROLLER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            }
        }

        // CENTER: Bluetooth Connection Pill & Emergency Stop
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val statusColor = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> AccentGreen
                ConnectionStatus.CONNECTING -> NeonYellow
                ConnectionStatus.ERROR -> AccentRed
                ConnectionStatus.DISCONNECTED -> TextMuted
            }

            Box(
                modifier = Modifier
                    .cyberpunkBorder(
                        borderColor = statusColor.copy(alpha = 0.8f),
                        glowColor = statusColor.copy(alpha = 0.35f),
                        glowRadius = 6.dp,
                        cornerCut = 8.dp
                    )
                    .clickable(onClick = onSettingsClick)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
                                    statusColor.copy(alpha = pulseAlpha)
                                } else {
                                    statusColor
                                }
                            )
                    )

                    Text(
                        text = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> "BT: ${deviceName ?: "ROBO-ESP32"}"
                            ConnectionStatus.CONNECTING -> "BT CONNECTING..."
                            ConnectionStatus.ERROR -> "BT LINK LOST"
                            ConnectionStatus.DISCONNECTED -> "DISCONNECTED"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = statusColor
                    )

                    if (connectionStatus == ConnectionStatus.CONNECTED) {
                        Text(
                            text = "${latencyMs}ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            color = AccentCyan
                        )
                    }
                }
            }

            // High-Visibility Emergency Stop Button
            if (isEmergencyStopped) {
                Box(
                    modifier = Modifier
                        .cyberpunkBorder(
                            borderColor = AccentRed,
                            glowColor = AccentRed.copy(alpha = pulseAlpha),
                            glowRadius = 10.dp,
                            cornerCut = 8.dp
                        )
                        .clickable(onClick = onResetEmergencyStop)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠️ E-STOPPED // TAP TO RESET",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = AccentRed
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .cyberpunkBorder(
                            borderColor = AccentRed.copy(alpha = 0.7f),
                            glowColor = AccentRed.copy(alpha = 0.3f),
                            glowRadius = 6.dp,
                            cornerCut = 8.dp
                        )
                        .clickable(onClick = onEmergencyStop)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🛑",
                            fontSize = 10.sp
                        )
                        Text(
                            text = "E-STOP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = AccentRed
                        )
                    }
                }
            }
        }

        // RIGHT: Telemetry Gauges (Battery & Speed) & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Battery Status Bar - True Telemetry (no fabricated defaults when disconnected)
            val isConnected = (connectionStatus == ConnectionStatus.CONNECTED && telemetry != null)
            val battPct = if (isConnected) telemetry.batteryPercentage else null
            val battVolts = if (isConnected) telemetry.batteryVolts else null
            val battColor = when {
                battPct == null -> TextMuted
                battPct > 40 -> AccentGreen
                battPct > 20 -> NeonYellow
                else -> AccentRed
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BATT ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (battVolts != null) String.format("%.1fV", battVolts) else "--.-V",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (isConnected) TextPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (battPct != null) "$battPct%" else "--%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = battColor
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Segmented gauge
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (seg in 1..5) {
                        val segActive = isConnected && battPct != null && ((seg * 20) <= battPct)
                        Box(
                            modifier = Modifier
                                .size(width = 12.dp, height = 5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (segActive) battColor else DarkPanelSurface)
                                .border(0.5.dp, if (segActive) battColor else TextMuted.copy(alpha = 0.5f), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            // Speed HUD Readout
            val speed = if (isConnected) telemetry.speedKmh else null
            Box(
                modifier = Modifier
                    .cyberpunkBorder(
                        borderColor = if (isConnected) NeonYellow.copy(alpha = 0.7f) else TextMuted.copy(alpha = 0.5f),
                        glowRadius = if (isConnected) 6.dp else 0.dp,
                        cornerCut = 6.dp
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (speed != null) String.format("%02d", speed) else "--",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (isConnected) NeonYellowBright else TextMuted
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "KM/H",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // Settings Gear Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .cyberpunkBorder(
                        borderColor = NeonYellow.copy(alpha = 0.6f),
                        glowRadius = 4.dp,
                        cornerCut = 6.dp
                    )
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    fontSize = 18.sp,
                    color = NeonYellow
                )
            }
        }
    }
}
