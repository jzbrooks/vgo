package com.jzbrooks.guacamole.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isGreaterThan
import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.*
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import org.junit.Test

class CommandVariantTests {
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

        val copy = path.copy()

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(path)
            override var attributes = emptyMap<String, String>()
        }

        CommandVariant().optimize(graphic)

        // M100,1 L103,6 L106,7 93,10 C109,8 113,12 120,10 H101 V-8 H103 S113,39 105,-6 Q112,-10 109, -3 T100,0 A4,4,93,1,1,109,15 Z
        // M100,1 l3,5 l3,1 -13,3 c16,-2 20,2 27,0 H101 V-8 h2 s10,47 2,2 q7,-4 4,3 t-9,3 a4,3,93,1,1,9,15 Z
        assertThat(path.commands.filterIsInstance<VariantCommand>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(8)
        assertThat(copy.commands.joinToString("").length).isGreaterThan(path.commands.joinToString("").length)
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

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(path)
            override var attributes = emptyMap<String, String>()
        }

        CommandVariant().optimize(graphic)

        assertThat(path.commands.filterIsInstance<VariantCommand>().filter { it.variant == CommandVariant.ABSOLUTE }).hasSize(1)
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

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(path)
            override var attributes = emptyMap<String, String>()
        }

        CommandVariant().optimize(graphic)

        assertThat(path.commands.filterIsInstance<VariantCommand>().filter { it.variant == CommandVariant.ABSOLUTE }).hasSize(3)
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

        val copy = path.copy()

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(path)
            override var attributes = emptyMap<String, String>()
        }

        CommandVariant().optimize(graphic)

        // M100,1 L103,6 L106,7 93,10 C109,8 113,12 120,10 M110,8 H101 V-8 Z H103 S113,39 105,-6 Q112,-10 109,-3 T100,0 A4,3,93,1,1,109,15 Z
        // M100,1 l3,5 l3,1 -13,3 c16,-2 20,2 27,0 M110,8 h-9 V-8 Z H103 s10,29 2,-16 q7,-4 4,3 t-9,3 a4,3,93,1,1,9,15 Z
        assertThat(path.commands.filterIsInstance<VariantCommand>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(8)
        assertThat(copy.commands.joinToString("").length).isGreaterThan(path.commands.joinToString("").length)
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

        val copy = path.copy()

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(path)
            override var attributes = emptyMap<String, String>()
        }

        CommandVariant().optimize(graphic)

        // M100,1 101,1 L103,6 L106,7 93,10 Z
        // m100,1 1,0 l2,5 l3,1 -13,3 Z
        assertThat(path.commands.filterIsInstance<VariantCommand>().filter { it.variant == CommandVariant.RELATIVE }).hasSize(3)
        assertThat(copy.commands.joinToString("").length).isGreaterThan(path.commands.joinToString("").length)
    }
}