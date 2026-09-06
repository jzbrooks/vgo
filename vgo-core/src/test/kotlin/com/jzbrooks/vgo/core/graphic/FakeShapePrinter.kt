package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.graphic.command.FakeCommandPrinter
import com.jzbrooks.vgo.core.util.math.Point
import java.math.RoundingMode
import java.text.DecimalFormat

class FakeShapePrinter : ShapePrinter {
    private val commandPrinter = FakeCommandPrinter()

    private val formatter =
        DecimalFormat().apply {
            maximumFractionDigits = 2
            isDecimalSeparatorAlwaysShown = false
            roundingMode = RoundingMode.HALF_UP
        }

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
                "<rect x=\"${print(shape.x)}\" y=\"${print(shape.y)}\" " +
                    "width=\"${print(shape.width)}\" height=\"${print(shape.height)}\"/>"
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

    private fun print(value: Float) = formatter.format(value)

    private fun print(points: List<Point>) = points.joinToString(" ") { "${print(it.x)},${print(it.y)}" }
}
