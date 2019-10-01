package com.jzbrooks.guacamole.util.math

open class Matrix3 {
    protected val data = Array(3) { Array(3) { 0f } }

    init {
        data[0][0] = 1f
        data[1][1] = 1f
        data[2][2] = 1f
    }

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

    operator fun get(row: Int, column: Int): Float {
        return data[row][column]
    }

    operator fun times(other: Matrix3): Matrix3 {
        return MutableMatrix3().apply {
            this[0, 0] = this@Matrix3[0, 0] * other[0, 0] + this@Matrix3[0, 1] * other[1, 0] + this@Matrix3[0, 2] * other[2, 0]
            this[0, 1] = this@Matrix3[0, 0] * other[0, 1] + this@Matrix3[0, 1] * other[1, 1] + this@Matrix3[0, 2] * other[2, 1]
            this[0, 2] = this@Matrix3[0, 0] * other[0, 2] + this@Matrix3[0, 1] * other[1, 2] + this@Matrix3[0, 2] * other[2, 2]
            this[1, 0] = this@Matrix3[1, 0] * other[0, 0] + this@Matrix3[1, 1] * other[1, 0] + this@Matrix3[1, 2] * other[2, 0]
            this[1, 1] = this@Matrix3[1, 0] * other[0, 1] + this@Matrix3[1, 1] * other[1, 1] + this@Matrix3[1, 2] * other[2, 1]
            this[1, 2] = this@Matrix3[1, 0] * other[0, 2] + this@Matrix3[1, 1] * other[1, 2] + this@Matrix3[1, 2] * other[2, 2]
            this[2, 0] = this@Matrix3[2, 0] * other[0, 0] + this@Matrix3[2, 1] * other[1, 0] + this@Matrix3[2, 2] * other[2, 0]
            this[2, 1] = this@Matrix3[2, 0] * other[0, 1] + this@Matrix3[2, 1] * other[1, 1] + this@Matrix3[2, 2] * other[2, 1]
            this[2, 2] = this@Matrix3[2, 0] * other[0, 2] + this@Matrix3[2, 1] * other[1, 2] + this@Matrix3[2, 2] * other[2, 2]
        }
    }

    operator fun times(other: Vector3): Vector3 {
        return Vector3(
                this[0, 0] * other.i + this[0, 1] * other.j + this[0, 2] * other.k,
                this[1, 0] * other.i + this[1, 1] * other.j + this[1, 2] * other.k,
                this[2, 0] * other.i + this[2, 1] * other.j + this[2, 2] * other.k
        )
    }

    companion object {
        val IDENTITY: Matrix3 = MutableMatrix3().apply {
            this[0, 0] = 1f
            this[1, 1] = 1f
            this[2, 2] = 1f
        }
    }
}

class MutableMatrix3 : Matrix3() {
    operator fun set(row: Int, column: Int, value: Float) {
        data[row][column] = value
    }
}