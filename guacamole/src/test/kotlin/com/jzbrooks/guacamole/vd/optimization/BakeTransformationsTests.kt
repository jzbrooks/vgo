package com.jzbrooks.guacamole.vd.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
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
}