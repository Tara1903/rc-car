package com.example.roboracer.renderer.gltf

import android.opengl.GLES20
import kotlin.math.abs
import kotlin.math.exp

/**
 * Encapsulates the loaded glTF 3D model, its scene graph nodes, and real-time animation engine.
 */
class GltfModel(
    val rootNode: GltfNode,
    val allNodes: List<GltfNode>,
    val materials: List<GltfMaterial>
) {
    // Fast lookup for named animation pivots
    private val nodeMap: Map<String, GltfNode> = allNodes.associateBy { it.name }

    val carRoot: GltfNode? = nodeMap["car_root"]
    val chassisRoot: GltfNode? = nodeMap["chassis_root"]
    val wheelFL: GltfNode? = nodeMap["front_left_wheel_pivot"]
    val wheelFR: GltfNode? = nodeMap["front_right_wheel_pivot"]
    val wheelRL: GltfNode? = nodeMap["rear_left_wheel_pivot"]
    val wheelRR: GltfNode? = nodeMap["rear_right_wheel_pivot"]

    // Real-time animation state
    private var leftWheelAngleDeg = 0.0f
    private var rightWheelAngleDeg = 0.0f
    private var currentPitchDeg = 0.0f
    private var currentRollDeg = 0.0f

    // Scale factor to comfortably frame the 0.22m physical car in the central viewport
    var modelScale: Float = 7.2f

    private val tempMvpMatrix = FloatArray(16)
    private val opaqueDrawables = mutableListOf<Pair<GltfNode, GltfPrimitive>>()
    private val transparentDrawables = mutableListOf<Pair<GltfNode, GltfPrimitive>>()

    init {
        // Apply initial model scaling to car_root and offset so wheels rest on the ground plane (y = -0.32f)
        carRoot?.let {
            Math3D.translateM(it.baseLocalMatrix, 0, 0.0f, -0.32f, 0.0f)
            Math3D.scaleM(it.baseLocalMatrix, 0, modelScale, modelScale, modelScale)
        }
        updateTransforms()

        // Cache drawables once at initialization
        val allDrawables = mutableListOf<Pair<GltfNode, GltfPrimitive>>()
        collectDrawables(rootNode, allDrawables)
        opaqueDrawables.addAll(allDrawables.filter { !it.second.material.isTransparent })
        transparentDrawables.addAll(allDrawables.filter { it.second.material.isTransparent })
    }

    fun findNode(name: String): GltfNode? = nodeMap[name]

    /**
     * Updates real-time physics and wheel rotations driven by controller inputs & telemetry.
     */
    fun updateAnimation(
        throttle: Float,
        steering: Float,
        isBraking: Boolean,
        leftPwm: Int,
        rightPwm: Int,
        dtSeconds: Float
    ) {
        val dt = dtSeconds.coerceIn(0.001f, 0.1f)

        // 1. Calculate Left and Right Wheel Angular Velocities
        // NOTE: In Mode B (Live Telemetry), leftPwm and rightPwm originate from the ESP32
        // firmware ramp generator as commanded PWM duty cycles [0..255], NOT encoder feedback.
        val leftFactor = when {
            isBraking && throttle >= 0.0f -> 0.0f
            leftPwm > 0 -> (leftPwm / 255.0f) * (if (throttle < 0) -1.0f else 1.0f)
            abs(throttle) < 0.05f && abs(steering) > 0.05f -> steering.coerceIn(-1.0f, 1.0f)
            else -> (throttle + steering * 0.40f).coerceIn(-1.0f, 1.0f)
        }

        val rightFactor = when {
            isBraking && throttle >= 0.0f -> 0.0f
            rightPwm > 0 -> (rightPwm / 255.0f) * (if (throttle < 0) -1.0f else 1.0f)
            abs(throttle) < 0.05f && abs(steering) > 0.05f -> (-steering).coerceIn(-1.0f, 1.0f)
            else -> (throttle - steering * 0.40f).coerceIn(-1.0f, 1.0f)
        }

        // Full speed wheel rotation rate (~720 deg/sec at 100% duty cycle)
        // Rolling forward corresponds to negative rotation around local X axis
        val maxRotSpeed = 720.0f
        leftWheelAngleDeg -= leftFactor * maxRotSpeed * dt
        rightWheelAngleDeg -= rightFactor * maxRotSpeed * dt

        // Keep angles normalized within [0, 360)
        leftWheelAngleDeg %= 360.0f
        rightWheelAngleDeg %= 360.0f

        // Apply rotation around wheel local X axis
        wheelFL?.animRotationXDeg = leftWheelAngleDeg
        wheelRL?.animRotationXDeg = leftWheelAngleDeg
        wheelFR?.animRotationXDeg = rightWheelAngleDeg
        wheelRR?.animRotationXDeg = rightWheelAngleDeg

        // 2. Chassis Simulated Body-Response (Inertial Visual Cues)
        // Rigid acrylic car has no physical suspension springs. Body tilt is a visual feedback cue.
        val targetPitch = when {
            isBraking -> 3.2f        // Forward pitch/dive under braking
            throttle > 0 -> -2.6f     // Rear squat under acceleration
            throttle < 0 -> 1.8f      // Mild dive during reverse
            else -> 0.0f
        }

        val targetRoll = when {
            abs(steering) > 0.05f -> -2.4f * steering * (abs(throttle).coerceAtLeast(0.3f))
            else -> 0.0f
        }

        // Smooth exponential spring damping towards target
        val pitchDamp = (1.0f - exp(-10.0f * dt)).coerceIn(0.0f, 1.0f)
        val rollDamp = (1.0f - exp(-10.0f * dt)).coerceIn(0.0f, 1.0f)
        val rawPitch = currentPitchDeg + (targetPitch - currentPitchDeg) * pitchDamp
        val rawRoll = currentRollDeg + (targetRoll - currentRollDeg) * rollDamp

        currentPitchDeg = if (rawPitch.isNaN() || rawPitch.isInfinite()) 0.0f else rawPitch.coerceIn(-5.0f, 5.0f)
        currentRollDeg = if (rawRoll.isNaN() || rawRoll.isInfinite()) 0.0f else rawRoll.coerceIn(-5.0f, 5.0f)

        chassisRoot?.animPitchDeg = currentPitchDeg
        chassisRoot?.animRollDeg = currentRollDeg

        // 3. Update the Scene Graph Transforms
        updateTransforms()
    }

    fun updateTransforms() {
        rootNode.updateTransforms(null)
    }

    /**
     * Renders all opaque and transparent primitives across the scene graph.
     */
    fun render(
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        shader: GltfShaderProgram,
        isBraking: Boolean
    ) {
        // Pass 1: Opaque
        for ((node, prim) in opaqueDrawables) {
            drawPrimitive(node, prim, viewMatrix, projMatrix, tempMvpMatrix, shader, isBraking)
        }

        // Pass 2: Transparent (with depth write disabled or blending enabled)
        for ((node, prim) in transparentDrawables) {
            drawPrimitive(node, prim, viewMatrix, projMatrix, tempMvpMatrix, shader, isBraking)
        }

        // Clean up vertex attribute state
        if (shader.aPositionHandle >= 0) GLES20.glDisableVertexAttribArray(shader.aPositionHandle)
        if (shader.aNormalHandle >= 0) GLES20.glDisableVertexAttribArray(shader.aNormalHandle)
        if (shader.aColorHandle >= 0) GLES20.glDisableVertexAttribArray(shader.aColorHandle)
    }

    private fun collectDrawables(node: GltfNode, list: MutableList<Pair<GltfNode, GltfPrimitive>>) {
        node.mesh?.primitives?.forEach { prim ->
            list.add(node to prim)
        }
        for (child in node.children) {
            collectDrawables(child, list)
        }
    }

    private fun drawPrimitive(
        node: GltfNode,
        prim: GltfPrimitive,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        mvpMatrix: FloatArray,
        shader: GltfShaderProgram,
        isBraking: Boolean
    ) {
        // Calculate Model-View-Projection Matrix
        Math3D.multiplyMM(mvpMatrix, 0, viewMatrix, 0, node.worldMatrix, 0)
        Math3D.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvpMatrix, 0)

        // Set MVP and Model uniforms
        GLES20.glUniformMatrix4fv(shader.uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrixHandle, 1, false, node.worldMatrix, 0)

        // Set Material Uniforms
        val mat = prim.material
        GLES20.glUniform4fv(shader.uMaterialColorHandle, 1, mat.baseColorFactor, 0)
        GLES20.glUniform1f(shader.uRoughnessHandle, mat.roughnessFactor)
        GLES20.glUniform1f(shader.uMetallicHandle, mat.metallicFactor)

        // Emissive: Boost rear brake lights when braking is active
        val emissive = FloatArray(3)
        System.arraycopy(mat.emissiveFactor, 0, emissive, 0, 3)
        if (isBraking && (node.name.contains("brake_light") || node.name.contains("tail_light") ||
                mat.name.contains("BrakeLight") || mat.name.contains("TailLight"))) {
            emissive[0] = 1.0f
            emissive[1] = 0.05f
            emissive[2] = 0.05f
        }
        GLES20.glUniform3fv(shader.uEmissiveFactorHandle, 1, emissive, 0)

        // Bind Position Buffer
        prim.positionBuffer.position(0)
        GLES20.glEnableVertexAttribArray(shader.aPositionHandle)
        GLES20.glVertexAttribPointer(shader.aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, prim.positionBuffer)

        // Bind Normal Buffer
        if (prim.normalBuffer != null && shader.aNormalHandle >= 0) {
            prim.normalBuffer.position(0)
            GLES20.glEnableVertexAttribArray(shader.aNormalHandle)
            GLES20.glVertexAttribPointer(shader.aNormalHandle, 3, GLES20.GL_FLOAT, false, 0, prim.normalBuffer)
        } else if (shader.aNormalHandle >= 0) {
            GLES20.glDisableVertexAttribArray(shader.aNormalHandle)
        }

        // Bind Color Buffer
        if (prim.colorBuffer != null && shader.aColorHandle >= 0) {
            prim.colorBuffer.position(0)
            GLES20.glEnableVertexAttribArray(shader.aColorHandle)
            GLES20.glVertexAttribPointer(shader.aColorHandle, 4, GLES20.GL_FLOAT, false, 0, prim.colorBuffer)
            GLES20.glUniform1i(shader.uHasVertexColorHandle, 1)
        } else {
            if (shader.aColorHandle >= 0) {
                GLES20.glDisableVertexAttribArray(shader.aColorHandle)
            }
            GLES20.glUniform1i(shader.uHasVertexColorHandle, 0)
        }

        // Draw Elements or Arrays
        if (prim.indexBuffer != null && prim.indexCount > 0) {
            prim.indexBuffer.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, prim.indexCount, prim.indexType, prim.indexBuffer)
        } else {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, prim.vertexCount)
        }
    }
}
