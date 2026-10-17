package com.example.roboracer.renderer.gltf

import java.nio.Buffer
import java.nio.FloatBuffer

/**
 * PBR Material representation derived from glTF 2.0 pbrMetallicRoughness specification.
 */
data class GltfMaterial(
    val name: String,
    val baseColorFactor: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
    val metallicFactor: Float = 0.0f,
    val roughnessFactor: Float = 0.5f,
    val emissiveFactor: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f),
    val isDoubleSided: Boolean = false,
    val isTransparent: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GltfMaterial) return false
        return name == other.name &&
                baseColorFactor.contentEquals(other.baseColorFactor) &&
                metallicFactor == other.metallicFactor &&
                roughnessFactor == other.roughnessFactor &&
                emissiveFactor.contentEquals(other.emissiveFactor)
    }

    override fun hashCode(): Int = name.hashCode()
}

/**
 * Geometric Primitive containing vertex attribute buffers and index buffers.
 */
class GltfPrimitive(
    val positionBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer?,
    val colorBuffer: FloatBuffer?,
    val indexBuffer: Buffer?,
    val indexCount: Int,
    val indexType: Int, // e.g. GLES20.GL_UNSIGNED_SHORT or GLES20.GL_UNSIGNED_INT
    val vertexCount: Int,
    val material: GltfMaterial
)

/**
 * Mesh consisting of one or more primitives.
 */
class GltfMesh(
    val name: String,
    val primitives: List<GltfPrimitive>
)

/**
 * Scene Graph Node with hierarchical local and world transform matrices.
 */
class GltfNode(
    val name: String,
    val mesh: GltfMesh? = null
) {
    val children = mutableListOf<GltfNode>()
    var parent: GltfNode? = null

    // Base local transform (from glTF TRS or Matrix)
    val baseLocalMatrix = FloatArray(16).apply { Math3D.setIdentityM(this) }
    // Dynamic transformed local matrix (including real-time animations)
    val localMatrix = FloatArray(16).apply { Math3D.setIdentityM(this) }
    // Resolved world matrix (parent worldMatrix * localMatrix)
    val worldMatrix = FloatArray(16).apply { Math3D.setIdentityM(this) }

    // Dynamic animation parameters
    var animRotationXDeg = 0.0f
    var animRotationYDeg = 0.0f
    var animRotationZDeg = 0.0f
    var animPitchDeg = 0.0f
    var animRollDeg = 0.0f
    var animTranslateX = 0.0f
    var animTranslateY = 0.0f
    var animTranslateZ = 0.0f

    fun addChild(child: GltfNode) {
        children.add(child)
        child.parent = this
    }

    fun findNode(targetName: String): GltfNode? {
        if (name == targetName) return this
        for (child in children) {
            val found = child.findNode(targetName)
            if (found != null) return found
        }
        return null
    }

    fun collectAllNodes(list: MutableList<GltfNode>) {
        list.add(this)
        for (child in children) {
            child.collectAllNodes(list)
        }
    }

    /**
     * Recursively updates this node's world matrix from its parent's world matrix.
     */
    fun updateTransforms(parentWorldMatrix: FloatArray?) {
        // 1. Start with copy of base local matrix
        System.arraycopy(baseLocalMatrix, 0, localMatrix, 0, 16)

        // 2. Apply dynamic translation offset if set
        if (animTranslateX != 0.0f || animTranslateY != 0.0f || animTranslateZ != 0.0f) {
            Math3D.translateM(localMatrix, 0, animTranslateX, animTranslateY, animTranslateZ)
        }

        // 3. Apply dynamic pitch/roll (chassis physics)
        if (animPitchDeg != 0.0f) {
            Math3D.rotateM(localMatrix, 0, animPitchDeg, 1.0f, 0.0f, 0.0f)
        }
        if (animRollDeg != 0.0f) {
            Math3D.rotateM(localMatrix, 0, animRollDeg, 0.0f, 0.0f, 1.0f)
        }

        // 4. Apply dynamic wheel rolling rotation (rotation around local X axis)
        if (animRotationXDeg != 0.0f) {
            Math3D.rotateM(localMatrix, 0, animRotationXDeg, 1.0f, 0.0f, 0.0f)
        }
        if (animRotationYDeg != 0.0f) {
            Math3D.rotateM(localMatrix, 0, animRotationYDeg, 0.0f, 1.0f, 0.0f)
        }
        if (animRotationZDeg != 0.0f) {
            Math3D.rotateM(localMatrix, 0, animRotationZDeg, 0.0f, 0.0f, 1.0f)
        }

        // 5. Concatenate with parent world matrix
        if (parentWorldMatrix != null) {
            Math3D.multiplyMM(worldMatrix, 0, parentWorldMatrix, 0, localMatrix, 0)
        } else {
            System.arraycopy(localMatrix, 0, worldMatrix, 0, 16)
        }

        // 6. Propagate to children
        for (child in children) {
            child.updateTransforms(worldMatrix)
        }
    }
}
