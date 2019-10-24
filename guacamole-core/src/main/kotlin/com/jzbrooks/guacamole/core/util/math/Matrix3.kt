package com.jzbrooks.guacamole.core.util.math

interface Matrix3 {
    operator fun get(row: Int, column: Int): Float
    operator fun times(other: Matrix3): Matrix3
    operator fun times(other: Vector3): Vector3

    companion object {
        val IDENTITY = object : Matrix3 {
            override fun get(row: Int, column: Int) = if (row == column) 1f else 0f

            override fun times(other: Matrix3): Matrix3 {
                val data = Array(3) { Array(3) { 0f } }

                for (i in 0..2) {
                    for (j in 0..2) {
                        data[i][j] = other[i, j]
                    }
                }

                return ArrayMatrix3(data)
            }

            override fun times(other: Vector3): Vector3 = other.copy()
        }
        fun from(data: Array<Array<Float>>): Matrix3 = ArrayMatrix3(data)
    }
}

interface MutableMatrix3 : Matrix3 {
    operator fun set(row: Int, column: Int, value: Float)
    companion object {
        fun identity(): MutableMatrix3 = ArrayMatrix3(arrayOf(
                arrayOf(1f, 0f, 0f),
                arrayOf(1f, 1f, 0f),
                arrayOf(1f, 0f, 1f)
        ))
        fun from(data: Array<Array<Float>>): MutableMatrix3 = ArrayMatrix3(data)
    }
}

private class ArrayMatrix3(private val data: Array<Array<Float>>) : MutableMatrix3 {

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
        val data = Array(3) { Array(3) { 0f }}

        data[0][0] = this[0, 0] * other[0, 0] + this[0, 1] * other[1, 0] + this[0, 2] * other[2, 0]
        data[0][1] = this[0, 0] * other[0, 1] + this[0, 1] * other[1, 1] + this[0, 2] * other[2, 1]
        data[0][2] = this[0, 0] * other[0, 2] + this[0, 1] * other[1, 2] + this[0, 2] * other[2, 2]
        data[1][0] = this[1, 0] * other[0, 0] + this[1, 1] * other[1, 0] + this[1, 2] * other[2, 0]
        data[1][1] = this[1, 0] * other[0, 1] + this[1, 1] * other[1, 1] + this[1, 2] * other[2, 1]
        data[1][2] = this[1, 0] * other[0, 2] + this[1, 1] * other[1, 2] + this[1, 2] * other[2, 2]
        data[2][0] = this[2, 0] * other[0, 0] + this[2, 1] * other[1, 0] + this[2, 2] * other[2, 0]
        data[2][1] = this[2, 0] * other[0, 1] + this[2, 1] * other[1, 1] + this[2, 2] * other[2, 1]
        data[2][2] = this[2, 0] * other[0, 2] + this[2, 1] * other[1, 2] + this[2, 2] * other[2, 2]

        return ArrayMatrix3(data)
    }

    override operator fun times(other: Vector3): Vector3 {
        return Vector3(
                this[0, 0] * other.i + this[0, 1] * other.j + this[0, 2] * other.k,
                this[1, 0] * other.i + this[1, 1] * other.j + this[1, 2] * other.k,
                this[2, 0] * other.i + this[2, 1] * other.j + this[2, 2] * other.k
        )
    }
}