package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class UseShorthandTests {
    @Test
    fun testRewriteCubicBezierCurveWithShorthand() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 200f))),
                        CubicBezierCurve(CommandVariant.RELATIVE, listOf(
                                CubicBezierCurve.Parameter(Point(100f, 100f), Point(450f, 100f), Point(250f, 200f)))
                        ),
                        CubicBezierCurve(CommandVariant.RELATIVE, listOf(
                                CubicBezierCurve.Parameter(Point(-200f, 100f), Point(400f, 300f), Point(250f, 200f)))
                        )
                )
        )

        UseShorthand().visit(path)

        assertThat(path.commands.last()::class).isEqualTo(ShortcutCubicBezierCurve::class)
    }
}