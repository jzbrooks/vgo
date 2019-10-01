package com.jzbrooks.guacamole.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.Group
import com.jzbrooks.guacamole.graphic.Path
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import com.jzbrooks.guacamole.graphic.command.LineTo
import com.jzbrooks.guacamole.graphic.command.MoveTo
import com.jzbrooks.guacamole.util.math.MutableMatrix3
import com.jzbrooks.guacamole.util.math.Point
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
                        transform = MutableMatrix3().apply {
                            this[0, 2] = 14f
                            this[1, 2] = 14f
                        }
                )
        )
        val graphic = object : Graphic {
            override var elements: List<Element> = elements
            override var attributes: Map<String, String> = emptyMap()
        }

        BakeTransformations().optimize(graphic)

        val firstGroup = graphic.elements.first() as Group
        val secondGroup = firstGroup.elements.first() as Group
        val groupTransforms = listOf(firstGroup.transform, secondGroup.transform)
        assertThat(groupTransforms).containsExactly(null, null)
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
                        transform = MutableMatrix3().apply {
                            this[0, 2] = 14f
                            this[1, 2] = 14f
                        }
                )
        )
        val graphic = object : Graphic {
            override var elements: List<Element> = elements
            override var attributes: Map<String, String> = emptyMap()
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