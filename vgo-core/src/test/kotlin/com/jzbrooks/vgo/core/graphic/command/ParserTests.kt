package com.jzbrooks.vgo.core.graphic.command

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.first
import assertk.assertions.hasClass
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.jzbrooks.vgo.core.util.math.Point
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlStyle.parameters
import org.junit.jupiter.api.Test

class ParserTests {
    private val moveToSingle = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f)))
    private val moveToImplicit = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f), Point(1f, 2f)))
    private val lineToSingle = LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 5f)))
    private val verticalLineToSingle = VerticalLineTo(CommandVariant.ABSOLUTE, listOf(3f))

    @Test
    fun testParseQuadraticBezierCurve() {
        val pathCommandString = "Q1,3 3,3"

        val commands = CommandString(pathCommandString).toCommandList()

        val expected =
            QuadraticBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(
                    QuadraticBezierCurve.Parameter(Point(1f, 3f), Point(3f, 3f)),
                ),
            )

        assertThat(commands).index(0).isEqualTo(expected)
    }

    @Test
    fun testParseShortcutQuadraticBezierCurve() {
        val pathCommandString = "T1,3"

        val commands = CommandString(pathCommandString).toCommandList()

        val expected = SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(Point(1f, 3f)))

        assertThat(commands).index(0).isEqualTo(expected)
    }

    @Test
    fun testParseCubicBezierCurve() {
        val pathCommandString = "C1,3 3,3 4,3"

        val commands = CommandString(pathCommandString).toCommandList()

        val expected =
            CubicBezierCurve(
                CommandVariant.ABSOLUTE,
                listOf(
                    CubicBezierCurve.Parameter(Point(1f, 3f), Point(3f, 3f), Point(4f, 3f)),
                ),
            )

        assertThat(commands).index(0).isEqualTo(expected)
    }

    @Test
    fun testParseShortcutCubicBezierCurve() {
        val pathCommandString = "S1,3 2,4"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands, "commands").index(0).all {
            prop("variant") { (it as ParameterizedCommand<*>).variant }.isEqualTo(CommandVariant.ABSOLUTE)
            isInstanceOf(SmoothCubicBezierCurve::class)
                .prop(SmoothCubicBezierCurve::parameters)
                .containsOnly(SmoothCubicBezierCurve.Parameter(Point(1f, 3f), Point(2f, 4f)))
        }
    }

    @Test
    fun testParseEllipticalArcCurve() {
        val pathCommandString = "A 1 3 97 1 0 10 10"

        val commands = CommandString(pathCommandString).toCommandList()

        val expected =
            EllipticalArcCurve(
                CommandVariant.ABSOLUTE,
                listOf(
                    EllipticalArcCurve.Parameter(
                        1f,
                        3f,
                        97f,
                        EllipticalArcCurve.ArcFlag.LARGE,
                        EllipticalArcCurve.SweepFlag.ANTICLOCKWISE,
                        Point(10f, 10f),
                    ),
                ),
            )

        assertThat(commands).index(0).isEqualTo(expected)
    }

    @Test
    fun testParseCompactCommandString() {
        val pathCommandString = "M1,1L2,5V3Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseLooseCommandString() {
        val pathCommandString = "M 1 1 L 2 5 V 3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseCommaSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1,1 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseCommaSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1,2 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToImplicit,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseSpaceSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1 1 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseSpaceSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1 1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToImplicit,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testParseMixedSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToImplicit,
            lineToSingle,
            verticalLineToSingle,
            ClosePath,
        )
    }

    @Test
    fun testArcParameterWithoutCommaBetweenFlags() {
        val pathCommandString = "M1,1 A 1,1,0,01,3,3"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            EllipticalArcCurve(
                CommandVariant.ABSOLUTE,
                listOf(
                    EllipticalArcCurve.Parameter(
                        1f,
                        1f,
                        0f,
                        EllipticalArcCurve.ArcFlag.SMALL,
                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                        Point(3f, 3f),
                    ),
                ),
            ),
        )
    }

    @Test
    fun testMultipleArcParametersWithoutCommaBetweenFlags() {
        val pathCommandString = "M1,1 A 1,1,0,01,3,3 2,2,0,10,4,4"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands).containsExactly(
            moveToSingle,
            EllipticalArcCurve(
                CommandVariant.ABSOLUTE,
                listOf(
                    EllipticalArcCurve.Parameter(
                        1f,
                        1f,
                        0f,
                        EllipticalArcCurve.ArcFlag.SMALL,
                        EllipticalArcCurve.SweepFlag.CLOCKWISE,
                        Point(3f, 3f),
                    ),
                    EllipticalArcCurve.Parameter(
                        2f,
                        2f,
                        0f,
                        EllipticalArcCurve.ArcFlag.LARGE,
                        EllipticalArcCurve.SweepFlag.ANTICLOCKWISE,
                        Point(4f, 4f),
                    ),
                ),
            ),
        )
    }

    @Test
    fun testParseRelativeCommandString() {
        val pathCommandString = "l2 5"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands)
            .index(0)
            .prop("variant") { (it as ParameterizedCommand<*>).variant }
            .isEqualTo(CommandVariant.RELATIVE)
    }

    @Test
    fun testParseFloatingPointCoordinate() {
        val pathCommandString = "l2.1 5"

        val commands = CommandString(pathCommandString).toCommandList()

        val lineCommand = commands[0] as LineTo

        assertThat(lineCommand::parameters).first().isEqualTo(Point(2.1f, 5f))
    }

    @Test
    fun testExponentialNotationCoordinate() {
        val pathCommandString = "l2e2 5"

        val commands = CommandString(pathCommandString).toCommandList()

        val lineCommand = commands[0] as LineTo

        assertThat(lineCommand::parameters).first().isEqualTo(Point(200f, 5f))
    }

    @Test
    fun testInvalidCommandParsing() {
        val commandString = CommandString("G 3 2")

        assertFailure {
            commandString.toCommandList()
        }.hasClass(IllegalStateException::class)
    }
}
