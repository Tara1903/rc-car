package com.example.roboracer.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CarGLSurfaceRenderer : GLSurfaceView.Renderer {

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

    private var carModel: RoboRacer3DModel? = null

    // Real-time animation inputs
    @Volatile var throttle: Float = 0f
    @Volatile var steering: Float = 0f
    @Volatile var isBraking: Boolean = false
    @Volatile var leftActualSpeed: Int = 0
    @Volatile var rightActualSpeed: Int = 0

    private var wheelAngleLeft: Float = 0f
    private var wheelAngleRight: Float = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.035f, 0.043f, 0.055f, 1.0f) // #090B0E Dark futuristic workshop background
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Compile Shaders
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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / (if (height == 0) 1f else height.toFloat())
        // 40 degree FOV rear perspective
        Matrix.perspectiveM(projectionMatrix, 0, 40.0f, aspect, 0.5f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val model = carModel ?: return
        GLES20.glUseProgram(programHandle)

        // Light position in world space (Top-rear-right key light)
        GLES20.glUniform3f(uLightPosHandle, 2.5f, 4.5f, 3.5f)

        // Camera View: Looking at car from rear-upper perspective
        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 1.45f, 3.1f, // Eye position
            0.0f, 0.15f, 0.0f, // Look at target
            0.0f, 1.0f, 0.0f   // Up vector
        )

        // 1. Draw Perspective Workshop Floor Grid (World space, static ground)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        drawMesh(model.groundGrid, modelMatrix, mvpMatrix)

        // 2. Draw Dynamic Chassis Neon Underglow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        drawMesh(model.underglowQuad, modelMatrix, mvpMatrix)

        // 3. Wheel Spin Animation
        val spinSpeedLeft = if (leftActualSpeed > 0) (leftActualSpeed / 255f) * 16f else (throttle * 14f)
        val spinSpeedRight = if (rightActualSpeed > 0) (rightActualSpeed / 255f) * 16f else (throttle * 14f)
        wheelAngleLeft -= spinSpeedLeft
        wheelAngleRight -= spinSpeedRight

        // 4. Chassis Dynamics (Pitch & Roll transforms)
        val pitchDeg = when {
            isBraking -> 3.2f       // Forward dive under braking
            throttle > 0 -> -2.6f    // Rear squat under acceleration
            throttle < 0 -> 1.8f     // Reverse dive
            else -> 0f
        }
        val rollDeg = steering * 2.8f // Roll into corner

        Matrix.setIdentityM(chassisMatrix, 0)
        Matrix.translateM(chassisMatrix, 0, 0f, 0f, 0f)
        Matrix.rotateM(chassisMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(chassisMatrix, 0, rollDeg, 0f, 0f, 1f)

        // Draw All Chassis Body Meshes
        for (mesh in model.bodyMeshes) {
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, chassisMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            drawMesh(mesh, chassisMatrix, mvpMatrix)
        }

        // 5. Draw 4x Wheels with Independent Spin Rotations
        // Rear Left Wheel
        drawWheel(model.wheelRL, model.wheelRLPos, wheelAngleLeft)
        // Rear Right Wheel
        drawWheel(model.wheelRR, model.wheelRRPos, wheelAngleRight)
        // Front Left Wheel
        drawWheel(model.wheelFL, model.wheelFLPos, wheelAngleLeft)
        // Front Right Wheel
        drawWheel(model.wheelFR, model.wheelFRPos, wheelAngleRight)
    }

    private fun drawWheel(wheelMesh: Mesh3D, pos: FloatArray, spinAngle: Float) {
        Matrix.setIdentityM(wheelMatrix, 0)
        // Parented to chassis pitch & roll
        Matrix.multiplyMM(wheelMatrix, 0, chassisMatrix, 0, wheelMatrix, 0)
        // Translate to wheel hub position
        Matrix.translateM(wheelMatrix, 0, pos[0], pos[1], pos[2])
        // Rotate along axle (X axis)
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
