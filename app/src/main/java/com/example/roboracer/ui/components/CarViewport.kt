package com.example.roboracer.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.roboracer.bluetooth.protocol.TelemetryFrame
import com.example.roboracer.control.ResolvedControl
import com.example.roboracer.renderer.CarGLSurfaceRenderer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Hardware-accelerated OpenGL ES 3D Vehicle Viewport.
 * Renders the Robo Racer 4WD chassis, electronics, and 4 independent spinning wheels
 * with perspective camera, workshop floor grid, and dynamic pitch/roll physics.
 */
@Composable
fun CarViewport(
    resolvedControl: ResolvedControl,
    telemetry: TelemetryFrame?,
    modifier: Modifier = Modifier
) {
    val renderer = remember { CarGLSurfaceRenderer() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

    // Synchronize control inputs and telemetry into 3D renderer
    LaunchedEffect(resolvedControl, telemetry) {
        renderer.throttle = resolvedControl.normalizedThrottle
        renderer.steering = resolvedControl.normalizedSteering
        renderer.isBraking = resolvedControl.isBrakeEngaged
        renderer.leftActualSpeed = telemetry?.leftActualSpeed ?: 0
        renderer.rightActualSpeed = telemetry?.rightActualSpeed ?: 0
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { glSurfaceView ->
                glSurfaceViewRef = glSurfaceView
                renderer.throttle = resolvedControl.normalizedThrottle
                renderer.steering = resolvedControl.normalizedSteering
                renderer.isBraking = resolvedControl.isBrakeEngaged
                renderer.leftActualSpeed = telemetry?.leftActualSpeed ?: 0
                renderer.rightActualSpeed = telemetry?.rightActualSpeed ?: 0
            }
        )

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
