package com.jzbrooks.vgo.vd.optimization

import assertk.assertThat
import assertk.assertions.*
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.optimization.ConvertCurvesToArcs
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.vd.VectorDrawableCommandPrinter
import org.junit.jupiter.api.Test

class ConvertCurvesToArcsTest {
    @Test
    fun `Convert curves to arcs`() {
        val path = Path(listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 30f))),
                CubicBezierCurve(CommandVariant.RELATIVE, listOf(
                        CubicBezierCurve.Parameter(
                                Point(0f, -5.302f),
                                Point(2.109f, -10.393f),
                                Point(5.858f, -14.142f)
                        )
                ))
        ))

        ConvertCurvesToArcs(VectorDrawableCommandPrinter(3)).visit(path)

        assertThat(path.commands[1]).isEqualTo(
                EllipticalArcCurve(CommandVariant.RELATIVE, listOf(
                        EllipticalArcCurve.Parameter(
                                20.008137f,
                                20.008137f,
                                0f,
                                EllipticalArcCurve.ArcFlag.SMALL,
                                EllipticalArcCurve.SweepFlag.CLOCKWISE,
                                Point(5.858f, -14.142f)
                        )
                ))
        )
    }

    fun `Sharp corners disqualify curves from arc collapse`() {
        // This data is taken from the first the first commands
        // in visibility_strike.xml. It presented tolerance challenges
        // w.r.t. over-smoothing sharp edges into ellipses.

        val path = Path(listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(12f, 4.5f))),
                CubicBezierCurve(CommandVariant.RELATIVE, listOf(
                        CubicBezierCurve.Parameter(
                                Point(5f, 0f),
                                Point(9.27f, -3.11f),
                                Point(11f, -7.5f) // 1, 12
                        )
                )),
                CubicBezierCurve(CommandVariant.RELATIVE, listOf(
                        CubicBezierCurve.Parameter(
                                Point(1.73f, 4.39f),
                                Point(6f, 7.5f),
                                Point(11f, 7.5f)
                        )
                ))
        ))

        val before = path.copy()

        ConvertCurvesToArcs(VectorDrawableCommandPrinter(3)).visit(path)

        assertThat(path).isEqualTo(before)
    }
}