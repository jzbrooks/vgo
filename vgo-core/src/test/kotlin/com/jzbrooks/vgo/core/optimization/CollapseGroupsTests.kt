package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.vgo.core.graphic.Attributes
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class CollapseGroupsTests {
    @Test
    fun testCollapseSingleUnnecessaryGroup() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(innerPath))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testCollapseSingleUnnecessaryNestedGroups() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(Group(listOf(innerPath))))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testRetainNestedGroupWithAttributes() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val innerGroupWithAttributes = Group(listOf(innerPath), Group.Attributes(null, mutableMapOf("android:scaleX" to "20")))
        val group = Group(listOf(innerGroupWithAttributes))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = object : Attributes {
                override val name: String? = null
                override val foreign: MutableMap<String, String> = mutableMapOf()
            }
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerGroupWithAttributes)
    }
}
