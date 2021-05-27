package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.containsNone
import assertk.assertions.hasSize
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class RemoveRedundantCommandsTests {
    @Test
    fun testRedundantLineToIsRemoved() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(0f, 0f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(103f, 6f))),
                ClosePath,
            )
        )

        RemoveRedundantCommands().visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(1)
        assertThat(path::commands).hasSize(3)
    }

    @Test
    fun testUniqueCommandsAreNotModified() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.RELATIVE, listOf(Point(100f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(103f, 6f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(106f, 7f), Point(93f, 10f))),
                ClosePath,
            )
        )

        RemoveRedundantCommands().visit(path)

        assertThat(path::commands).hasSize(4)
    }

    @Test
    fun testRedundantClosePathsAreRemoved() {
        val path = createPath(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(1f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 1f))),
                LineTo(CommandVariant.RELATIVE, listOf(Point(-3f, -2f))),
                ClosePath,
            )
        )

        RemoveRedundantCommands().visit(path)

        assertThat(path::commands).containsNone(ClosePath)
    }
}
