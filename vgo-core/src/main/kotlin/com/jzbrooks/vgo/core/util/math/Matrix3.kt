package com.jzbrooks.vgo.core.util.math

import kotlin.math.absoluteValue

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
private value class ArrayMatrix3(private val data: FloatArray) : MutableMatrix3 {
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

    override operator fun times(other: Vector3): Vector3 {
        return Vector3(
            this[0, 0] * other.i + this[0, 1] * other.j + this[0, 2] * other.k,
            this[1, 0] * other.i + this[1, 1] * other.j + this[1, 2] * other.k,
            this[2, 0] * other.i + this[2, 1] * other.j + this[2, 2] * other.k,
        )
    }

    override fun clone(): Matrix3 = ArrayMatrix3(data.clone())

    // @Cleanup: There's some room for improvement here,
    // it would be nice to override equals, but that isn't possible yet
    override fun contentsEqual(
        other: Matrix3,
        delta: Float,
    ) = Matrix3.contentsEqual(this, other, delta)
}
