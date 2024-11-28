package com.jzbrooks.vgo.core.util.math

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class RectangleTests {
    @Test
    fun `test non-intersection`() {
        val first = Rectangle(10f, 90f, 110f, 10f)
        val second = Rectangle(130f, 90f, 110f, 10f)

        val intersects = first intersects second

        assertThat(intersects).isFalse()
    }

    @Test
    fun `test intersection`() {
        val first = Rectangle(10f, 90f, 110f, 10f)
        val second = Rectangle(90f, 90f, 110f, 10f)

        val intersects = first intersects second

        assertThat(intersects).isTrue()
    }

    @Test
    fun `test interior rectangle`() {
        val first = Rectangle(10f, 90f, 110f, 10f)
        val second = Rectangle(20f, 40f, 40f, 20f)

        val intersects = first intersects second

        assertThat(intersects).isTrue()
    }
}
