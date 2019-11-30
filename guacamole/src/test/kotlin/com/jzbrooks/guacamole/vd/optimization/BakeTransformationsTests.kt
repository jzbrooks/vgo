package com.jzbrooks.guacamole.vd.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.ClosePath
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.LineTo
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
                        LineTo(CommandVariant.RELATIVE, listOf(Point(4f, 4f))),
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
                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(1f, 1f)))
                        ))
                ),
                mutableMapOf("android:rotation" to "90")
        )

        BakeTransformations().visit(group)

        val path = group.elements.first() as Path
        val firstCommand = path.commands[0] as MoveTo
        val secondCommand = path.commands[1] as LineTo

        assertThat(firstCommand.parameters.first().x).isCloseTo(-10f, 0.00001f)
        assertThat(firstCommand.parameters.first().y).isCloseTo(10f, 0.00001f)
        assertThat(secondCommand.parameters.first().x).isCloseTo(-1f, 0.00001f)
        assertThat(secondCommand.parameters.first().y).isCloseTo(1f, 0.00001f)
    }
}