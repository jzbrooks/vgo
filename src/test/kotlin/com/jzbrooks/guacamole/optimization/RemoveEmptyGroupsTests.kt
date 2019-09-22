package com.jzbrooks.guacamole.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import com.jzbrooks.guacamole.graphic.command.MoveTo
import com.jzbrooks.guacamole.graphic.command.Point
import org.junit.Test

class RemoveEmptyGroupsTests {
    @Test
    fun testCollapseNestedEmptyGroup() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf()))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var attributes = emptyMap<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.visit(graphic)

        assertThat(graphic.elements).hasSize(0)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithPath() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf(Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 2f))))))))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var attributes = emptyMap<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.visit(graphic)

        assertThat(graphic.elements).hasSize(1)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithAttributes() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(emptyList(), mapOf("android:name" to "base")))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var attributes = emptyMap<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.visit(graphic)

        assertThat(graphic.elements).hasSize(1)
    }

    @Test
    fun testCollapseEmptyGroupAndAvoidAdjacentElements() {
        val nestedEmptyGroups = listOf(
                Group(emptyList()),
                Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 2f)))))
        )

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var attributes = emptyMap<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.visit(graphic)

        assertThat(graphic.elements.filterIsInstance<Path>()).hasSize(1)
    }
}