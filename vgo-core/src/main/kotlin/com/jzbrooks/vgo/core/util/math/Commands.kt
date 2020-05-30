package com.jzbrooks.vgo.core.util.math

import com.jzbrooks.vgo.core.graphic.command.*
import java.util.*

/**
 * Computes absolute coordinates for a given command in the sequence.
 * By default, the absolute coordinate of the last command is returned.
 * @param commands: The complete list of **relative** commands for a given path. The initial moveto can be absolute.
 * @param commandIndex: The index of a specific command to compute absolute coordinates from relative
 */
fun computeAbsoluteCoordinates(commands: List<Command>): Point {
    assert(commands.drop(1).filterIsInstance<ParameterizedCommand<*>>().all { it.variant == CommandVariant.RELATIVE })

    val pathStart = Stack<Point>()
    var currentPoint = Point(0f, 0f)

    for (i in commands.indices) {
        val command = commands[i]
        val newCurrentPoint = when (command) {
            is MoveTo -> command.parameters[0]
            is LineTo -> command.parameters[0]
            is HorizontalLineTo -> Point(command.parameters[0], 0f)
            is VerticalLineTo -> Point(0f, command.parameters[0])
            is CubicBezierCurve -> command.parameters[0].end
            is SmoothCubicBezierCurve -> command.parameters[0].end
            is QuadraticBezierCurve -> command.parameters[0].end
            is SmoothQuadraticBezierCurve -> command.parameters[0]
            is EllipticalArcCurve -> command.parameters[0].end
            is ClosePath -> pathStart.pop()
            else -> throw IllegalStateException("Unsupported command encountered: $command")
        }

        if (command !is ClosePath) {
            currentPoint += newCurrentPoint
        } else {
            currentPoint = newCurrentPoint
        }

        if (command is MoveTo) {
            pathStart.push(currentPoint.copy())
        }
    }

    return currentPoint
}