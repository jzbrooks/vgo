package com.jzbrooks.vgo.core.transformation

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Path
import org.junit.jupiter.api.Test

class ConvertShapesToPathsTests {
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
    fun testGradientBrushIsCarriedToPath() {
        val circle =
            createCircle().apply {
                fillBrush = gradient
            }

        val path = ConvertShapesToPaths.convertToPath(circle)

        assertThat(path::fill).isEqualTo(gradient)
        assertThat(path::stroke).isEqualTo(Colors.TRANSPARENT)
    }

    @Test
    fun testSolidColorBrushIsCarriedToPath() {
        val circle = createCircle(fill = Color(0xFFFF0000u))

        val path = ConvertShapesToPaths.convertToPath(circle)

        assertThat(path::fill).isEqualTo(Color(0xFFFF0000u))
    }

    private fun createCircle(
        fill: Color = Colors.BLACK,
        stroke: Color = Colors.TRANSPARENT,
    ) = Circle(
        null,
        mutableMapOf(),
        5f,
        5f,
        2f,
        fill,
        Path.FillRule.NON_ZERO,
        stroke,
        1f,
        Path.LineCap.BUTT,
        Path.LineJoin.MITER,
        4f,
    )
}
