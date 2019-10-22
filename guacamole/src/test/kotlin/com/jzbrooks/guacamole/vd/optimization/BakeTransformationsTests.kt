package com.jzbrooks.guacamole.vd.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.LineTo
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.Test

class BakeTransformationsTests {
    @Test
    fun testTransformationAttributesRemoved() {
        val elements = listOf<Element>(
                Group(
                        listOf(
                                Group(listOf(
                                        Path(listOf(
                                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                                        ))))
                        ),
                        mutableMapOf("android:translateX" to "14", "android:translateY" to "14")
                )
        )
        val graphic = object : Graphic {
            override var elements: List<Element> = elements
            override var attributes: MutableMap<String, String> = mutableMapOf()
        }

        BakeTransformations().optimize(graphic)

        val parentGroup = graphic.elements.first() as Group
        val childGroup = parentGroup.elements.first() as Group
        assertThat(parentGroup.attributes).doesNotContainKey("android:translateX")
        assertThat(parentGroup.attributes).doesNotContainKey("android:translateY")
        assertThat(childGroup.attributes).doesNotContainKey("android:translateX")
        assertThat(childGroup.attributes).doesNotContainKey("android:translateY")
    }

    @Test
    fun testAncestorGroupTransformationAppliedToPathElements() {
        val elements = listOf<Element>(
                Group(
                        listOf(
                                Group(listOf(
                                        Path(listOf(
                                                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 10f))),
                                                LineTo(CommandVariant.ABSOLUTE, listOf(Point(40f, 4f)))
                                        ))))
                        ),
                        mutableMapOf("android:translateX" to "14", "android:translateY" to "14")
                )
        )
        val graphic = object : Graphic {
            override var elements: List<Element> = elements
            override var attributes: MutableMap<String, String> = mutableMapOf()
        }

        BakeTransformations().optimize(graphic)

        val path = ((graphic.elements.first() as Group).elements.first() as Group).elements.first() as Path
        assertThat(path.commands)
                .containsExactly(
                        MoveTo(CommandVariant.ABSOLUTE, listOf(Point(24f, 24f))),
                        LineTo(CommandVariant.ABSOLUTE, listOf(Point(54f, 18f)))
                )
    }
}