package com.jzbrooks.vgo.core.transformation

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class CollapseGroupsTests {
    @Test
    fun testCollapseSingleUnnecessaryGroup() {
        val innerPath = createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(innerPath))

        val graphic = createGraphic(listOf(group))

        val groupCollapser = CollapseGroups()
        traverseBottomUp(graphic) {
            if (it is ContainerElement) it.accept(groupCollapser)
        }

        assertThat(graphic::elements).containsExactly(innerPath)
    }

    @Test
    fun testCollapseSingleUnnecessaryNestedGroups() {
        val innerPath = createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group = Group(listOf(Group(listOf(innerPath))))
        val graphic = createGraphic(listOf(group))

        val groupCollapser = CollapseGroups()
        traverseBottomUp(graphic) {
            if (it is ContainerElement) it.accept(groupCollapser)
        }

        assertThat(graphic::elements).containsExactly(innerPath)
    }

    @Test
    fun testRetainNestedGroupWithAttributes() {
        val scale = Matrix3.from(floatArrayOf(20f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        val innerPath = createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val innerGroupWithAttributes = Group(listOf(innerPath), null, mutableMapOf(), scale)
        val group = Group(listOf(innerGroupWithAttributes))

        val graphic = createGraphic(listOf(group))

        val groupCollapser = CollapseGroups()
        traverseBottomUp(graphic) {
            if (it is ContainerElement) it.accept(groupCollapser)
        }

        assertThat(graphic::elements).containsExactly(innerGroupWithAttributes)
    }

    @Test
    fun testAvoidCollapsingGroupsWithClipPaths() {
        val clip = createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(10f, 15f)))))
        val group =
            Group(
                listOf(
                    ClipPath(listOf(clip), null, mutableMapOf()),
                    Group(
                        listOf(ClipPath(listOf(clip), null, mutableMapOf())),
                    ),
                ),
            )

        val graphic = createGraphic(listOf(group))

        val groupCollapser = CollapseGroups()
        traverseBottomUp(graphic) {
            if (it is ContainerElement) it.accept(groupCollapser)
        }

        assertThat(graphic::elements).containsExactly(group)
    }
}
