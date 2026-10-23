package com.example.roboracer.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.roboracer.bluetooth.ConnectionStatus
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import com.example.roboracer.control.ResolvedControl
import com.example.roboracer.physics.DigitalTwinMode
import com.example.roboracer.physics.SurfacePreset
import com.example.roboracer.renderer.CarGLSurfaceRenderer
import com.example.roboracer.theme.AccentCyan
import com.example.roboracer.theme.AccentGreen
import com.example.roboracer.theme.AccentRed
import kotlinx.coroutines.delay

/**
 * Hardware-accelerated OpenGL ES 3D Vehicle Viewport with Orbit Camera,
 * Real-Time Ground Physics Simulation, and Mode Switching.
 */
@Composable
fun CarViewport(
    resolvedControl: ResolvedControl,
    telemetry: TelemetryFrame?,
    connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    isEmergencyStopped: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderer = remember { CarGLSurfaceRenderer(context.applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

    var currentMode by remember { mutableStateOf(DigitalTwinMode.SIMULATION) }
    var currentSurface by remember { mutableStateOf(SurfacePreset.SMOOTH_FLOOR) }
    var isSimPaused by remember { mutableStateOf(false) }

    // Live physics state for HUD overlay
    var simSpeed by remember { mutableStateOf(0.0f) }
    var simYawDeg by remember { mutableStateOf(0.0f) }
    var simIsColliding by remember { mutableStateOf(false) }

    // Synchronize control inputs and telemetry into 3D renderer
    LaunchedEffect(resolvedControl, telemetry, connectionStatus, isEmergencyStopped) {
        renderer.throttle = resolvedControl.normalizedThrottle
        renderer.steering = resolvedControl.normalizedSteering
        renderer.isBraking = resolvedControl.isBrakeEngaged
        renderer.leftCommandedPwm = telemetry?.leftActualSpeed ?: 0
        renderer.rightCommandedPwm = telemetry?.rightActualSpeed ?: 0
        renderer.isConnected = connectionStatus == ConnectionStatus.CONNECTED
        renderer.isFailsafe = telemetry?.isFailsafe == true || isEmergencyStopped
    }

    // Periodic HUD telemetry polling loop (~10 Hz)
    LaunchedEffect(currentMode) {
        while (true) {
            val s = renderer.physicsEngine.state
            simSpeed = s.linearSpeed
            simYawDeg = s.yawDeg
            simIsColliding = s.isColliding
            delay(100)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. OpenGL ES Surface View with gesture recognizers for Orbit, Zoom and Pan
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Wait for first finger down
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        var lastPos = firstDown.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break

                            if (pressed.size == 1) {
                                // Single-finger: orbit camera
                                val newPos = pressed[0].position
                                val delta = newPos - lastPos
                                if (delta.getDistance() > 0.5f) {
                                    renderer.orbit(delta.x * 0.45f, -delta.y * 0.35f)
                                }
                                lastPos = newPos
                            } else if (pressed.size >= 2) {
                                // Two-finger: compute centroid delta (pan) and scale (zoom)
                                val prevPositions = pressed.map { it.previousPosition }
                                val currPositions = pressed.map { it.position }

                                val prevCentroid = prevPositions.fold(Offset.Zero) { acc, o -> acc + o } / prevPositions.size.toFloat()
                                val currCentroid = currPositions.fold(Offset.Zero) { acc, o -> acc + o } / currPositions.size.toFloat()

                                val pan = currCentroid - prevCentroid
                                if (pan != Offset.Zero) {
                                    renderer.pan(-pan.x * 0.003f, pan.y * 0.003f)
                                }

                                // Compute zoom from span change
                                val prevSpan = (prevPositions[1] - prevPositions[0]).getDistance()
                                val currSpan = (currPositions[1] - currPositions[0]).getDistance()
                                if (prevSpan > 1f && currSpan > 1f) {
                                    val zoomFactor = prevSpan / currSpan
                                    renderer.zoom(zoomFactor)
                                }

                                lastPos = currCentroid
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
            update = { glSurfaceView ->
                glSurfaceViewRef = glSurfaceView
                renderer.throttle = resolvedControl.normalizedThrottle
                renderer.steering = resolvedControl.normalizedSteering
                renderer.isBraking = resolvedControl.isBrakeEngaged
                renderer.leftActualSpeed = telemetry?.leftActualSpeed ?: 0
                renderer.rightActualSpeed = telemetry?.rightActualSpeed ?: 0
            }
        )

        // 2. TOP HUD CONTROLS OVERLAY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Selector Pill
            Row(
                modifier = Modifier
                    .background(Color(0xCC090B0E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (currentMode == DigitalTwinMode.SIMULATION) AccentCyan.copy(alpha = 0.25f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            currentMode = DigitalTwinMode.SIMULATION
                            renderer.mode = DigitalTwinMode.SIMULATION
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "SIMULATED",
                        color = if (currentMode == DigitalTwinMode.SIMULATION) AccentCyan else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .background(
                            if (currentMode == DigitalTwinMode.LIVE_TELEMETRY) AccentGreen.copy(alpha = 0.25f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            currentMode = DigitalTwinMode.LIVE_TELEMETRY
                            renderer.mode = DigitalTwinMode.LIVE_TELEMETRY
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LIVE TELEMETRY",
                        color = if (currentMode == DigitalTwinMode.LIVE_TELEMETRY) AccentGreen else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // SIMULATION TOOLBAR (Surface preset, Pause, Reset)
            if (currentMode == DigitalTwinMode.SIMULATION) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Surface Preset Switcher
                    Box(
                        modifier = Modifier
                            .background(Color(0xCC12161F), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable {
                                currentSurface = if (currentSurface == SurfacePreset.SMOOTH_FLOOR) {
                                    SurfacePreset.CARPET
                                } else {
                                    SurfacePreset.SMOOTH_FLOOR
                                }
                                renderer.physicsEngine.surfacePreset = currentSurface
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (currentSurface == SurfacePreset.SMOOTH_FLOOR) "FLOOR: TILE" else "FLOOR: CARPET",
                            color = Color(0xFFD0D7DE),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Pause / Resume Button
                    Box(
                        modifier = Modifier
                            .background(Color(0xCC12161F), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable {
                                isSimPaused = renderer.physicsEngine.togglePause()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isSimPaused) "▶ RESUME" else "⏸ PAUSE",
                            color = if (isSimPaused) AccentRed else Color(0xFFD0D7DE),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Reset Pose Button
                    Box(
                        modifier = Modifier
                            .background(Color(0xCC12161F), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                            .clickable {
                                renderer.physicsEngine.reset()
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "RESET CAR",
                            color = Color(0xFFD0D7DE),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Camera Reset Button
            Box(
                modifier = Modifier
                    .background(Color(0xCC090B0E), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(6.dp))
                    .clickable {
                        renderer.resetCamera()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "RESET CAM",
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. BOTTOM-LEFT HUD TELEMETRY TELETYPE
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 12.dp)
                .background(Color(0xBB090B0E), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (currentMode == DigitalTwinMode.SIMULATION) {
                Text(
                    text = "[OFFLINE DIGITAL TWIN SIMULATION]",
                    color = AccentCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SIM SPEED: ${String.format("%.2f", simSpeed)} m/s | YAW: ${String.format("%.0f", simYawDeg)}°",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (simIsColliding) {
                    Text(
                        text = "⚠ COLLISION DETECTED (SIMULATED)",
                        color = AccentRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                val isConnected = connectionStatus == ConnectionStatus.CONNECTED
                val isFailsafe = telemetry?.isFailsafe == true || isEmergencyStopped
                val hasTelemetry = telemetry != null

                when {
                    !isConnected -> {
                        Text(
                            text = "[ESP32 DISCONNECTED]",
                            color = Color(0xFF9EABB8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "WHEELS LOCKED (NO MOTOR COMMANDS)",
                            color = Color(0xFF5B6877),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    isFailsafe -> {
                        Text(
                            text = "⚠ [FAILSAFE / E-STOP ACTIVE]",
                            color = AccentRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "MOTORS KILLED BY SAFETY INTERLOCK",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    hasTelemetry -> {
                        Text(
                            text = "[LIVE ESP32 TELEMETRY]",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "CMD PWM: L ${telemetry.leftActualSpeed}/255 | R ${telemetry.rightActualSpeed}/255",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "MOTION: COMMAND-DERIVED (NO ENCODERS)",
                            color = Color(0xFF9EABB8),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    else -> {
                        Text(
                            text = "[LIVE PREDICTION (AWAITING TELEM)]",
                            color = AccentRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PREDICTED FROM CONTROLLER COMMANDS",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> glSurfaceViewRef?.onResume()
                    Lifecycle.Event.ON_PAUSE -> glSurfaceViewRef?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                glSurfaceViewRef?.onPause()
            }
        }
    }
}
