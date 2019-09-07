package com.jzbrooks.avdo.graphic.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

class ParserTests {
    private val moveToSingle = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f)))
    private val moveToImplicit = MoveTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f), Point(1f, 2f)))
    private val lineToSingle = LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 5f)))
    private val verticalLineToSingle = VerticalLineTo(CommandVariant.ABSOLUTE, listOf(3f))

    @Test
    fun testParseCompactCommandString() {
        val pathCommandString = "M1,1L2,5V3Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToSingle)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseLooseCommandString() {
        val pathCommandString = "M 1 1 L 2 5 V 3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToSingle)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseCommaSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1,1 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToSingle)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseCommaSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1,2 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToImplicit)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseSpaceSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1 1 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToSingle)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseSpaceSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1 1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToImplicit)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseMixedSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(moveToImplicit)
        assertThat(commands[1])
                .isEqualTo(lineToSingle)
        assertThat(commands[2])
                .isEqualTo(verticalLineToSingle)
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseRelativeCommandString() {
        val pathCommandString = "l2 5"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .prop("variant") { (it as VariantCommand).variant }
                .isEqualTo(CommandVariant.RELATIVE)
    }

    @Test
    fun testParseFloatingPointCoordinate() {
        val pathCommandString = "l2.1 5"

        val commands = CommandString(pathCommandString).toCommandList()

        val lineCommand = commands[0] as LineTo

        assertThat(lineCommand.arguments[0])
                .isEqualTo(Point(2.1f, 5f))
    }

    @Test
    fun testExponentialNotationCoordinate() {
        val pathCommandString = "l2e2 5"

        val commands = CommandString(pathCommandString).toCommandList()

        val lineCommand = commands[0] as LineTo

        assertThat(lineCommand.arguments[0])
                .isEqualTo(Point(200f, 5f))
    }
}