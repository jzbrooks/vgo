package com.jzbrooks.guacamole.util.math

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class Matrix3Tests {
    @Test
    fun testMultiplyIdentitiesProducesIdentity() {
        val first = Matrix3.IDENTITY
        val second = Matrix3.IDENTITY

        val third = first * second

        assertThat(third).isEqualTo(Matrix3.IDENTITY)
    }

    @Test
    fun testMultiply() {
        val first = MutableMatrix3().apply {
            this[0, 0] = 10f
            this[0, 1] = 20f
            this[0, 2] = 10f
            this[1, 0] = 4f
            this[1, 1] = 5f
            this[1, 2] = 6f
            this[2, 0] = 2f
            this[2, 1] = 3f
            this[2, 2] = 5f
        }
        val second = MutableMatrix3().apply {
            this[0, 0] = 3f
            this[0, 1] = 2f
            this[0, 2] = 4f
            this[1, 0] = 3f
            this[1, 1] = 3f
            this[1, 2] = 9f
            this[2, 0] = 4f
            this[2, 1] = 4f
            this[2, 2] = 2f
        }
        val expected = MutableMatrix3().apply {
            this[0, 0] = 130f
            this[0, 1] = 120f
            this[0, 2] = 240f
            this[1, 0] = 51f
            this[1, 1] = 47f
            this[1, 2] = 73f
            this[2, 0] = 35f
            this[2, 1] = 33f
            this[2, 2] = 45f
        }

        val third = first * second

        assertThat(third).isEqualTo(expected)
    }

    @Test
    fun testMatrixVectorMultiplication() {
        val matrix = MutableMatrix3().apply {
            this[0, 0] = 10f
            this[0, 1] = 10f
            this[0, 2] = 4f
        }
        val vector = Vector3(10f, 9f, 8f)

        val newVector = matrix * vector

        assertThat(newVector).isEqualTo(Vector3(222f, 9f, 8f))
    }
}