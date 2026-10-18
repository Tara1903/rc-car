package com.example.roboracer.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.example.roboracer.physics.DigitalTwinMode
import com.example.roboracer.physics.VehiclePhysicsEngine
import com.example.roboracer.renderer.gltf.GltfModel
import com.example.roboracer.renderer.gltf.GltfParser
import com.example.roboracer.renderer.gltf.GltfShaderProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * OpenGL ES 2.0 Renderer displaying the Robo Racer 4WD vehicle.
 * Supports:
 * - Mode A (Digital Twin Simulation): Real-time physics engine, ground navigation, obstacle collisions.
 * - Mode B (Live Vehicle Visualization): Real-time telemetry reflection and command prediction.
 * - Orbit camera controls: 1-finger orbit, 2-finger zoom and pan.
 * - glTF 2.0 PBR vehicle model with fallback to procedural geometry.
 */
class CarGLSurfaceRenderer(var context: Context? = null) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Car3DRenderer"

        private const val VERTEX_SHADER_SRC = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;
            attribute vec4 a_Position;
            attribute vec3 a_Normal;
            attribute vec4 a_Color;
            varying vec4 v_Color;
            varying vec3 v_Normal;
            varying vec3 v_Position;
            void main() {
                v_Position = vec3(u_ModelMatrix * a_Position);
                v_Normal = vec3(u_ModelMatrix * vec4(a_Normal, 0.0));
                v_Color = a_Color;
                gl_Position = u_MVPMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER_SRC = """
            precision mediump float;
            varying vec4 v_Color;
            varying vec3 v_Normal;
            varying vec3 v_Position;
            uniform vec3 u_LightPos;
            void main() {
                vec3 lightDir = normalize(u_LightPos - v_Position);
                vec3 normal = normalize(v_Normal);
                float diff = max(dot(normal, lightDir), 0.0);
                float ambient = 0.44;
                float lighting = min(ambient + diff * 0.62, 1.0);
                gl_FragColor = vec4(v_Color.rgb * lighting, v_Color.a);
            }
        """
    }

    private var programHandle = 0
    private var uMVPMatrixHandle = 0
    private var uModelMatrixHandle = 0
    private var uLightPosHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0
    private var aColorHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val chassisMatrix = FloatArray(16)
    private val wheelMatrix = FloatArray(16)

    // Fallback procedural geometry, ground grid and obstacle meshes
    private var carModel: RoboRacer3DModel? = null
    private val obstacleMeshes = mutableListOf<Mesh3D>()

    // Detailed glTF 2.0 model & shader
    var gltfModel: GltfModel? = null
        private set
    private var gltfShader: GltfShaderProgram? = null
    private var lastFrameTimeNs: Long = 0L

    // Physics Engine & Operational Modes
    val physicsEngine = VehiclePhysicsEngine()
    @Volatile var mode: DigitalTwinMode = DigitalTwinMode.SIMULATION

    // Spherical Orbit Camera Parameters
    @Volatile var cameraDistance: Float = 2.85f
    @Volatile var cameraAzimuthDeg: Float = 0.0f
    @Volatile var cameraElevationDeg: Float = 32.0f
    @Volatile var cameraTargetX: Float = 0.0f
    @Volatile var cameraTargetY: Float = 0.15f
    @Volatile var cameraTargetZ: Float = 0.0f

    // Real-time animation inputs
    @Volatile var throttle: Float = 0f
    @Volatile var steering: Float = 0f
    @Volatile var isBraking: Boolean = false
    @Volatile var leftActualSpeed: Int = 0
    @Volatile var rightActualSpeed: Int = 0

    private var wheelAngleLeft: Float = 0f
    private var wheelAngleRight: Float = 0f

    fun orbit(deltaAzimuthDeg: Float, deltaElevationDeg: Float) {
        cameraAzimuthDeg = (cameraAzimuthDeg + deltaAzimuthDeg) % 360.0f
        cameraElevationDeg = (cameraElevationDeg + deltaElevationDeg).coerceIn(10.0f, 85.0f)
    }

    fun zoom(factor: Float) {
        cameraDistance = (cameraDistance * factor).coerceIn(1.2f, 6.5f)
    }

    fun pan(deltaX: Float, deltaZ: Float) {
        cameraTargetX += deltaX
        cameraTargetZ += deltaZ
    }

    fun resetCamera() {
        cameraDistance = 2.85f
        cameraAzimuthDeg = 0.0f
        cameraElevationDeg = 32.0f
        cameraTargetX = 0.0f
        cameraTargetY = 0.15f
        cameraTargetZ = 0.0f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.035f, 0.043f, 0.055f, 1.0f) // #090B0E Dark futuristic workshop background
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Compile legacy shader for workshop grid, obstacles & underglow quad (and fallback)
        val vertShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SRC)
        val fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC)
        programHandle = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertShader)
            GLES20.glAttachShader(it, fragShader)
            GLES20.glLinkProgram(it)
        }

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_ModelMatrix")
        uLightPosHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos")
        aPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position")
        aNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal")
        aColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color")

        carModel = RoboRacer3DModel()

        // Create 3D Meshes for Arena Obstacles
        obstacleMeshes.clear()
        for (obs in physicsEngine.obstacles) {
            val w = obs.maxX - obs.minX
            val d = obs.maxZ - obs.minZ
            val h = 0.32f
            val cx = (obs.minX + obs.maxX) / 2.0f
            val cz = (obs.minZ + obs.maxZ) / 2.0f
            val cy = -0.32f + (h / 2.0f) // Rest on ground plane (y = -0.32)
            obstacleMeshes.add(
                Mesh3D.createBox(
                    w = w, h = h, d = d,
                    r = 0.88f, g = 0.42f, b = 0.12f, a = 0.85f, // Industrial safety orange
                    tx = cx, ty = cy, tz = cz
                )
            )
        }

        // Load realistic glTF 2.0 Robo Racer asset from APK assets
        context?.let { ctx ->
            try {
                ctx.assets.open("models/robo_racer_car.glb").use { stream ->
                    gltfModel = GltfParser.parse(stream)
                    gltfShader = GltfShaderProgram()
                    Log.i(TAG, "Robo Racer glTF model loaded successfully (${gltfModel?.allNodes?.size} nodes)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load robo_racer_car.glb, falling back to procedural model", e)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / (if (height == 0) 1f else height.toFloat())
        // 42 degree FOV perspective camera
        Matrix.perspectiveM(projectionMatrix, 0, 42.0f, aspect, 0.4f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val model = carModel ?: return

        // 1. Advance Physics or Real-Time Animation
        val now = System.nanoTime()
        val dt = if (lastFrameTimeNs > 0L) {
            ((now - lastFrameTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        } else {
            0.016f
        }
        lastFrameTimeNs = now

        val pState = if (mode == DigitalTwinMode.SIMULATION) {
            physicsEngine.step(dt, throttle, steering, isBraking)
        } else {
            physicsEngine.state
        }

        // 2. Compute Camera View Matrix using Spherical Orbit Coordinates
        val azRad = Math.toRadians(cameraAzimuthDeg.toDouble()).toFloat()
        val elRad = Math.toRadians(cameraElevationDeg.toDouble()).toFloat()

        val currentTargetX = (if (mode == DigitalTwinMode.SIMULATION) pState.x else 0.0f) + cameraTargetX
        val currentTargetY = cameraTargetY
        val currentTargetZ = (if (mode == DigitalTwinMode.SIMULATION) pState.z else 0.0f) + cameraTargetZ

        val cosEl = cos(elRad)
        val sinEl = sin(elRad)
        val cosAz = cos(azRad)
        val sinAz = sin(azRad)

        val eyeX = currentTargetX + cameraDistance * cosEl * sinAz
        val eyeY = currentTargetY + cameraDistance * sinEl
        val eyeZ = currentTargetZ + cameraDistance * cosEl * cosAz

        Matrix.setLookAtM(
            viewMatrix, 0,
            eyeX, eyeY, eyeZ,
            currentTargetX, currentTargetY, currentTargetZ,
            0.0f, 1.0f, 0.0f
        )

        // 3. Render Static Ground Grid & Obstacles
        GLES20.glUseProgram(programHandle)
        GLES20.glUniform3f(uLightPosHandle, 2.5f + currentTargetX, 4.5f, 3.5f + currentTargetZ)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        drawMesh(model.groundGrid, modelMatrix, mvpMatrix)

        // Render Arena Obstacle Boxes in simulation mode
        if (mode == DigitalTwinMode.SIMULATION) {
            for (obsMesh in obstacleMeshes) {
                drawMesh(obsMesh, modelMatrix, mvpMatrix)
            }
        }

        // 4. Render Dynamic Neon Underglow Quad
        Matrix.setIdentityM(modelMatrix, 0)
        if (mode == DigitalTwinMode.SIMULATION) {
            Matrix.translateM(modelMatrix, 0, pState.x, 0.0f, pState.z)
        }
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        drawMesh(model.underglowQuad, modelMatrix, mvpMatrix)

        // 5. Render Robo Racer Vehicle (glTF 2.0 or Procedural Fallback)
        val gModel = gltfModel
        val gShader = gltfShader
        if (gModel != null && gShader != null) {
            if (mode == DigitalTwinMode.SIMULATION) {
                // Apply dynamic 2D ground translation and yaw rotation to car_root
                gModel.carRoot?.animTranslateX = pState.x
                gModel.carRoot?.animTranslateZ = pState.z
                gModel.carRoot?.animRotationYDeg = pState.yawDeg

                // Pitch squat/dive & roll compliance
                gModel.chassisRoot?.animPitchDeg = pState.pitchDeg
                gModel.chassisRoot?.animRollDeg = pState.rollDeg

                // 4-wheel independent spin from physics simulation
                gModel.wheelFL?.animRotationXDeg = -pState.wheelAngleL
                gModel.wheelRL?.animRotationXDeg = -pState.wheelAngleL
                gModel.wheelFR?.animRotationXDeg = -pState.wheelAngleR
                gModel.wheelRR?.animRotationXDeg = -pState.wheelAngleR

                gModel.updateTransforms()
            } else {
                // Mode B: Live Telemetry
                gModel.carRoot?.animTranslateX = 0.0f
                gModel.carRoot?.animTranslateZ = 0.0f
                gModel.carRoot?.animRotationYDeg = 0.0f
                gModel.updateAnimation(
                    throttle = throttle,
                    steering = steering,
                    isBraking = isBraking,
                    leftPwm = leftActualSpeed,
                    rightPwm = rightActualSpeed,
                    dtSeconds = dt
                )
            }

            // Render glTF hierarchy with PBR lighting
            gShader.use()
            gShader.setLighting(
                lightPos = floatArrayOf(2.5f + currentTargetX, 4.5f, 3.5f + currentTargetZ),
                viewPos = floatArrayOf(eyeX, eyeY, eyeZ)
            )
            gModel.render(viewMatrix, projectionMatrix, gShader, isBraking)
        } else {
            // Procedural Fallback
            val carX = if (mode == DigitalTwinMode.SIMULATION) pState.x else 0.0f
            val carZ = if (mode == DigitalTwinMode.SIMULATION) pState.z else 0.0f
            val carYaw = if (mode == DigitalTwinMode.SIMULATION) pState.yawDeg else 0.0f
            val pitchDeg = if (mode == DigitalTwinMode.SIMULATION) pState.pitchDeg else (if (isBraking) 3.2f else if (throttle > 0) -2.6f else 0.0f)
            val rollDeg = if (mode == DigitalTwinMode.SIMULATION) pState.rollDeg else (steering * 2.8f)

            if (mode == DigitalTwinMode.SIMULATION) {
                wheelAngleLeft = -pState.wheelAngleL
                wheelAngleRight = -pState.wheelAngleR
            } else {
                val spinSpeedLeft = if (leftActualSpeed > 0) (leftActualSpeed / 255f) * 16f else (throttle * 14f)
                val spinSpeedRight = if (rightActualSpeed > 0) (rightActualSpeed / 255f) * 16f else (throttle * 14f)
                wheelAngleLeft -= spinSpeedLeft
                wheelAngleRight -= spinSpeedRight
            }

            Matrix.setIdentityM(chassisMatrix, 0)
            Matrix.translateM(chassisMatrix, 0, carX, 0f, carZ)
            Matrix.rotateM(chassisMatrix, 0, carYaw, 0f, 1f, 0f)
            Matrix.rotateM(chassisMatrix, 0, pitchDeg, 1f, 0f, 0f)
            Matrix.rotateM(chassisMatrix, 0, rollDeg, 0f, 0f, 1f)

            // Draw Chassis Meshes
            for (mesh in model.bodyMeshes) {
                Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, chassisMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
                drawMesh(mesh, chassisMatrix, mvpMatrix)
            }

            // Draw 4x Wheels
            drawWheel(model.wheelRL, model.wheelRLPos, wheelAngleLeft)
            drawWheel(model.wheelRR, model.wheelRRPos, wheelAngleRight)
            drawWheel(model.wheelFL, model.wheelFLPos, wheelAngleLeft)
            drawWheel(model.wheelFR, model.wheelFRPos, wheelAngleRight)
        }
    }

    private fun drawWheel(wheelMesh: Mesh3D, pos: FloatArray, spinAngle: Float) {
        Matrix.setIdentityM(wheelMatrix, 0)
        Matrix.multiplyMM(wheelMatrix, 0, chassisMatrix, 0, wheelMatrix, 0)
        Matrix.translateM(wheelMatrix, 0, pos[0], pos[1], pos[2])
        Matrix.rotateM(wheelMatrix, 0, spinAngle, 1f, 0f, 0f)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, wheelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        drawMesh(wheelMesh, wheelMatrix, mvpMatrix)
    }

    private fun drawMesh(mesh: Mesh3D, mMat: FloatArray, mvpMat: FloatArray) {
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, mMat, 0)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMat, 0)

        mesh.vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, Mesh3D.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        mesh.vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, Mesh3D.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aNormalHandle)

        mesh.vertexBuffer.position(6)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, Mesh3D.STRIDE, mesh.vertexBuffer)
        GLES20.glEnableVertexAttribArray(aColorHandle)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
        GLES20.glDisableVertexAttribArray(aColorHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Shader compile error: " + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
            }
        }
    }
}
