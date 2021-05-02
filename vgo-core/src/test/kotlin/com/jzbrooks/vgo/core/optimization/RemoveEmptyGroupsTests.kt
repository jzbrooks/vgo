package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class RemoveEmptyGroupsTests {
    @Test
    fun testCollapseNestedEmptyGroup() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf()))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
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
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

        assertThat(graphic.elements).hasSize(1)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithAttributes() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(emptyList(), Group.Attributes("base", mutableMapOf())))))))

        val graphic = object : Graphic {
            override var elements: List<Element> = nestedEmptyGroups
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
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
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
        }

        val emptyGroups = RemoveEmptyGroups()
        emptyGroups.optimize(graphic)

        assertThat(graphic.elements.filterIsInstance<Path>()).hasSize(1)
    }
}