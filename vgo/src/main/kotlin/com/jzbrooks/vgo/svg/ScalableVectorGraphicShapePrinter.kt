package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.ShapePrinter
import com.jzbrooks.vgo.core.util.math.Point

class ScalableVectorGraphicShapePrinter(
    private val commandPrinter: ScalableVectorGraphicCommandPrinter,
) : ShapePrinter {
    override fun print(path: Path): String =
        buildString {
            append("<path d=\"")
            for (command in path.commands) append(commandPrinter.print(command))
            append("\"/>")
        }

    override fun print(shape: Shape): String =
        when (shape) {
            is Circle -> {
                "<circle cx=\"${print(shape.cx)}\" cy=\"${print(shape.cy)}\" r=\"${print(shape.r)}\"/>"
            }

            is Ellipse -> {
                "<ellipse cx=\"${print(shape.cx)}\" cy=\"${print(shape.cy)}\" " +
                    "rx=\"${print(shape.rx)}\" ry=\"${print(shape.ry)}\"/>"
            }

            is Rect -> {
                buildString {
                    append("<rect x=\"${print(shape.x)}\" y=\"${print(shape.y)}\" ")
                    append("width=\"${print(shape.width)}\" height=\"${print(shape.height)}\"")
                    if (shape.rx > 0f) append(" rx=\"${print(shape.rx)}\"")
                    if (shape.ry > 0f) append(" ry=\"${print(shape.ry)}\"")
                    append("/>")
                }
            }

            is Line -> {
                "<line x1=\"${print(shape.x1)}\" y1=\"${print(shape.y1)}\" " +
                    "x2=\"${print(shape.x2)}\" y2=\"${print(shape.y2)}\"/>"
            }

            is Polyline -> {
                "<polyline points=\"${print(shape.points)}\"/>"
            }

            is Polygon -> {
                "<polygon points=\"${print(shape.points)}\"/>"
            }
        }

    private fun print(value: Float) = commandPrinter.formatter.format(value)

    private fun print(points: List<Point>) = points.joinToString(" ") { "${print(it.x)},${print(it.y)}" }
}
