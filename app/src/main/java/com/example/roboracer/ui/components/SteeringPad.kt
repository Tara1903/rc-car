package com.example.roboracer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roboracer.theme.DarkPanelSurface
import com.example.roboracer.theme.NeonYellow
import com.example.roboracer.theme.NeonYellowBright
import com.example.roboracer.theme.SteeringActive
import com.example.roboracer.theme.TextMuted
import com.example.roboracer.ui.effects.cyberpunkBorder

@Composable
fun SteeringPad(
    isLeft: Boolean,
    isPressed: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnPressedChange by rememberUpdatedState(onPressedChange)

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) SteeringActive else DarkPanelSurface,
        animationSpec = tween(120),
        label = "steeringBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isPressed) NeonYellowBright else NeonYellow.copy(alpha = 0.5f),
        animationSpec = tween(120),
        label = "steeringBorder"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(130.dp)
            .cyberpunkBorder(
                borderColor = borderColor,
                glowColor = if (isPressed) NeonYellow.copy(alpha = 0.6f) else NeonYellow.copy(alpha = 0.2f),
                backgroundColor = bgColor,
                glowRadius = if (isPressed) 14.dp else 6.dp,
                cornerCut = 16.dp
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    currentOnPressedChange(true)
                    val pointerId = down.id

                    // Track until pointer release or cancel
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentChange = event.changes.firstOrNull { it.id == pointerId }
                        if (currentChange == null || !currentChange.pressed) {
                            currentOnPressedChange(false)
                            break
                        }
                    }
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon chevron arrows
            Text(
                text = if (isLeft) "◄ ◄" else "► ►",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = if (isPressed) 32.sp else 28.sp,
                color = if (isPressed) NeonYellowBright else NeonYellow
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isLeft) "LEFT" else "RIGHT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = if (isPressed) Color.White else TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "STEER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = TextMuted
            )
        }
    }
}
