package com.jzbrooks.guacamole.graphic.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class StringConversionTests {
    @Test
    fun testMoveToStringConversion() {
        val moveTo = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 2f), Point(2f, 3f)))
        val moveToString = moveTo.toString()
        assertThat(moveToString).isEqualTo("M1,2 2,3")
    }

    @Test
    fun testLineToStringConversion() {
        val lineTo = LineTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 2f), Point(2f, 3f)))
        val lineToString = lineTo.toString()
        assertThat(lineToString).isEqualTo("L1,2 2,3")
    }

    @Test
    fun testVerticalLineToStringConversion() {
        val verticalLineTo = VerticalLineTo(CommandVariant.ABSOLUTE, listOf(1f, 2f))
        val verticalLineToString = verticalLineTo.toString()
        assertThat(verticalLineToString).isEqualTo("V1 2")
    }

    @Test
    fun testHorizontalLineToStringConversion() {
        val horizontalLineTo = HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(1f, 2f))
        val horizontalLineToString = horizontalLineTo.toString()
        assertThat(horizontalLineToString).isEqualTo("H1 2")
    }

    @Test
    fun testClosePathStringConversion() {
        val closePath = ClosePath()
        val closePathString = closePath.toString()
        assertThat(closePathString).isEqualTo("Z")
    }

    @Test
    fun testQuadraticBezierCurveConversion() {
        val quadraticBezierCurve = QuadraticBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(QuadraticBezierCurve.Parameter(Point(1f, 2f), Point(3f, 2.4f)))
        )
        val quadraticBezierCurveString = quadraticBezierCurve.toString()
        assertThat(quadraticBezierCurveString).isEqualTo("Q1,2 3,2.4")
    }

    @Test
    fun testShortcutQuadraticBezierCurveConversion() {
        val shortcutQuadraticBezierCurve = ShortcutQuadraticBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(Point(1f, 2f), Point(3f, 2.4f))
        )
        val shortcutQuadraticBezierCurveString = shortcutQuadraticBezierCurve.toString()
        assertThat(shortcutQuadraticBezierCurveString).isEqualTo("T1,2 3,2.4")
    }

    @Test
    fun testCubicBezierCurveConversion() {
        val cubicBezierCurve = CubicBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(CubicBezierCurve.Parameter(Point(1f, 2f), Point(3f, 2.4f), Point(5f, 6f)))
        )
        val cubicBezierCurveString = cubicBezierCurve.toString()
        assertThat(cubicBezierCurveString).isEqualTo("C1,2 3,2.4 5,6")
    }

    @Test
    fun testShortcutCubicBezierCurveConversion() {
        val shortcutCubicBezierCurve = ShortcutCubicBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(ShortcutCubicBezierCurve.Parameter(Point(1f, 2f), Point(3f, 2.4f)))
        )
        val shortcutCubicBezierCurveString = shortcutCubicBezierCurve.toString()
        assertThat(shortcutCubicBezierCurveString).isEqualTo("S1,2 3,2.4")
    }

    @Test
    fun testEllipticalArcConversion() {
        val ellipticalArcCurve = EllipticalArcCurve(
                CommandVariant.ABSOLUTE,
                listOf(EllipticalArcCurve.Parameter(1f, 3f, 120f, EllipticalArcCurve.ArcFlag.LARGE, EllipticalArcCurve.SweepFlag.ANTICLOCKWISE, Point(3f, 2f)))
        )
        val ellipticalArcCurveString = ellipticalArcCurve.toString()
        assertThat(ellipticalArcCurveString).isEqualTo("A1,3,120,1,0,3,2")
    }

    @Test
    fun testCommandListConversion() {

    }
}