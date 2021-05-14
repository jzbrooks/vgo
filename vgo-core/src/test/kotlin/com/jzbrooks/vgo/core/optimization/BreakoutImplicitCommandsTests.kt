package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isTrue
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class BreakoutImplicitCommandsTests {
    @Test
    fun testImplicitCommandsBrokenOut() {
        val path = Path(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(103f, 6f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f), Point(93f, 10f))),
                ClosePath,
            ),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        BreakoutImplicitCommands().visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().none { it.parameters.size > 1 }).isTrue()
        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(3)
    }

    @Test
    fun testImplicitLineToCommandsInMoveToAreBrokenOut() {
        val path = Path(
            listOf(
                MoveTo(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        Point(100f, 1f),
                        Point(103f, 6f),
                        Point(106f, 7f),
                        Point(93f, 10f)
                    )
                ),
                ClosePath,
            ),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        BreakoutImplicitCommands().visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().none { it.parameters.size > 1 }).isTrue()
        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(3)
    }

    @Test
    fun testSingleParameterCommandsAreNotModified() {
        val path = Path(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(Point(109f, 8f), Point(113f, 12f), Point(120f, 10f)))),
                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(101f)),
                VerticalLineTo(CommandVariant.ABSOLUTE, listOf(-8f)),
                HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(103f)),
                SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(SmoothCubicBezierCurve.Parameter(Point(113f, 39f), Point(105f, -6f)))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(112f, -10f), Point(109f, -3f)))),
                SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(Point(100f, 0f))),
                EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(4f, 3f, 93f, EllipticalArcCurve.ArcFlag.LARGE, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(109f, 15f)))),
                ClosePath,
            ),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )
        val sizeBefore = path.commands.size

        BreakoutImplicitCommands().visit(path)

        assertThat(path.commands).hasSize(sizeBefore)
    }
}
