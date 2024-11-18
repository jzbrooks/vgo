package com.jzbrooks.vgo.core.util.math

import assertk.all
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import org.junit.jupiter.api.Test

class CollisionDetectionTest {
    @Test
    fun `Sample points`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 5f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(10f, 5f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(10f, 5f))),
                HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
                VerticalLineTo(CommandVariant.RELATIVE, listOf(5f)),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(20f)
            prop(Rectangle::right).isEqualTo(40f)
            prop(Rectangle::bottom).isEqualTo(5f)
        }
    }

    @Test
    fun `Sample curves`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(
                    Point(0f, 50f),
                    Point(10f, 50f),
                    Point(20f, 100f),
                )))
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(6f, 0.1f)
            prop(Rectangle::top).isEqualTo(100f)
            prop(Rectangle::right).isEqualTo(20f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }

    @Test
    fun `Sample smooth curves`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(
                    Point(0f, 50f),
                    Point(10f, 50f),
                    Point(20f, 100f),
                ))),
                SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(SmoothCubicBezierCurve.Parameter(Point(0f, 25f), Point(10f, 75f))))
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(6f, 0.1f)
            prop(Rectangle::top).isCloseTo(112f, 0.1f)
            prop(Rectangle::right).isCloseTo(22f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `Sample quadratic curves`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(40f, 10f), Point(10f, 40f))))
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(40f)
            prop(Rectangle::right).isEqualTo(25f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }
}
