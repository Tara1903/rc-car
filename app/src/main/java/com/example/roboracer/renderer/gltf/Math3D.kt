package com.example.roboracer.renderer.gltf

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Pure Kotlin 4x4 column-major matrix and 3D vector operations.
 * Compatible with OpenGL ES 2.0/3.0 column-major conventions.
 * Runs seamlessly on both Android devices (ART) and JVM host unit tests.
 */
object Math3D {

    fun setIdentityM(m: FloatArray, offset: Int = 0) {
        for (i in 0 until 16) {
            m[offset + i] = if (i % 5 == 0) 1.0f else 0.0f
        }
    }

    fun multiplyMM(result: FloatArray, rOffset: Int, lhs: FloatArray, lOffset: Int, rhs: FloatArray, rOffsetIn: Int) {
        for (col in 0 until 4) {
            val rhsCol = rOffsetIn + col * 4
            val rhs0 = rhs[rhsCol]
            val rhs1 = rhs[rhsCol + 1]
            val rhs2 = rhs[rhsCol + 2]
            val rhs3 = rhs[rhsCol + 3]

            result[rOffset + col * 4] =
                lhs[lOffset] * rhs0 + lhs[lOffset + 4] * rhs1 + lhs[lOffset + 8] * rhs2 + lhs[lOffset + 12] * rhs3
            result[rOffset + col * 4 + 1] =
                lhs[lOffset + 1] * rhs0 + lhs[lOffset + 5] * rhs1 + lhs[lOffset + 9] * rhs2 + lhs[lOffset + 13] * rhs3
            result[rOffset + col * 4 + 2] =
                lhs[lOffset + 2] * rhs0 + lhs[lOffset + 6] * rhs1 + lhs[lOffset + 10] * rhs2 + lhs[lOffset + 14] * rhs3
            result[rOffset + col * 4 + 3] =
                lhs[lOffset + 3] * rhs0 + lhs[lOffset + 7] * rhs1 + lhs[lOffset + 11] * rhs2 + lhs[lOffset + 15] * rhs3
        }
    }

    fun translateM(m: FloatArray, offset: Int, x: Float, y: Float, z: Float) {
        for (i in 0 until 4) {
            val mi = offset + i
            m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z
        }
    }

    fun scaleM(m: FloatArray, offset: Int, x: Float, y: Float, z: Float) {
        for (i in 0 until 4) {
            val mi = offset + i
            m[mi] *= x
            m[4 + mi] *= y
            m[8 + mi] *= z
        }
    }

    fun rotateM(m: FloatArray, offset: Int, a: Float, x: Float, y: Float, z: Float) {
        val temp = FloatArray(32)
        setRotateM(temp, 0, a, x, y, z)
        multiplyMM(temp, 16, m, offset, temp, 0)
        System.arraycopy(temp, 16, m, offset, 16)
    }

    fun setRotateM(rm: FloatArray, rmOffset: Int, a: Float, x: Float, y: Float, z: Float) {
        var nx = x
        var ny = y
        var nz = z
        val rad = a * (Math.PI.toFloat() / 180.0f)
        val s = sin(rad.toDouble()).toFloat()
        val c = cos(rad.toDouble()).toFloat()
        if (nx != 0.0f && ny == 0.0f && nz == 0.0f) {
            setIdentityM(rm, rmOffset)
            if (nx < 0.0f) {
                rm[rmOffset + 5] = c
                rm[rmOffset + 6] = s
                rm[rmOffset + 9] = -s
                rm[rmOffset + 10] = c
            } else {
                rm[rmOffset + 5] = c
                rm[rmOffset + 6] = -s
                rm[rmOffset + 9] = s
                rm[rmOffset + 10] = c
            }
            return
        }
        if (nx == 0.0f && ny != 0.0f && nz == 0.0f) {
            setIdentityM(rm, rmOffset)
            if (ny < 0.0f) {
                rm[rmOffset] = c
                rm[rmOffset + 2] = -s
                rm[rmOffset + 8] = s
                rm[rmOffset + 10] = c
            } else {
                rm[rmOffset] = c
                rm[rmOffset + 2] = s
                rm[rmOffset + 8] = -s
                rm[rmOffset + 10] = c
            }
            return
        }
        if (nx == 0.0f && ny == 0.0f && nz != 0.0f) {
            setIdentityM(rm, rmOffset)
            if (nz < 0.0f) {
                rm[rmOffset] = c
                rm[rmOffset + 1] = s
                rm[rmOffset + 4] = -s
                rm[rmOffset + 5] = c
            } else {
                rm[rmOffset] = c
                rm[rmOffset + 1] = -s
                rm[rmOffset + 4] = s
                rm[rmOffset + 5] = c
            }
            return
        }
        val len = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        if (len != 1.0f && len != 0.0f) {
            val recipLen = 1.0f / len
            nx *= recipLen
            ny *= recipLen
            nz *= recipLen
        }
        val nc = 1.0f - c
        val xy = nx * ny
        val yz = ny * nz
        val zx = nz * nx
        val xs = nx * s
        val ys = ny * s
        val zs = nz * s
        rm[rmOffset] = nx * nx * nc + c
        rm[rmOffset + 4] = xy * nc - zs
        rm[rmOffset + 8] = zx * nc + ys
        rm[rmOffset + 12] = 0.0f
        rm[rmOffset + 1] = xy * nc + zs
        rm[rmOffset + 5] = ny * ny * nc + c
        rm[rmOffset + 9] = yz * nc - xs
        rm[rmOffset + 13] = 0.0f
        rm[rmOffset + 2] = zx * nc - ys
        rm[rmOffset + 6] = yz * nc + xs
        rm[rmOffset + 10] = nz * nz * nc + c
        rm[rmOffset + 14] = 0.0f
        rm[rmOffset + 3] = 0.0f
        rm[rmOffset + 7] = 0.0f
        rm[rmOffset + 11] = 0.0f
        rm[rmOffset + 15] = 1.0f
    }

    fun perspectiveM(m: FloatArray, offset: Int, fovy: Float, aspect: Float, zNear: Float, zFar: Float) {
        val f = 1.0f / tan(fovy * (Math.PI / 360.0)).toFloat()
        val rangeReciprocal = 1.0f / (zNear - zFar)

        setIdentityM(m, offset)
        m[offset] = f / aspect
        m[offset + 5] = f
        m[offset + 10] = (zFar + zNear) * rangeReciprocal
        m[offset + 11] = -1.0f
        m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal
        m[offset + 15] = 0.0f
    }

    fun setLookAtM(
        rm: FloatArray, rmOffset: Int,
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float
    ) {
        var fx = centerX - eyeX
        var fy = centerY - eyeY
        var fz = centerZ - eyeZ
        val rlf = 1.0f / sqrt((fx * fx + fy * fy + fz * fz).toDouble()).toFloat()
        fx *= rlf
        fy *= rlf
        fz *= rlf

        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX
        val rls = 1.0f / sqrt((sx * sx + sy * sy + sz * sz).toDouble()).toFloat()
        sx *= rls
        sy *= rls
        sz *= rls

        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        rm[rmOffset] = sx
        rm[rmOffset + 1] = ux
        rm[rmOffset + 2] = -fx
        rm[rmOffset + 3] = 0.0f

        rm[rmOffset + 4] = sy
        rm[rmOffset + 5] = uy
        rm[rmOffset + 6] = -fy
        rm[rmOffset + 7] = 0.0f

        rm[rmOffset + 8] = sz
        rm[rmOffset + 9] = uz
        rm[rmOffset + 10] = -fz
        rm[rmOffset + 11] = 0.0f

        rm[rmOffset + 12] = 0.0f
        rm[rmOffset + 13] = 0.0f
        rm[rmOffset + 14] = 0.0f
        rm[rmOffset + 15] = 1.0f

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ)
    }

    fun quaternionToMatrix(qx: Float, qy: Float, qz: Float, qw: Float, m: FloatArray, offset: Int = 0) {
        val x2 = qx + qx
        val y2 = qy + qy
        val z2 = qz + qz
        val xx = qx * x2
        val xy = qx * y2
        val xz = qx * z2
        val yy = qy * y2
        val yz = qy * z2
        val zz = qz * z2
        val wx = qw * x2
        val wy = qw * y2
        val wz = qw * z2

        m[offset + 0] = 1.0f - (yy + zz)
        m[offset + 1] = xy + wz
        m[offset + 2] = xz - wy
        m[offset + 3] = 0.0f

        m[offset + 4] = xy - wz
        m[offset + 5] = 1.0f - (xx + zz)
        m[offset + 6] = yz + wx
        m[offset + 7] = 0.0f

        m[offset + 8] = xz + wy
        m[offset + 9] = yz - wx
        m[offset + 10] = 1.0f - (xx + yy)
        m[offset + 11] = 0.0f

        m[offset + 12] = 0.0f
        m[offset + 13] = 0.0f
        m[offset + 14] = 0.0f
        m[offset + 15] = 1.0f
    }
}
