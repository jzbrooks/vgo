package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class SimplifyBezierCurveCommandsTests {
    @Test
    fun testCurrentCubicAndPreviousCubicUsesShorthand() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(100f, 100f), Point(450f, 100f), Point(250f, 200f))
                    )
                ),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(-200f, 100f), Point(400f, 300f), Point(250f, 200f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentCubicAndPreviousShortcutUsesShorthand() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(450f, 100f), Point(250f, 200f))
                    )
                ),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(-200f, 100f), Point(400f, 300f), Point(250f, 200f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentNonCubicNonShorthandUsesShorthand() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(0f, 0f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(0f, 0f), Point(400f, 300f), Point(250f, 200f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentQuadraticAndPreviousQuadraticUsesShorthand() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                QuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        QuadraticBezierCurve.Parameter(Point(450f, 100f), Point(250f, 200f))
                    )
                ),
                QuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        QuadraticBezierCurve.Parameter(Point(-200f, 100f), Point(400f, 300f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(SmoothQuadraticBezierCurve::class)
    }

    @Test
    fun testCurrentQuadraticAndPreviousShorthandUsesShorthand() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                QuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        QuadraticBezierCurve.Parameter(Point(-300f, 100f), Point(150f, 50f))
                    )
                ),
                SmoothQuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        Point(250f, 200f)
                    )
                ),
                QuadraticBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        QuadraticBezierCurve.Parameter(Point(-200f, 100f), Point(250f, 200f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(SmoothQuadraticBezierCurve::class)
    }

    @Test
    fun testStraightCubicBezierCurveIsConvertedToLineTo() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(20f, 20f), Point(20f, 20f), Point(20f, 20f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(LineTo::class)
    }

    @Test
    fun testShortcutCubicIsExpandedIfPreviousIsStraightened() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(20f, 20f), Point(20f, 20f), Point(20f, 20f))
                    )
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(450f, 100f), Point(250f, 200f))
                    )
                )
            ),
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path::commands)
            .index(path.commands.size - 1)
            .isInstanceOf(CubicBezierCurve::class)
            .prop(CubicBezierCurve::parameters)
            .transform("last element") { it.last() }
            .prop(CubicBezierCurve.Parameter::startControl)
            .isEqualTo(Point.ZERO)
    }
}
