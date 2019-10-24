package com.jzbrooks.guacamole.core.util.math

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
        val first = Matrix3.from(
                arrayOf(
                        arrayOf(10f, 20f, 10f),
                        arrayOf(4f, 5f, 6f),
                        arrayOf(2f, 3f, 5f)
                )
        )
        val second = Matrix3.from(
                arrayOf(
                        arrayOf(3f, 2f, 4f),
                        arrayOf(3f, 3f, 9f),
                        arrayOf(4f, 4f, 2f)
                )
        )
        val expected = Matrix3.from(
                arrayOf(
                        arrayOf(130f, 120f, 240f),
                        arrayOf(51f, 47f, 73f),
                        arrayOf(35f, 33f, 45f)
                )
        )

        val third = first * second

        assertThat(third).isEqualTo(expected)
    }

    @Test
    fun testMatrixVectorMultiplication() {
        val matrix = Matrix3.from(
                arrayOf(
                        arrayOf(10f, 10f, 4f),
                        arrayOf(0f, 1f, 0f),
                        arrayOf(0f, 0f, 1f)
                )
        )
        val vector = Vector3(10f, 9f, 8f)

        val newVector = matrix * vector

        assertThat(newVector).isEqualTo(Vector3(222f, 9f, 8f))
    }
}