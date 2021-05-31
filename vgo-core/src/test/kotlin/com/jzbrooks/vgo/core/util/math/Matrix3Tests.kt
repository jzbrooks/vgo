package com.jzbrooks.vgo.core.util.math

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.util.assertk.isEqualTo
import org.junit.jupiter.api.Test

class Matrix3Tests {
    private val delta = 0.001f

    @Test
    fun testMultiplyIdentitiesProducesIdentity() {
        val first = Matrix3.IDENTITY
        val second = Matrix3.IDENTITY

        assertThat(first * second).isEqualTo(Matrix3.IDENTITY)
    }

    @Test
    fun testMultiply() {
        val first = Matrix3.from(floatArrayOf(10f, 20f, 10f, 4f, 5f, 6f, 2f, 3f, 5f))
        val second = Matrix3.from(floatArrayOf(3f, 2f, 4f, 3f, 3f, 9f, 4f, 4f, 2f))
        val product = first * second

        val expected = Matrix3.from(floatArrayOf(130f, 120f, 240f, 51f, 47f, 73f, 35f, 33f, 45f))

        assertThat(product).isEqualTo(expected)
    }

    @Test
    fun testMatrixVectorMultiplication() {
        val matrix = Matrix3.from(floatArrayOf(10f, 10f, 4f, 0f, 1f, 0f, 0f, 0f, 1f))
        val vector = Vector3(10f, 9f, 8f)

        val product = matrix * vector

        assertThat(product).isEqualTo(Vector3(222f, 9f, 8f))
    }
}
