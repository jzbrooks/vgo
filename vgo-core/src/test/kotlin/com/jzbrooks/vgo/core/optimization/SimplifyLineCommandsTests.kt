package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class SimplifyLineCommandsTests {
    @Test
    fun testSimplifyLineToAsVerticalLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(0f, 6f))),
                        ClosePath
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
                        LineTo(CommandVariant.RELATIVE, listOf(Point(6f, 0f))),
                        ClosePath
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
                        LineTo(CommandVariant.RELATIVE, listOf(Point(0.0000000000001f, 6f))),
                        ClosePath
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
                        LineTo(CommandVariant.RELATIVE, listOf(Point(6f, 0.0000000000001f))),
                        ClosePath
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
                        LineTo(CommandVariant.RELATIVE, listOf(Point(0f, 6f), Point(10f, 0f))),
                        ClosePath
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<LineTo>()).hasSize(1)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(0)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(0)
    }

    @Test
    fun testCollapseSequentialPositiveHorizontalLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(12f)),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(15f)),
                        ClosePath
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(1)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>().single().parameters).containsOnly(27f)
    }

    @Test
    fun testDoNotCollapseSequentialHorizontalLineToWithOppositeSign() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(12f)),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(-15f)),
                        ClosePath
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()).hasSize(2)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()[0].parameters).containsOnly(12f)
        assertThat(path.commands.filterIsInstance<HorizontalLineTo>()[1].parameters).containsOnly(-15f)
    }

    @Test
    fun testCollapseSequentialPositiveVerticalLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(5f)),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(15f)),
                        ClosePath
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(1)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>().single().parameters).containsOnly(20f)
    }

    @Test
    fun testDoNotCollapseSequentialVerticalLineToWithOppositeSign() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(12f)),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(-5f)),
                        ClosePath
                )
        )

        SimplifyLineCommands(0f).visit(path)

        assertThat(path.commands.filterIsInstance<VerticalLineTo>()).hasSize(2)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()[0].parameters).containsOnly(12f)
        assertThat(path.commands.filterIsInstance<VerticalLineTo>()[1].parameters).containsOnly(-5f)
    }
}