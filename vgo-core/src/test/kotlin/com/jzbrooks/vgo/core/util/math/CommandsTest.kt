package com.jzbrooks.vgo.core.util.math

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import org.junit.jupiter.api.Test

class CommandsTest {
    @Test
    fun `Absolute coordinates are correctly computed from relative commands`() {
        val commands = listOf<Command>(
            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 1f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(3f, 7f))),
            HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
            VerticalLineTo(CommandVariant.RELATIVE, listOf(-4f))
        )

        val absoluteCoordinates = computeAbsoluteCoordinates(commands)

        assertThat(absoluteCoordinates).isEqualTo(Point(14f, 10f))
    }

    @Test
    fun `Closed paths in relative commands compute absolute coordinates correctly`() {
        val commands = listOf(
            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 1f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(3f, 7f))),
            HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
            VerticalLineTo(CommandVariant.RELATIVE, listOf(-4f)),
            ClosePath
        )

        val absoluteCoordinates = computeAbsoluteCoordinates(commands)

        assertThat(absoluteCoordinates).isEqualTo(Point(10f, 1f))
    }

    @Test
    fun `Subpaths before relative commands compute absolute coordinates correctly`() {
        val commands = listOf(
            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 1f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
            MoveTo(CommandVariant.RELATIVE, listOf(Point(1f, 1f))),
            LineTo(CommandVariant.RELATIVE, listOf(Point(3f, 7f))),
            ClosePath,
            HorizontalLineTo(CommandVariant.RELATIVE, listOf(10f)),
            VerticalLineTo(CommandVariant.RELATIVE, listOf(-4f)),
            ClosePath
        )

        val absoluteCoordinates = computeAbsoluteCoordinates(commands.take(6))

        assertThat(absoluteCoordinates).isEqualTo(Point(12f, 8f))
    }
}
