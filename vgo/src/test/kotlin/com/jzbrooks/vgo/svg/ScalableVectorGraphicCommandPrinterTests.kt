package com.jzbrooks.vgo.svg

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class ScalableVectorGraphicCommandPrinterTests {
    private val printer = ScalableVectorGraphicCommandPrinter(3)

    @Test
    fun `omits separators before negative polycommand parameters`() {
        val command =
            LineTo(
                CommandVariant.RELATIVE,
                listOf(Point(1f, 2f), Point(-3f, 4f), Point(5f, 6f)),
            )

        assertThat(printer.print(command)).isEqualTo("l1,2-3,4 5,6")
    }

    @Test
    fun `omits separators before negative curve parameter sets`() {
        val command =
            CubicBezierCurve(
                CommandVariant.RELATIVE,
                listOf(
                    CubicBezierCurve.Parameter(Point(1f, 2f), Point(3f, 4f), Point(5f, 6f)),
                    CubicBezierCurve.Parameter(Point(-1f, 2f), Point(3f, 4f), Point(5f, 6f)),
                ),
            )

        assertThat(printer.print(command)).isEqualTo("c1,2 3,4 5,6-1,2 3,4 5,6")
    }
}
