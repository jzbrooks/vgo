package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.ParameterizedCommand
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo

/**
 * Computes absolute coordinates for a given command in the sequence.
 * By default, the absolute coordinate of the last command is returned.
 * @param commands The complete list of **relative** commands for a given path. The initial moveto can be absolute.
 */
fun computeAbsoluteCoordinates(commands: List<Command>): Point {
    assert(commands.drop(1).filterIsInstance<ParameterizedCommand<*>>().all { it.variant == CommandVariant.RELATIVE })

    var currentPoint = Point(0f, 0f)
    val pathStart = ArrayDeque<Point>()

    for (i in commands.indices) {
        val command = commands[i]
        val previousCommand = commands.getOrNull(i - 1)

        if (previousCommand is ClosePath && command !is MoveTo) {
            pathStart.addFirst(currentPoint.copy())
        }

        val newCurrentPoint =
            when (command) {
                is MoveTo -> command.parameters[0]
                is LineTo -> command.parameters[0]
                is HorizontalLineTo -> Point(command.parameters[0], 0f)
                is VerticalLineTo -> Point(0f, command.parameters[0])
                is CubicBezierCurve -> command.parameters[0].end
                is SmoothCubicBezierCurve -> command.parameters[0].end
                is QuadraticBezierCurve -> command.parameters[0].end
                is SmoothQuadraticBezierCurve -> command.parameters[0]
                is EllipticalArcCurve -> command.parameters[0].end
                is ClosePath -> pathStart.removeFirst()
            }

        if (command !is ClosePath) {
            currentPoint += newCurrentPoint
        } else {
            currentPoint = newCurrentPoint
        }

        if (i == 0 || (previousCommand is ClosePath && command is MoveTo)) {
            pathStart.addFirst(currentPoint.copy())
        }
    }

    return currentPoint
}
