package com.jzbrooks.vgo.core.util.math

import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

interface Matrix3 {
    operator fun get(
        row: Int,
        column: Int,
    ): Float

    operator fun times(other: Matrix3): Matrix3

    operator fun times(other: Vector3): Vector3

    fun clone(): Matrix3

    fun contentsEqual(
        other: Matrix3,
        delta: Float = 0.001f,
    ): Boolean

    companion object {
        val IDENTITY =
            object : Matrix3 {
                override fun get(
                    row: Int,
                    column: Int,
                ) = if (row == column) 1f else 0f

                override fun times(other: Matrix3) = other.clone()

                override fun times(other: Vector3): Vector3 = other.copy()

                override fun clone(): Matrix3 = this

                override fun contentsEqual(
                    other: Matrix3,
                    delta: Float,
                ) = contentsEqual(this, other, delta)
            }

        @JvmStatic
        fun from(data: FloatArray): Matrix3 = ArrayMatrix3(data)

        @JvmStatic
        fun contentsEqual(
            first: Matrix3,
            second: Matrix3,
            delta: Float,
        ): Boolean {
            for (i in 0..2) {
                for (j in 0..2) {
                    val difference = first[i, j] - second[i, j]
                    if (difference.absoluteValue > delta) return false
                }
            }
            return true
        }
    }
}

interface MutableMatrix3 : Matrix3 {
    operator fun set(
        row: Int,
        column: Int,
        value: Float,
    )

    companion object {
        @JvmStatic
        fun identity(): MutableMatrix3 = ArrayMatrix3(floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 1f))

        @JvmStatic
        fun from(data: FloatArray): MutableMatrix3 = ArrayMatrix3(data)
    }
}

@JvmInline
private value class ArrayMatrix3(
    private val data: FloatArray,
) : MutableMatrix3 {
    override operator fun get(
        row: Int,
        column: Int,
    ) = data[row * 3 + column]

    override operator fun set(
        row: Int,
        column: Int,
        value: Float,
    ) {
        data[row * 3 + column] = value
    }

    override operator fun times(other: Matrix3): Matrix3 {
        val data = FloatArray(9)

        data[0] = this[0, 0] * other[0, 0] + this[0, 1] * other[1, 0] + this[0, 2] * other[2, 0]
        data[1] = this[0, 0] * other[0, 1] + this[0, 1] * other[1, 1] + this[0, 2] * other[2, 1]
        data[2] = this[0, 0] * other[0, 2] + this[0, 1] * other[1, 2] + this[0, 2] * other[2, 2]
        data[3] = this[1, 0] * other[0, 0] + this[1, 1] * other[1, 0] + this[1, 2] * other[2, 0]
        data[4] = this[1, 0] * other[0, 1] + this[1, 1] * other[1, 1] + this[1, 2] * other[2, 1]
        data[5] = this[1, 0] * other[0, 2] + this[1, 1] * other[1, 2] + this[1, 2] * other[2, 2]
        data[6] = this[2, 0] * other[0, 0] + this[2, 1] * other[1, 0] + this[2, 2] * other[2, 0]
        data[7] = this[2, 0] * other[0, 1] + this[2, 1] * other[1, 1] + this[2, 2] * other[2, 1]
        data[8] = this[2, 0] * other[0, 2] + this[2, 1] * other[1, 2] + this[2, 2] * other[2, 2]

        return ArrayMatrix3(data)
    }

    override operator fun times(other: Vector3): Vector3 =
        Vector3(
            this[0, 0] * other.i + this[0, 1] * other.j + this[0, 2] * other.k,
            this[1, 0] * other.i + this[1, 1] * other.j + this[1, 2] * other.k,
            this[2, 0] * other.i + this[2, 1] * other.j + this[2, 2] * other.k,
        )

    override fun clone(): Matrix3 = ArrayMatrix3(data.clone())

    // @Cleanup: There's some room for improvement here,
    // it would be nice to override equals, but that isn't possible yet
    override fun contentsEqual(
        other: Matrix3,
        delta: Float,
    ) = Matrix3.contentsEqual(this, other, delta)
}

/**
 * Computes a transformation matrix based on the provided parameters.
 *
 * @param scaleX The scale factor in the X direction. Default is 1.
 * @param scaleY The scale factor in the Y direction. Default is 1.
 * @param translationX The translation in the X direction. Default is 0.
 * @param translationY The translation in the Y direction. Default is 0.
 * @param rotation The rotation angle in degrees. Default is 0.
 * @param pivotX The pivot point in the X direction. Default is 0.
 * @param pivotY The pivot point in the Y direction. Default is 0.
 */
fun computeTransformation(
    scaleX: Float? = null,
    scaleY: Float? = null,
    translationX: Float? = null,
    translationY: Float? = null,
    rotation: Float? = null,
    pivotX: Float? = null,
    pivotY: Float? = null,
): Matrix3 {
    val scale =
        Matrix3.from(
            floatArrayOf(scaleX ?: 1f, 0f, 0f, 0f, scaleY ?: 1f, 0f, 0f, 0f, 1f),
        )

    val translation =
        Matrix3.from(
            floatArrayOf(1f, 0f, translationX ?: 0f, 0f, 1f, translationY ?: 0f, 0f, 0f, 1f),
        )

    val pivot =
        Matrix3.from(
            floatArrayOf(1f, 0f, pivotX ?: 0f, 0f, 1f, pivotY ?: 0f, 0f, 0f, 1f),
        )

    val pivotInverse =
        Matrix3.from(
            floatArrayOf(1f, 0f, (pivotX ?: 0f) * -1, 0f, 1f, (pivotY ?: 0f) * -1, 0f, 0f, 1f),
        )

    val rotate =
        if (rotation != null) {
            val radians = rotation * PI.toFloat() / 180f
            Matrix3.from(floatArrayOf(cos(radians), -sin(radians), 0f, sin(radians), cos(radians), 0f, 0f, 0f, 1f))
        } else {
            Matrix3.IDENTITY
        }

    return pivotInverse * translation * rotate * scale * pivot
}
