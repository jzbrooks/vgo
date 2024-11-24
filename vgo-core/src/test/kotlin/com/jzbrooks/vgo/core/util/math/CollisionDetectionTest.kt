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
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
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
    fun `relative polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(310f)
            prop(Rectangle::right).isEqualTo(70f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }

    @Test
    fun `absolute polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(40f, 90f),
                            Point(70f, 120f),
                            Point(150f, 200f),
                        ),
                        CubicBezierCurve.Parameter(
                            Point(150f, 150f),
                            Point(175f, 175f),
                            Point(200f, 250f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(6f, 0.1f)
            prop(Rectangle::top).isEqualTo(250f)
            prop(Rectangle::right).isEqualTo(200f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }

    @Test
    fun `relative smooth polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                        SmoothCubicBezierCurve.Parameter(
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(310f)
            prop(Rectangle::right).isEqualTo(70f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }

    @Test
    fun `absolute smooth polycubic bounding box`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(
                            Point(70f, 120f),
                            Point(150f, 200f),
                        ),
                        SmoothCubicBezierCurve.Parameter(
                            Point(175f, 175f),
                            Point(200f, 250f),
                        ),
                    ),
                ),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(6f, 0.1f)
            prop(Rectangle::top).isEqualTo(250f)
            prop(Rectangle::right).isEqualTo(200f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }

    @Test
    fun `Sample smooth curves`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                CubicBezierCurve(
                    CommandVariant.ABSOLUTE,
                    listOf(
                        CubicBezierCurve.Parameter(
                            Point(0f, 50f),
                            Point(10f, 50f),
                            Point(20f, 100f),
                        ),
                    ),
                ),
                SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(SmoothCubicBezierCurve.Parameter(Point(0f, 25f), Point(10f, 75f)))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isCloseTo(6f, 0.1f)
            prop(Rectangle::top).isCloseTo(110.16f, 0.1f)
            prop(Rectangle::right).isCloseTo(21.25f, 0.1f)
            prop(Rectangle::bottom).isCloseTo(10f, 0.1f)
        }
    }

    @Test
    fun `Sample quadratic curves`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(40f, 10f), Point(10f, 40f)))),
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

    @Test
    fun `correct bounding box for polycommands`() {
        val commands =
            listOf<Command>(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 10f), Point(10f, 10f), Point(10f, 10f))),
            )

        val surveyor = Surveyor()
        val box = surveyor.findBoundingBox(commands)

        assertThat(box).all {
            prop(Rectangle::left).isEqualTo(10f)
            prop(Rectangle::top).isEqualTo(30f)
            prop(Rectangle::right).isEqualTo(30f)
            prop(Rectangle::bottom).isEqualTo(10f)
        }
    }
}
