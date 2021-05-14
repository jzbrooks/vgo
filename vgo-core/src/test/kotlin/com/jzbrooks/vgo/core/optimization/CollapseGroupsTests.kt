package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class CollapseGroupsTests {
    @Test
    fun testCollapseSingleUnnecessaryGroup() {
        val innerPath = Path(
            listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )
        val group = Group(listOf(innerPath))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testCollapseSingleUnnecessaryNestedGroups() {
        val innerPath = Path(
            listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )

        val group = Group(listOf(Group(listOf(innerPath))))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testRetainNestedGroupWithAttributes() {
        val scale = Matrix3.from(
            arrayOf(
                floatArrayOf(20f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, 0f, 1f),
            ),
        )

        val innerPath = Path(
            listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))),
            null,
            mutableMapOf(),
            Colors.BLACK,
        )
        val innerGroupWithAttributes = Group(listOf(innerPath), null, mutableMapOf(), scale)
        val group = Group(listOf(innerGroupWithAttributes))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override val id: String? = null
            override val foreign: MutableMap<String, String> = mutableMapOf()
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerGroupWithAttributes)
    }
}
