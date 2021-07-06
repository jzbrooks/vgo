package com.jzbrooks.vgo.core.optimization

import assertk.assertThat
import assertk.assertions.hasSize
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import com.jzbrooks.vgo.core.util.element.traverseBottomUp
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import org.junit.jupiter.api.Test

class RemoveEmptyGroupsTests {
    @Test
    fun testCollapseNestedEmptyGroup() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(listOf()))))))

        val graphic = createGraphic(nestedEmptyGroups)

        val groupRemover = RemoveEmptyGroups()

        traverseBottomUp(graphic) {
            it.accept(groupRemover)
        }

        assertThat(graphic::elements).hasSize(0)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithPath() {
        val nestedEmptyGroups = listOf(
            Group(
                listOf(
                    Group(
                        listOf(
                            Group(
                                listOf(
                                    createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 2f)))))
                                )
                            )
                        )
                    )
                )
            )
        )

        val graphic = createGraphic(nestedEmptyGroups)

        val groupRemover = RemoveEmptyGroups()

        traverseBottomUp(graphic) {
            it.accept(groupRemover)
        }

        assertThat(graphic::elements).hasSize(1)
    }

    @Test
    fun testAvoidCollapsingNestedGroupWithAttributes() {
        val nestedEmptyGroups = listOf(Group(listOf(Group(listOf(Group(emptyList(), "base", mutableMapOf(), Matrix3.IDENTITY))))))

        val graphic = createGraphic(nestedEmptyGroups)

        val groupRemover = RemoveEmptyGroups()

        traverseBottomUp(graphic) {
            it.accept(groupRemover)
        }

        assertThat(graphic::elements).hasSize(1)
    }

    @Test
    fun testCollapseEmptyGroupAndAvoidAdjacentElements() {
        val nestedEmptyGroups = listOf(
            Group(emptyList()),
            createPath(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(4f, 2f))))),
        )

        val graphic = createGraphic(nestedEmptyGroups)

        val groupRemover = RemoveEmptyGroups()

        traverseBottomUp(graphic) {
            it.accept(groupRemover)
        }

        assertThat(graphic.elements.filterIsInstance<Path>()).hasSize(1)
    }
}
