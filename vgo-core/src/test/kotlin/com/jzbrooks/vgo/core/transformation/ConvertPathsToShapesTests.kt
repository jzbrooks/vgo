package com.jzbrooks.vgo.core.transformation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.element.createGraphic
import com.jzbrooks.vgo.core.util.element.createPath
import org.junit.jupiter.api.Test

class ConvertPathsToShapesTests {
    @Test
    fun `converts merged circular subpaths to circles`() {
        val path =
            createPath(
                CommandString("M5,10a5,5,0,1,1,10,0a5,5,0,1,1-10,0M18,20a2,2,0,1,1,4,0a2,2,0,1,1-4,0")
                    .toCommandList(),
            )
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes().visit(graphic)

        assertThat(graphic.elements).hasSize(2)
        assertThat(graphic.elements).containsExactly(
            Circle(
                null,
                mutableMapOf(),
                10f,
                10f,
                5f,
                path.fill,
                path.fillRule,
                path.stroke,
                path.strokeWidth,
                path.strokeLineCap,
                path.strokeLineJoin,
                path.strokeMiterLimit,
            ),
            Circle(
                null,
                mutableMapOf(),
                20f,
                20f,
                2f,
                path.fill,
                path.fillRule,
                path.stroke,
                path.strokeWidth,
                path.strokeLineCap,
                path.strokeLineJoin,
                path.strokeMiterLimit,
            ),
        )
    }

    @Test
    fun `leaves mixed paths unchanged`() {
        val path = createPath(CommandString("M0,0L10,10").toCommandList())
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes().visit(graphic)

        assertThat(graphic.elements.single()).isInstanceOf<Path>().isEqualTo(path)
    }
}
