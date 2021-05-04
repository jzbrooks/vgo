package com.jzbrooks.vgo.core.util.math

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CurvesTests {
    private val nonRelativeCurve = CubicBezierCurve(
        CommandVariant.ABSOLUTE,
        listOf(
            CubicBezierCurve.Parameter(Point(-2.5f, 0f), Point(-5f, 2.25f), Point(-5f, 5f))
        )
    )

    private val multiParameterCurve = CubicBezierCurve(
        CommandVariant.RELATIVE,
        listOf(
            CubicBezierCurve.Parameter(Point(-2.5f, 0f), Point(-5f, 2.25f), Point(-5f, 5f)),
            CubicBezierCurve.Parameter(Point(-2.5f, 0f), Point(-5f, 2.25f), Point(-5f, 5f))
        )
    )

    @Test
    fun `Ensure non-relative interpolation throws`() {
        assertThrows<AssertionError> {
            nonRelativeCurve.interpolate(0.2f)
        }
    }

    @Test
    fun `Ensure multiple curve parameter interpolation throws`() {
        assertThrows<AssertionError> {
            multiParameterCurve.interpolate(0.2f)
        }
    }

    @Test
    fun `Ensure non-relative throws`() {
        assertThrows<AssertionError> {
            nonRelativeCurve.isConvex()
        }
    }

    @Test
    fun `Ensure multiple curve parameters throws`() {
        assertThrows<AssertionError> {
            multiParameterCurve.isConvex()
        }
    }

    @Test
    fun `Ensure non-relative shortcut throws`() {
        val nonRelativeCurve = SmoothCubicBezierCurve(
            CommandVariant.ABSOLUTE,
            listOf(
                SmoothCubicBezierCurve.Parameter(Point(-5f, 2.25f), Point(-5f, 5f))
            )
        )

        assertThrows<AssertionError> {
            nonRelativeCurve.isConvex()
        }
    }

    @Test
    fun `Ensure multiple shortcut curve parameters throws`() {
        val mutliParameterCurve = SmoothCubicBezierCurve(
            CommandVariant.RELATIVE,
            listOf(
                SmoothCubicBezierCurve.Parameter(Point(-5f, 2.25f), Point(-5f, 5f)),
                SmoothCubicBezierCurve.Parameter(Point(-5f, 2.25f), Point(-5f, 5f))
            )
        )

        assertThrows<AssertionError> {
            mutliParameterCurve.isConvex()
        }
    }

    @MethodSource
    @ParameterizedTest
    fun `Curve fits to circle`(data: FitCircle) {
        val circle = data.curve.fitCircle(0.01f)
        assertThat(circle).isEqualTo(data.expectedCircle)
    }

    @MethodSource
    @ParameterizedTest
    fun `Shortcut curve does not fit to circle`(curve: SmoothCubicBezierCurve) {
        val circle = curve.fitCircle(0.01f)
        assertThat(circle).isNull()
    }

    @MethodSource
    @ParameterizedTest
    fun `Point along a curve is computed correctly`(data: ParameterizedCurve) {
        val point = data.curve.interpolate(data.t)
        assertThat(point).isEqualTo(data.expected)
    }

    @MethodSource
    @ParameterizedTest
    fun `Point along a shortcut curve is computed correctly`(data: ShortcutParameterizedCurve) {
        val point = data.curve.interpolate(data.t)
        assertThat(point).isEqualTo(data.expected)
    }

    @MethodSource
    @ParameterizedTest
    fun `Convex curves are convex`(curve: CubicBezierCurve) {
        assertThat(curve.isConvex()).isTrue()
    }

    @MethodSource
    @ParameterizedTest
    fun `Concave curves are not convex`(curve: CubicBezierCurve) {
        assertThat(curve.isConvex()).isFalse()
    }

    @MethodSource
    @ParameterizedTest
    fun `Convex shortcut curves are convex`(curve: SmoothCubicBezierCurve) {
        assertThat(curve.isConvex()).isTrue()
    }

    @MethodSource
    @ParameterizedTest
    fun `Concave shortcut curves are not convex`(curve: SmoothCubicBezierCurve) {
        assertThat(curve.isConvex()).isFalse()
    }

    data class FitCircle(val curve: CubicBezierCurve, val expectedCircle: Circle)
    data class ShortcutFitCircle(val curve: SmoothCubicBezierCurve, val expectedCircle: Circle?)

    data class ParameterizedCurve(val curve: CubicBezierCurve, val t: Float, val expected: Point)
    data class ShortcutParameterizedCurve(val curve: SmoothCubicBezierCurve, val t: Float, val expected: Point)

    companion object {
        @JvmStatic
        fun `Curve fits to circle`(): List<FitCircle> {
            return listOf(
                FitCircle(
                    CubicBezierCurve(
                        CommandVariant.RELATIVE,
                        listOf(
                            CubicBezierCurve.Parameter(Point(2.761f, 0f), Point(5f, 2.239f), Point(5f, 5f))
                        )
                    ),
                    Circle(Point(-0.0005425225f, 5.0005436f), 5.0005436f)
                )
            )
        }

        @JvmStatic
        fun `Shortcut curve does not fit to circle`(): List<SmoothCubicBezierCurve> {
            return listOf(
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(5f, 2.239f), Point(5f, 5f))
                    )
                )
            )
        }

        @JvmStatic
        fun `Point along a curve is computed correctly`(): List<ParameterizedCurve> {
            return listOf(
                ParameterizedCurve(
                    CubicBezierCurve(
                        CommandVariant.RELATIVE,
                        listOf(
                            CubicBezierCurve.Parameter(Point(-5.2f, -1f), Point(-5.1f, 14f), Point(12.4f, 11.1f))
                        )
                    ),
                    0.54f,
                    Point(-1.8822453f, 7.0387707f)
                )
            )
        }

        @JvmStatic
        fun `Point along a shortcut curve is computed correctly`(): List<ShortcutParameterizedCurve> {
            return listOf(
                ShortcutParameterizedCurve(
                    SmoothCubicBezierCurve(
                        CommandVariant.RELATIVE,
                        listOf(
                            SmoothCubicBezierCurve.Parameter(Point(-5.1f, 14f), Point(12.4f, 11.1f))
                        )
                    ),
                    0.54f,
                    Point(-0.099726915f, 7.381562f)
                )
            )
        }

        @JvmStatic
        fun `Convex curves are convex`(): List<CubicBezierCurve> {
            return listOf(
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(-2.5f, 0f), Point(-5f, 2.25f), Point(-5f, 5f))
                    )
                ),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(1.5f, 0.5f), Point(3.5f, 2f), Point(4f, 4f))
                    )
                )
            )
        }

        @JvmStatic
        fun `Concave curves are not convex`(): List<CubicBezierCurve> {
            return listOf(
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(2.5f, 0f), Point(-5f, 2.25f), Point(-5f, 5f))
                    )
                ),
                CubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        CubicBezierCurve.Parameter(Point(109f, 8f), Point(113f, 12f), Point(120f, 10f))
                    )
                )
            )
        }

        @JvmStatic
        fun `Convex shortcut curves are convex`(): List<SmoothCubicBezierCurve> {
            return listOf(
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(5f, 2.25f), Point(5f, 5f))
                    )
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(1.5f, 0.5f), Point(3.5f, 2f))
                    )
                ),
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(109f, 8f), Point(113f, 12f))
                    )
                )
            )
        }

        @JvmStatic
        fun `Concave shortcut curves are not convex`(): List<SmoothCubicBezierCurve> {
            return listOf(
                SmoothCubicBezierCurve(
                    CommandVariant.RELATIVE,
                    listOf(
                        SmoothCubicBezierCurve.Parameter(Point(2.5f, 0f), Point(-5f, 2.25f))
                    )
                )
            )
        }
    }
}
