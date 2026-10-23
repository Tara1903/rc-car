package com.example.roboracer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboracer.theme.AccentRed
import com.example.roboracer.theme.AccelActive
import com.example.roboracer.theme.AccelGlow
import com.example.roboracer.theme.BrakeActive
import com.example.roboracer.theme.DarkPanelSurface
import com.example.roboracer.theme.NeonYellow
import com.example.roboracer.theme.NeonYellowBright
import com.example.roboracer.theme.TextMuted
import com.example.roboracer.theme.TextPrimary
import com.example.roboracer.ui.effects.cyberpunkBorder

@Composable
fun DrivePedals(
    isBrakePressed: Boolean,
    onBrakeChange: (Boolean) -> Unit,
    isAccelPressed: Boolean,
    onAccelChange: (Boolean) -> Unit,
    isReverseReady: Boolean,
    modifier: Modifier = Modifier
) {
    val currentOnBrakeChange by rememberUpdatedState(onBrakeChange)
    val currentOnAccelChange by rememberUpdatedState(onAccelChange)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // BRAKE / REVERSE PEDAL (Bottom Left)
        val brakeScale by animateFloatAsState(
            targetValue = if (isBrakePressed) 0.94f else 1.0f,
            animationSpec = tween(80),
            label = "brakeScale"
        )
        val brakeBgColor by animateColorAsState(
            targetValue = if (isBrakePressed) BrakeActive.copy(alpha = 0.45f) else DarkPanelSurface,
            animationSpec = tween(100),
            label = "brakeBg"
        )
        val brakeBorderColor by animateColorAsState(
            targetValue = if (isBrakePressed) BrakeActive else AccentRed.copy(alpha = 0.7f),
            animationSpec = tween(100),
            label = "brakeBorder"
        )

        Box(
            modifier = Modifier
                .scale(brakeScale)
                .width(220.dp)
                .height(84.dp)
                .cyberpunkBorder(
                    borderColor = brakeBorderColor,
                    glowColor = BrakeActive.copy(alpha = if (isBrakePressed) 0.7f else 0.25f),
                    backgroundColor = brakeBgColor,
                    glowRadius = if (isBrakePressed) 16.dp else 8.dp,
                    cornerCut = 14.dp
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        currentOnBrakeChange(true)
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                currentOnBrakeChange(false)
                                break
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "◎ ", // Disc brake rotor icon
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBrakePressed) Color.White else AccentRed
                    )
                    Text(
                        text = if (isReverseReady) "REVERSE" else "BRAKE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp,
                        color = if (isBrakePressed) Color.White else AccentRed
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isReverseReady) "REVERSE ACTIVE" else "HOLD TO BRAKE OR REVERSE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = if (isReverseReady) BrakeActive else TextMuted
                )
            }
        }

        // ACCELERATE PEDAL (Bottom Right) - Neon Green
        val accelScale by animateFloatAsState(
            targetValue = if (isAccelPressed) 0.94f else 1.0f,
            animationSpec = tween(80),
            label = "accelScale"
        )
        val accelBgColor by animateColorAsState(
            targetValue = if (isAccelPressed) AccelActive.copy(alpha = 0.45f) else DarkPanelSurface,
            animationSpec = tween(100),
            label = "accelBg"
        )
        val accelBorderColor by animateColorAsState(
            targetValue = if (isAccelPressed) AccelActive else AccelActive.copy(alpha = 0.75f),
            animationSpec = tween(100),
            label = "accelBorder"
        )

        Box(
            modifier = Modifier
                .scale(accelScale)
                .width(220.dp)
                .height(84.dp)
                .cyberpunkBorder(
                    borderColor = accelBorderColor,
                    glowColor = AccelGlow.copy(alpha = if (isAccelPressed) 0.75f else 0.3f),
                    backgroundColor = accelBgColor,
                    glowRadius = if (isAccelPressed) 16.dp else 8.dp,
                    cornerCut = 14.dp
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        currentOnAccelChange(true)
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                currentOnAccelChange(false)
                                break
                            }
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚡ ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAccelPressed) Color.White else AccelActive
                    )
                    Text(
                        text = "ACCELERATE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.2.sp,
                        color = if (isAccelPressed) Color.White else AccelActive
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "HOLD TO MOVE FORWARD",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = if (isAccelPressed) Color.White else TextMuted
                )
            }
        }
    }
}
