package com.jzbrooks.avdo.graphic.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

class ParserTests {

    @Test
    fun testParseCompactCommandString() {
        val pathCommandString = "M1,1L2,5V3Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseLooseCommandString() {
        val pathCommandString = "M 1 1 L 2 5 V 3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseCommaSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1,1 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseCommaSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1,2 L2,5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1, 1 to 2)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseSpaceSeparatedPairsWithoutImplicitCommands() {
        val pathCommandString = "M1 1 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseSpaceSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1 1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1, 1 to 2)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseMixedSeparatedPairsWithImplicitCommands() {
        val pathCommandString = "M1,1 1 2 L2 5 V3 Z"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .isEqualTo(MoveTo(VariantCommand.Variant.ABSOLUTE, listOf(1 to 1, 1 to 2)))
        assertThat(commands[1])
                .isEqualTo(LineTo(VariantCommand.Variant.ABSOLUTE, listOf(2 to 5)))
        assertThat(commands[2])
                .isEqualTo(VerticalLineTo(VariantCommand.Variant.ABSOLUTE, listOf(3)))
        assertThat(commands[3])
                .isInstanceOf(ClosePath::class.java)
    }

    @Test
    fun testParseRelativeCommandString() {
        val pathCommandString = "l2 5"

        val commands = CommandString(pathCommandString).toCommandList()

        assertThat(commands[0])
                .prop("variant") { (it as VariantCommand).variant }
                .isEqualTo(VariantCommand.Variant.RELATIVE)
    }
}