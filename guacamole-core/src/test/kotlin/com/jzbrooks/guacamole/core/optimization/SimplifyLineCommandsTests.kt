package com.jzbrooks.guacamole.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.*
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.util.math.Point
import kotlin.test.Test

class SimplifyLineCommandsTests {
    @Test
    fun testSimplifyLineToAsVerticalLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 6f))),
                        ClosePath()
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(1)
    }

    @Test
    fun testSimplifyLineToAsHorizontalLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(6f, 0f))),
                        ClosePath()
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(1)
    }

    @Test
    fun testSimplifyLineToAsVerticalLineToInTolerance() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(0.0000000000001f, 6f))),
                        ClosePath()
                )
        )

        SimplifyLineCommands(0.0001f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(1)
    }

    @Test
    fun testSimplifyLineToAsHorizontalLineToInTolerance() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(6f, 0.0000000000001f))),
                        ClosePath()
                )
        )

        SimplifyLineCommands(0.0001f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(1)
    }

    @Test
    fun testDoesNotSimplifyLineToWithMultipleParameters() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 6f), Point(10f, 0f))),
                        ClosePath()
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(1)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(0)
    }
}