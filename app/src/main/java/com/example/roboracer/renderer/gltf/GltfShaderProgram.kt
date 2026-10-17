package com.example.roboracer.renderer.gltf

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL ES 2.0 Shader Program for rendering glTF 2.0 models with
 * Blinn-Phong PBR-like lighting, key and fill lights, specular highlights,
 * vertex colors, and emissive materials.
 */
class GltfShaderProgram {

    companion object {
        private const val TAG = "GltfShaderProgram"

        private const val VERTEX_SHADER_SRC = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;
            
            attribute vec4 a_Position;
            attribute vec3 a_Normal;
            attribute vec4 a_Color;
            
            varying vec3 v_Position;
            varying vec3 v_Normal;
            varying vec4 v_Color;
            
            void main() {
                v_Position = vec3(u_ModelMatrix * a_Position);
                v_Normal = normalize(vec3(u_ModelMatrix * vec4(a_Normal, 0.0)));
                v_Color = a_Color;
                gl_Position = u_MVPMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER_SRC = """
            precision mediump float;
            
            varying vec3 v_Position;
            varying vec3 v_Normal;
            varying vec4 v_Color;
            
            uniform vec4 u_MaterialColor;
            uniform vec3 u_EmissiveFactor;
            uniform float u_Roughness;
            uniform float u_Metallic;
            uniform int u_HasVertexColor;
            
            uniform vec3 u_LightPos;
            uniform vec3 u_LightColor;
            uniform vec3 u_AmbientLight;
            uniform vec3 u_ViewPos;
            
            void main() {
                vec4 baseColor = u_MaterialColor;
                if (u_HasVertexColor == 1) {
                    baseColor *= v_Color;
                }
                
                // Normal & lighting calculation
                vec3 N = normalize(v_Normal);
                vec3 L = normalize(u_LightPos - v_Position);
                vec3 V = normalize(u_ViewPos - v_Position);
                vec3 H = normalize(L + V);
                
                // Key Light Diffuse (Lambertian)
                float NdotL = max(dot(N, L), 0.0);
                vec3 diffuse = u_LightColor * NdotL;
                
                // Fill Light from front-upper-left to illuminate darker chassis details
                vec3 fillLightDir = normalize(vec3(-2.0, 3.0, -3.0) - v_Position);
                float fillDiff = max(dot(N, fillLightDir), 0.0) * 0.35;
                
                // Specular reflection (Blinn-Phong) tuned by roughness & metallic
                float shininess = mix(64.0, 4.0, u_Roughness);
                float specStrength = mix(0.12, 0.75, u_Metallic);
                float spec = pow(max(dot(N, H), 0.0), shininess) * specStrength;
                
                vec3 lighting = u_AmbientLight + diffuse * 0.82 + vec3(fillDiff) + vec3(spec);
                vec3 finalRgb = baseColor.rgb * lighting + u_EmissiveFactor;
                
                gl_FragColor = vec4(finalRgb, baseColor.a);
            }
        """
    }

    var programHandle: Int = 0
        private set

    var uMVPMatrixHandle: Int = -1
        private set
    var uModelMatrixHandle: Int = -1
        private set
    var uMaterialColorHandle: Int = -1
        private set
    var uRoughnessHandle: Int = -1
        private set
    var uMetallicHandle: Int = -1
        private set
    var uEmissiveFactorHandle: Int = -1
        private set
    var uHasVertexColorHandle: Int = -1
        private set

    var uLightPosHandle: Int = -1
        private set
    var uLightColorHandle: Int = -1
        private set
    var uAmbientLightHandle: Int = -1
        private set
    var uViewPosHandle: Int = -1
        private set

    var aPositionHandle: Int = -1
        private set
    var aNormalHandle: Int = -1
        private set
    var aColorHandle: Int = -1
        private set

    init {
        compileAndLink()
    }

    private fun compileAndLink() {
        val vertShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_SRC)
        val fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC)

        programHandle = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vertShader)
            GLES20.glAttachShader(prog, fragShader)
            GLES20.glLinkProgram(prog)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(prog)
                Log.e(TAG, "Shader link error: $error")
                GLES20.glDeleteProgram(prog)
                throw RuntimeException("Failed to link glTF shader: $error")
            }
        }

        // Cache uniform locations
        uMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_ModelMatrix")
        uMaterialColorHandle = GLES20.glGetUniformLocation(programHandle, "u_MaterialColor")
        uRoughnessHandle = GLES20.glGetUniformLocation(programHandle, "u_Roughness")
        uMetallicHandle = GLES20.glGetUniformLocation(programHandle, "u_Metallic")
        uEmissiveFactorHandle = GLES20.glGetUniformLocation(programHandle, "u_EmissiveFactor")
        uHasVertexColorHandle = GLES20.glGetUniformLocation(programHandle, "u_HasVertexColor")

        uLightPosHandle = GLES20.glGetUniformLocation(programHandle, "u_LightPos")
        uLightColorHandle = GLES20.glGetUniformLocation(programHandle, "u_LightColor")
        uAmbientLightHandle = GLES20.glGetUniformLocation(programHandle, "u_AmbientLight")
        uViewPosHandle = GLES20.glGetUniformLocation(programHandle, "u_ViewPos")

        // Cache attribute locations
        aPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position")
        aNormalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal")
        aColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color")
    }

    fun use() {
        GLES20.glUseProgram(programHandle)
    }

    fun setLighting(
        lightPos: FloatArray,
        lightColor: FloatArray = floatArrayOf(1.0f, 0.98f, 0.92f),
        ambientLight: FloatArray = floatArrayOf(0.40f, 0.42f, 0.45f),
        viewPos: FloatArray = floatArrayOf(0.0f, 1.45f, 3.1f)
    ) {
        GLES20.glUniform3fv(uLightPosHandle, 1, lightPos, 0)
        GLES20.glUniform3fv(uLightColorHandle, 1, lightColor, 0)
        GLES20.glUniform3fv(uAmbientLightHandle, 1, ambientLight, 0)
        GLES20.glUniform3fv(uViewPosHandle, 1, viewPos, 0)
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compilation error: $error")
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compilation failed: $error")
            }
        }
    }
}
