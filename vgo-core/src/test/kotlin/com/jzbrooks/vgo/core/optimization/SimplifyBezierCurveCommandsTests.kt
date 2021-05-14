package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class SimplifyBezierCurveCommandsTests {
    @Test
    fun testCurrentCubicAndPreviousCubicUsesShorthand() {
        val path = Path(
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
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentCubicAndPreviousShortcutUsesShorthand() {
        val path = Path(
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
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentNonCubicNonShorthandUsesShorthand() {
        val path = Path(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(0f, 0f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(0f, 0f), Point(400f, 300f), Point(250f, 200f))
                    )
                )
            ),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(SmoothCubicBezierCurve::class)
    }

    @Test
    fun testCurrentQuadraticAndPreviousQuadraticUsesShorthand() {
        val path = Path(
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
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(SmoothQuadraticBezierCurve::class)
    }

    @Test
    fun testCurrentQuadraticAndPreviousShorthandUsesShorthand() {
        val path = Path(
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
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(SmoothQuadraticBezierCurve::class)
    }

    @Test
    fun testStraightCubicBezierCurveIsConvertedToLineTo() {
        val path = Path(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(20f, 20f), Point(20f, 20f), Point(20f, 20f))
                    )
                )
            ),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(LineTo::class)
    }

    @Test
    fun testShortcutCubicIsExpandedIfPreviousIsStraightened() {
        val path = Path(
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
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        SimplifyBezierCurveCommands(0.00001f).visit(path)

        assertThat(path.commands.last()::class).isEqualTo(CubicBezierCurve::class)
        assertThat((path.commands.last() as CubicBezierCurve).parameters.last().startControl).isEqualTo(Point.ZERO)
    }
}
