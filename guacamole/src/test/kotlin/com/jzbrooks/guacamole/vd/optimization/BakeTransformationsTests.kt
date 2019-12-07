package com.jzbrooks.guacamole.vd.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.ClosePath
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.HorizontalLineTo
import com.jzbrooks.guacamole.core.graphic.command.LineTo
import com.jzbrooks.guacamole.core.graphic.command.VerticalLineTo
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.jupiter.api.Test

class BakeTransformationsTests {
    @Test
    fun testTransformationAttributesRemoved() {
        val group = Group(
                listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                        ))
                ),
                mutableMapOf("android:translateX" to "14", "android:translateY" to "14")
        )

        BakeTransformations().visit(group)

        assertThat(group.attributes).doesNotContainKey("android:translateX")
        assertThat(group.attributes).doesNotContainKey("android:translateY")
    }

    @Test
    fun testAncestorGroupTransformationAppliedToPathElements() {
        val group = Group(
                listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                        ))
                ),
                mutableMapOf("android:translateX" to "14", "android:translateY" to "14")
        )

        BakeTransformations().visit(group)

        val path = group.elements.first() as Path
        assertThat(path.commands)
                .containsExactly(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(54f, 18f)))
                )
    }

    @Test
    fun testBakeHandlesRelativeCommands() {
        val group = Group(
                listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                LineTo(CommandVariant.RELATIVE, listOf(Point(4f, 4f))),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 4f))),
                                ClosePath()
                        ))
                ),
                mutableMapOf("android:translateX" to "14", "android:translateY" to "14")
        )

        BakeTransformations().visit(group)

        val path = group.elements.first() as Path
        assertThat(path.commands)
                .containsExactly(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(28f, 28f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(18f, 18f))),
                        ClosePath()
                )
    }

    @Test
    fun testGroupRotationApplied() {
        val group = Group(
                listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                LineTo(CommandVariant.RELATIVE, listOf(Point(1f, 1f)))
                        ))
                ),
                mutableMapOf("android:rotation" to "90")
        )

        BakeTransformations().visit(group)

        val path = group.elements.first() as Path
        val firstCommand = path.commands[0] as MoveTo
        val secondCommand = path.commands[1] as LineTo

        assertThat(firstCommand.parameters.first().x).isCloseTo(-10f, 0.001f)
        assertThat(firstCommand.parameters.first().y).isCloseTo(10f, 0.001f)
        assertThat(secondCommand.parameters.first().x).isCloseTo(-11f, 0.001f)
        assertThat(secondCommand.parameters.first().y).isCloseTo(11f, 0.001f)
    }

    @Test
    fun testGroupRotationAppliedWithSequentialRelativeCommands() {
        val group = Group(
                listOf(
                        Path(listOf(
                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                HorizontalLineTo(CommandVariant.RELATIVE, listOf(4f)),
                                VerticalLineTo(CommandVariant.RELATIVE, listOf(4f)),
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 14f))),
                                ClosePath()
                        ))
                ),
                mutableMapOf("android:pivotX" to "10", "android:pivotY" to "10", "android:rotation" to "15")
        )

        BakeTransformations().visit(group)

        val path = group.elements.first() as Path
        val firstCommand = path.commands[0] as MoveTo
        val secondCommand = path.commands[1] as LineTo
        val thirdCommand = path.commands[2] as LineTo
        val fourthCommand = path.commands[3] as LineTo

        assertThat(firstCommand).isEqualTo(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))))

        assertThat(secondCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(secondCommand.parameters.first().x).isCloseTo(13.863f, 0.001f)
        assertThat(secondCommand.parameters.first().y).isCloseTo(11.035f, 0.001f)

        assertThat(thirdCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(thirdCommand.parameters.first().x).isCloseTo(12.828f, 0.001f)
        assertThat(thirdCommand.parameters.first().y).isCloseTo(14.899f, 0.001f)

        assertThat(fourthCommand.variant).isEqualTo(CommandVariant.ABSOLUTE)
        assertThat(fourthCommand.parameters.first().x).isCloseTo(8.965f, 0.001f)
        assertThat(fourthCommand.parameters.first().y).isCloseTo(13.864f, 0.001f)
    }
}