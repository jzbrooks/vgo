package com.jzbrooks.guacamole.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.Test

class RemoveEmptyGroupsTests {
    @Test
    fun testCollapseNestedEmptyGroup() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf()))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var attributes = mutableMapOf<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

        assertThat(graphic.elements).hasSize(0)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithPath() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf(Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 2f))))))))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var attributes = mutableMapOf<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

        assertThat(graphic.elements).hasSize(1)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithAttributes() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(emptyList(), mutableMapOf("android:name" to "base")))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var attributes = mutableMapOf<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

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
            override var attributes = mutableMapOf<String, String>()
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

        assertThat(graphic.elements.filterIsInstance<Path>()).hasSize(1)
    }
}