package com.jzbrooks.vgo.core.transformation

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import assertk.assertions.single
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Rect
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

        assertThat(graphic::elements).containsExactly(
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
        val path = createPath(CommandString("M0,0C2,3,4,5,10,10").toCommandList())
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes().visit(graphic)

        assertThat(graphic::elements).single().isInstanceOf<Path>().isEqualTo(path)
    }

    @Test
    fun `converts merged rectangular subpaths to rectangles`() {
        val path = createPath(CommandString("M10,20h30v40h-30ZM2,3H8V12H2Z").toCommandList())
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes().visit(graphic)

        assertThat(graphic::elements).hasSize(2)
        assertThat(graphic::elements).index(0).isInstanceOf<Rect>().all {
            prop(Rect::x).isEqualTo(10f)
            prop(Rect::y).isEqualTo(20f)
            prop(Rect::width).isEqualTo(30f)
            prop(Rect::height).isEqualTo(40f)
        }
        assertThat(graphic::elements).index(1).isInstanceOf<Rect>().all {
            prop(Rect::x).isEqualTo(2f)
            prop(Rect::y).isEqualTo(3f)
            prop(Rect::width).isEqualTo(6f)
            prop(Rect::height).isEqualTo(9f)
        }
    }

    @Test
    fun `converts ellipse and line subpaths`() {
        val path = createPath(CommandString("M5,10a5,3,0,1,1,10,0a5,3,0,1,1-10,0M2,3L8,12").toCommandList())
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes().visit(graphic)

        assertThat(graphic::elements).hasSize(2)
        assertThat(graphic::elements).index(0).isInstanceOf<Ellipse>().all {
            prop(Ellipse::cx).isEqualTo(10f)
            prop(Ellipse::cy).isEqualTo(10f)
            prop(Ellipse::rx).isEqualTo(5f)
            prop(Ellipse::ry).isEqualTo(3f)
        }
        assertThat(graphic::elements).index(1).isInstanceOf<Line>().all {
            prop(Line::x1).isEqualTo(2f)
            prop(Line::y1).isEqualTo(3f)
            prop(Line::x2).isEqualTo(8f)
            prop(Line::y2).isEqualTo(12f)
        }
    }

    @Test
    fun `keeps path when recovered shapes are rejected`() {
        val path = createPath(CommandString("M0,0h24v24h-24Z").toCommandList())
        val graphic = createGraphic(listOf(path))

        ConvertPathsToShapes(shouldConvert = { _, _ -> false }).visit(graphic)

        assertThat(graphic.elements.single()).isEqualTo(path)
    }
}
