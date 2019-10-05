package com.jzbrooks.guacamole.core.optimization

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.guacamole.core.graphic.Element
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.util.math.Point
import org.junit.Test

class CollapseGroupsTests {
    @Test
    fun testCollapseSingleUnnecessaryGroup() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(innerPath))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = emptyMap<String, String>()
            override val optimizationRegistry = object : OptimizationRegistry {
                override val optimizations = emptyList<Optimization>()
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
            override var attributes = emptyMap<String, String>()
            override val optimizationRegistry = object : OptimizationRegistry {
                override val optimizations = emptyList<Optimization>()
            }
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerPath)
    }

    @Test
    fun testRetainNestedGroupWithAttributes() {
        val innerPath = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val innerGroupWithAttributes = Group(listOf(innerPath), mapOf("android:scaleX" to "20"))
        val group = Group(listOf(innerGroupWithAttributes))

        val graphic = object : Graphic {
            override var elements: List<Element> = listOf(group)
            override var attributes = emptyMap<String, String>()
            override val optimizationRegistry = object : OptimizationRegistry {
                override val optimizations = emptyList<Optimization>()
            }
        }

        CollapseGroups().optimize(graphic)

        assertThat(graphic.elements).containsExactly(innerGroupWithAttributes)
    }
}