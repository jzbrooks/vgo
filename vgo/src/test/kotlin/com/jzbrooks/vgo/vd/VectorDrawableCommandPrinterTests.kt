package com.jzbrooks.vgo.vd

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class VectorDrawableCommandPrinterTests {
    @Test
    fun testPrintMoveTo() {
        val command = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1.03424f, 2.0f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("M1.034,2")
    }

    @Test
    fun testPrintLineTo() {
        val command = LineTo(CommandVariant.ABSOLUTE, listOf(Point(1.03424f, 2.0f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("L1.034,2")
    }

    @Test
    fun testPrintVerticalLineTo() {
        val command = VerticalLineTo(CommandVariant.ABSOLUTE, listOf(1.03424f))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("V1.034")
    }

    @Test
    fun testPrintHorizontalLineTo() {
        val command = HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(1.03424f))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("H1.034")
    }

    @Test
    fun testPrintCubicBezierCurve() {
        val command = CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(
                CubicBezierCurve.Parameter(
                        Point(1.03424f, 2.0f),
                        Point(1.3924f, 1.0f),
                        Point(1.99424f, 12.3f)
                )
        ))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("C1.034,2 1.392,1 1.994,12.3")
    }

    @Test
    fun testPrintShortcutCubicBezierCurve() {
        val command = ShortcutCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(
                ShortcutCubicBezierCurve.Parameter(
                        Point(1.03424f, 2.0f),
                        Point(1.3924f, 1.0f)
                )
        ))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("S1.034,2 1.392,1")
    }

    @Test
    fun testPrintQuadraticBezierCurve() {
        val command = QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(
                QuadraticBezierCurve.Parameter(
                        Point(1.03424f, 2.0f),
                        Point(1.3924f, 1.0f)
                )
        ))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("Q1.034,2 1.392,1")
    }

    @Test
    fun testPrintShortcutQuadraticBezierCurve() {
        val command = ShortcutQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(Point(1.03424f, 2.0f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("T1.034,2")
    }

    @Test
    fun testPrintEllipticalArcCurve() {
        val command = EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(
                EllipticalArcCurve.Parameter(
                        12f,
                        10.5f,
                        30f,
                        EllipticalArcCurve.ArcFlag.SMALL,
                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                        Point(59f, 12.0124f)
                )
        ))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("A12,10.5,30,0,1,59,12.012")
    }

    @Test
    fun testPrintClosePath() {
        val result = VectorDrawableCommandPrinter(3).print(ClosePath)
        assertThat(result).isEqualTo("Z")
    }

    @Test
    fun testPrintRoundedCoordinates() {
        val command = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0.03494f, 0.012415f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("M.035.012")
    }

    @Test
    fun testPrintCoordinatesNearEpsilonAsIntegers() {
        val command = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0.00094f, 0.00015f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("M0,0")
    }

    @Test
    fun testPrintCoordinatesWithCascadeRoundNearEpsilonAsIntegers() {
        val command = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0.999994f, 1.999995f)))
        val result = VectorDrawableCommandPrinter(3).print(command)
        assertThat(result).isEqualTo("M1,2")
    }
}