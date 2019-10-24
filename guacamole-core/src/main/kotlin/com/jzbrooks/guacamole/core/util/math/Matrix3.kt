package com.jzbrooks.guacamole.core.util.math

interface Matrix3 {
    operator fun get(row: Int, column: Int): Float
    operator fun times(other: Matrix3): Matrix3
    operator fun times(other: Vector3): Vector3

    companion object {
        val IDENTITY = from()
        fun from(
                data: Array<Array<Float>> = Array(3) { j -> Array(3) { i -> if (i == j) 1f else 0f } }
        ): Matrix3 = ArrayMatrix3(data)
    }
}

interface MutableMatrix3 : Matrix3 {
    operator fun set(row: Int, column: Int, value: Float)
    companion object {
        fun from(
                data: Array<Array<Float>> = Array(3) { j -> Array(3) { i -> if (i == j) 1f else 0f } }
        ): MutableMatrix3 = ArrayMatrix3(data)
    }
}

private class ArrayMatrix3(
    internal val data: Array<Array<Float>> = Array(3) { Array(3) { 0f } }
) : MutableMatrix3 {

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix3) return false

        for (i in data.indices) {
            for (j in data[i].indices) {
                if (data[i][j] != other[i, j]) return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var hash = 11

        for (row in data) {
            for (column in row) {
                hash += column.hashCode()
            }
        }

        return hash
    }

    override operator fun get(row: Int, column: Int): Float {
        return data[row][column]
    }

    override operator fun set(row: Int, column: Int, value: Float) {
        data[row][column] = value
    }

    override operator fun times(other: Matrix3): Matrix3 {
        return ArrayMatrix3().apply {
            this[0, 0] = this@ArrayMatrix3[0, 0] * other[0, 0] + this@ArrayMatrix3[0, 1] * other[1, 0] + this@ArrayMatrix3[0, 2] * other[2, 0]
            this[0, 1] = this@ArrayMatrix3[0, 0] * other[0, 1] + this@ArrayMatrix3[0, 1] * other[1, 1] + this@ArrayMatrix3[0, 2] * other[2, 1]
            this[0, 2] = this@ArrayMatrix3[0, 0] * other[0, 2] + this@ArrayMatrix3[0, 1] * other[1, 2] + this@ArrayMatrix3[0, 2] * other[2, 2]
            this[1, 0] = this@ArrayMatrix3[1, 0] * other[0, 0] + this@ArrayMatrix3[1, 1] * other[1, 0] + this@ArrayMatrix3[1, 2] * other[2, 0]
            this[1, 1] = this@ArrayMatrix3[1, 0] * other[0, 1] + this@ArrayMatrix3[1, 1] * other[1, 1] + this@ArrayMatrix3[1, 2] * other[2, 1]
            this[1, 2] = this@ArrayMatrix3[1, 0] * other[0, 2] + this@ArrayMatrix3[1, 1] * other[1, 2] + this@ArrayMatrix3[1, 2] * other[2, 2]
            this[2, 0] = this@ArrayMatrix3[2, 0] * other[0, 0] + this@ArrayMatrix3[2, 1] * other[1, 0] + this@ArrayMatrix3[2, 2] * other[2, 0]
            this[2, 1] = this@ArrayMatrix3[2, 0] * other[0, 1] + this@ArrayMatrix3[2, 1] * other[1, 1] + this@ArrayMatrix3[2, 2] * other[2, 1]
            this[2, 2] = this@ArrayMatrix3[2, 0] * other[0, 2] + this@ArrayMatrix3[2, 1] * other[1, 2] + this@ArrayMatrix3[2, 2] * other[2, 2]
        }
    }

    override operator fun times(other: Vector3): Vector3 {
        return Vector3(
                this[0, 0] * other.i + this[0, 1] * other.j + this[0, 2] * other.k,
                this[1, 0] * other.i + this[1, 1] * other.j + this[1, 2] * other.k,
                this[2, 0] * other.i + this[2, 1] * other.j + this[2, 2] * other.k
        )
    }
}