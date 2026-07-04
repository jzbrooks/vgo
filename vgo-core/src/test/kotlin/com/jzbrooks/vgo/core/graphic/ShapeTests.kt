package com.jzbrooks.vgo.core.graphic

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import org.junit.jupiter.api.Test

class ShapeTests {
    private val gradient =
        LinearGradient(
            startX = 0f,
            startY = 0f,
            endX = 10f,
            endY = 0f,
            stops =
                listOf(
                    GradientStop(0f, Color(0xFFB125EAu)),
                    GradientStop(1f, Color(0xFF008AFFu)),
                ),
        )

    @Test
    fun testEqualityIncludesBrushes() {
        val first = createRect().apply { fillBrush = gradient }
        val second = createRect()

        assertThat(first).isNotEqualTo(second)
        assertThat(first.hashCode(), "hash code").isNotEqualTo(second.hashCode())
    }

    @Test
    fun testShapesWithEqualBrushesAreEqual() {
        val first = createRect().apply { fillBrush = gradient }
        val second = createRect().apply { fillBrush = gradient.copy() }

        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode(), "hash code").isEqualTo(second.hashCode())
    }

    private fun createRect() =
        Rect(
            null,
            mutableMapOf(),
            0f,
            0f,
            10f,
            10f,
            0f,
            0f,
            Colors.BLACK,
            Path.FillRule.NON_ZERO,
            Colors.TRANSPARENT,
            1f,
            Path.LineCap.BUTT,
            Path.LineJoin.MITER,
            4f,
        )
}
