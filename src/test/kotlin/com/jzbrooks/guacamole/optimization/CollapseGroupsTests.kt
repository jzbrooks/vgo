package com.jzbrooks.guacamole.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.CommandVariant
import com.jzbrooks.guacamole.graphic.command.MoveTo
import com.jzbrooks.guacamole.graphic.command.Point
import org.junit.Test

class CollapseGroupsTests {
    @Test
    fun testCollapseSingleUnnecessaryGroup() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(innerPath))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        CollapseGroups().visit(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testCollapseSingleUnnecessaryNestedGroups() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(Group(listOf(innerPath))))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        CollapseGroups().visit(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testRetainNestedGroupWithAttributes() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val innerGroupWithAttributes = Group(listOf(innerPath), mapOf("android:scaleX" to "20"))
        val group = Group(listOf(innerGroupWithAttributes))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var size = Size(Dimension(100), Dimension(100))
            override var viewBox = ViewBox(0, 0, 100, 100)
            override var metadata = emptyMap<String, String>()
        }

        CollapseGroups().visit(graphic)

        assertThat(graphic.elements).containsExactly(innerGroupWithAttributes)
    }
}