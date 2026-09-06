package com.jzbrooks.vgo.svg

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.element.createPath
import org.junit.jupiter.api.Test

class ScalableVectorGraphicShapePrinterTests {
    private val printer = ScalableVectorGraphicShapePrinter(ScalableVectorGraphicCommandPrinter(3))

    @Test
    fun `prints path command data within a path element`() {
        val path = createPath(CommandString("M1,2h24v24h-24Z").toCommandList())

        assertThat(printer.print(path)).isEqualTo("<path d=\"M1,2h24v24h-24Z\"/>")
    }

    @Test
    fun `prints circle`() {
        val circle =
            Circle(
                null,
                mutableMapOf(),
                10f,
                12.5f,
                4.25f,
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            )

        assertThat(printer.print(circle)).isEqualTo("<circle cx=\"10\" cy=\"12.5\" r=\"4.25\"/>")
    }

    @Test
    fun `prints ellipse`() {
        val ellipse =
            Ellipse(
                null,
                mutableMapOf(),
                10f,
                12.5f,
                4.25f,
                2f,
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            )

        assertThat(printer.print(ellipse)).isEqualTo("<ellipse cx=\"10\" cy=\"12.5\" rx=\"4.25\" ry=\"2\"/>")
    }

    @Test
    fun `prints rect`() {
        assertThat(printer.print(rect(rx = 0f, ry = 0f)))
            .isEqualTo("<rect x=\"1\" y=\"2\" width=\"30\" height=\"40\"/>")
    }

    @Test
    fun `prints rounded rect corner radii`() {
        assertThat(printer.print(rect(rx = 3f, ry = 4f)))
            .isEqualTo("<rect x=\"1\" y=\"2\" width=\"30\" height=\"40\" rx=\"3\" ry=\"4\"/>")
    }

    @Test
    fun `prints line`() {
        val line =
            Line(
                null,
                mutableMapOf(),
                1f,
                2f,
                8f,
                12f,
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            )

        assertThat(printer.print(line)).isEqualTo("<line x1=\"1\" y1=\"2\" x2=\"8\" y2=\"12\"/>")
    }

    @Test
    fun `prints polyline`() {
        val polyline =
            Polyline(
                null,
                mutableMapOf(),
                listOf(Point(1f, 2f), Point(3f, 4f)),
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            )

        assertThat(printer.print(polyline)).isEqualTo("<polyline points=\"1,2 3,4\"/>")
    }

    @Test
    fun `prints polygon`() {
        val polygon =
            Polygon(
                null,
                mutableMapOf(),
                listOf(Point(1f, 2f), Point(3f, 4f)),
                Colors.BLACK,
                Path.FillRule.NON_ZERO,
                Colors.TRANSPARENT,
                1f,
                Path.LineCap.BUTT,
                Path.LineJoin.MITER,
                4f,
            )

        assertThat(printer.print(polygon)).isEqualTo("<polygon points=\"1,2 3,4\"/>")
    }

    private fun rect(
        rx: Float,
        ry: Float,
    ) = Rect(
        null,
        mutableMapOf(),
        1f,
        2f,
        30f,
        40f,
        rx,
        ry,
        Colors.BLACK,
        Path.FillRule.NON_ZERO,
        Colors.TRANSPARENT,
        1f,
        Path.LineCap.BUTT,
        Path.LineJoin.MITER,
        4f,
    )
}
