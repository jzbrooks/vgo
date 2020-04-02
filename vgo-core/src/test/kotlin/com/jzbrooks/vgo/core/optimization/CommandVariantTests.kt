package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.optimization.CommandVariant as CommandVariantOpt
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class CommandVariantTests {
    // This is okay because this optimization only ever compares
    // two commands of the same type, which should have similar
    // string representations by default
    private val commandPrinter = object : CommandPrinter {
        override fun print(command: Command) = command.toString()
    }

    @Test
    fun testConvertOnlyAbsoluteCommands() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(103f, 6f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f), Point(93f, 10f))),
                        CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(Point(109f, 8f), Point(113f, 12f), Point(120f, 10f)))),
                        HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(101f)),
                        VerticalLineTo(CommandVariant.ABSOLUTE, listOf(-8f)),
                        HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(103f)),
                        ShortcutCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(ShortcutCubicBezierCurve.Parameter(Point(113f, 39f), Point(105f, -6f)))),
                        QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(112f, -10f), Point(109f, -3f)))),
                        ShortcutQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(Point(100f, 0f))),
                        EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(4f, 3f, 93f, EllipticalArcCurve.ArcFlag.LARGE, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(109f, 15f)))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        // M100,1 L103,6 L106,7 93,10 C109,8 113,12 120,10 H101 V-8 H103 S113,39 105,-6 Q112,-10 109, -3 T100,0 A4,4,93,1,1,109,15 Z
        // M100,1 l3,5 l3,1 -13,3 c16,-2 20,2 27,0 H101 V-8 h2 s10,47 2,2 q7,-4 4,3 t-9,3 a4,3,93,1,1,9,15 Z
        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(8)
    }

    @Test
    fun testConvertOnlyRelativeCommands() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 1f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(3f, 7f))),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(0f)),
                        VerticalLineTo(CommandVariant.RELATIVE, listOf(-4f))
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.ABSOLUTE }).hasSize(1)
    }

    @Test
    fun testConvertMixedCommands() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.RELATIVE, listOf(Point(10f, 1f))),
                        LineTo(CommandVariant.RELATIVE, listOf(Point(-9f, 6f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(3f, 7f))),
                        HorizontalLineTo(CommandVariant.RELATIVE, listOf(0f)),
                        VerticalLineTo(CommandVariant.ABSOLUTE, listOf(-14f))
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.ABSOLUTE }).hasSize(3)
    }

    @Test
    fun testConvertCommandsWithSubPath() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(103f, 6f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f), Point(93f, 10f))),
                        CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(Point(109f, 8f), Point(113f, 12f), Point(120f, 10f)))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(110f, 8f))),
                        HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(101f)),
                        VerticalLineTo(CommandVariant.ABSOLUTE, listOf(-8f)),
                        ClosePath(),
                        HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(103f)),
                        ShortcutCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(ShortcutCubicBezierCurve.Parameter(Point(113f, 39f), Point(105f, -6f)))),
                        QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(QuadraticBezierCurve.Parameter(Point(112f, -10f), Point(109f, -3f)))),
                        ShortcutQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(Point(100f, 0f))),
                        EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(EllipticalArcCurve.Parameter(4f, 3f, 93f, EllipticalArcCurve.ArcFlag.LARGE, EllipticalArcCurve.SweepFlag.CLOCKWISE, Point(109f, 15f)))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(9)
    }

    @Test
    fun testConvertMoveToWithImplicitLineTo() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f), Point(101f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(103f, 6f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f), Point(93f, 10f))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        // M100,1 101,1 L103,6 L106,7 93,10 Z
        // M100,1 101,1 l2,5 l3,1 -13,3 Z
        // todo: the case of implicit commands in the initial moveto could be handled better
        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(2)
    }

    @Test
    fun testComputedRelativeCommandUpdatesCurrentPointByAllComponents() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(CubicBezierCurve.Parameter(Point(105f, 8f), Point(115f, 10f), Point(100f, 10f)))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        assertThat(path.commands.filterIsInstance<ParameterizedCommand<*>>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(2)
    }

    @Test
    fun testNestedSubpathConversion() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(15f, 5f))),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f))),
                        ClosePath(),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(107f, 7f))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        val lastLineTo = path.commands.filterIsInstance<LineTo>().last()
        assertThat(lastLineTo.variant).isEqualTo(CommandVariant.RELATIVE)
        assertThat(lastLineTo.parameters.last()).isEqualTo(Point(7f, 6f))
    }

    @Test
    fun testSequentialSubpathConversion() {
        val path = Path(
                listOf(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(100f, 1f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(15f, 5f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(106f, 7f))),
                        ClosePath(),
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(17f, 21f))),
                        ClosePath()
                )
        )

        CommandVariantOpt(CommandVariantOpt.Mode.Compact(commandPrinter)).visit(path)

        val lastLineTo = path.commands.filterIsInstance<LineTo>().last()
        assertThat(lastLineTo.variant).isEqualTo(CommandVariant.RELATIVE)
        assertThat(lastLineTo.parameters.last()).isEqualTo(Point(7f, 6f))
    }
}