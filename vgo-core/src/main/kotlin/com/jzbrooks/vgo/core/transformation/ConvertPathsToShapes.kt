package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

class ConvertPathsToShapes(
    val commandPrinter: CommandPrinter,
) : BottomUpTransformer {
    // This is temporary and should be removed in the next round of breaking changes.
    // The real fix for this is that `CommandPrinter` exposes its formatter on the interface.
    // So that core transformations don't have to know about the different formats.
    // This is safe for now because it is only used for SVGs.
    @Deprecated("Upstream this into CommandPrinter")
    private val formatter =
        DecimalFormat().apply {
            maximumFractionDigits = 3
            isDecimalSeparatorAlwaysShown = false
            isGroupingUsed = false
            roundingMode = RoundingMode.HALF_UP
            minimumIntegerDigits = 0
            decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        }

    override fun visit(graphic: Graphic) = convertPaths(graphic)

    override fun visit(group: Group) = convertPaths(group)

    override fun visit(extra: Extra) {}

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun convertPaths(container: ContainerElement) {
        container.elements =
            container.elements.flatMap { element ->
                if (element is Path) shapesOrNull(element) ?: listOf(element) else listOf(element)
            }
    }

    private fun shapesOrNull(path: Path): List<Shape>? {
        if (path.id != null || path.foreign.isNotEmpty()) return null

        val shapes = mutableListOf<Shape>()
        var index = 0
        var current = Point.ZERO
        while (index < path.commands.size) {
            val move = path.commands.getOrNull(index) as? MoveTo ?: return null
            if (move.parameters.size != 1) return null
            val start =
                when (move.variant) {
                    CommandVariant.ABSOLUTE -> move.parameters.single()
                    CommandVariant.RELATIVE -> current + move.parameters.single()
                }
            val match =
                matchEllipse(path, index, start)
                    ?: matchRect(path, index, start)
                    ?: matchLine(path, index, start)
                    ?: return null
            shapes += match.shape
            current = match.end
            index = match.nextIndex
        }

        return shapes.takeIf { it.isNotEmpty() && shapesAreShorterThanPath(path, it) }
    }

    private fun matchEllipse(
        path: Path,
        index: Int,
        start: Point,
    ): Match? {
        val first = path.commands.getOrNull(index + 1) as? EllipticalArcCurve ?: return null
        val second = path.commands.getOrNull(index + 2) as? EllipticalArcCurve ?: return null
        if (first.parameters.size != 1 || second.parameters.size != 1) return null

        val firstParameter = first.parameters.single()
        val secondParameter = second.parameters.single()
        if (!firstParameter.isEllipseHalf() || !secondParameter.isEllipseHalf()) return null
        val opposite = firstParameter.endpointFrom(start, first.variant)
        val end = secondParameter.endpointFrom(opposite, second.variant)
        val radiusX = firstParameter.radiusX
        val radiusY = firstParameter.radiusY
        if (!close(secondParameter.radiusX, radiusX) ||
            !close(secondParameter.radiusY, radiusY) ||
            !close(opposite.x - start.x, radiusX * 2f) ||
            !close(opposite.y, start.y) ||
            !close(end.x, start.x) ||
            !close(end.y, start.y)
        ) {
            return null
        }

        val shape =
            if (close(radiusX, radiusY)) {
                Circle(
                    null,
                    mutableMapOf(),
                    start.x + radiusX,
                    start.y,
                    radiusX,
                    path.fill,
                    path.fillRule,
                    path.stroke,
                    path.strokeWidth,
                    path.strokeLineCap,
                    path.strokeLineJoin,
                    path.strokeMiterLimit,
                )
            } else {
                Ellipse(
                    null,
                    mutableMapOf(),
                    start.x + radiusX,
                    start.y,
                    radiusX,
                    radiusY,
                    path.fill,
                    path.fillRule,
                    path.stroke,
                    path.strokeWidth,
                    path.strokeLineCap,
                    path.strokeLineJoin,
                    path.strokeMiterLimit,
                )
            }
        val nextIndex = index + if (path.commands.getOrNull(index + 3) is ClosePath) 4 else 3
        return Match(shape, nextIndex, end)
    }

    private fun matchRect(
        path: Path,
        index: Int,
        start: Point,
    ): Match? {
        val horizontalOut = path.commands.getOrNull(index + 1) as? HorizontalLineTo ?: return null
        val vertical = path.commands.getOrNull(index + 2) as? VerticalLineTo ?: return null
        val horizontalBack = path.commands.getOrNull(index + 3) as? HorizontalLineTo ?: return null
        if (path.commands.getOrNull(index + 4) !is ClosePath ||
            horizontalOut.parameters.size != 1 ||
            vertical.parameters.size != 1 ||
            horizontalBack.parameters.size != 1
        ) {
            return null
        }

        val oppositeX = horizontalOut.endpointFrom(start.x)
        val oppositeY = vertical.endpointFrom(start.y)
        val endX = horizontalBack.endpointFrom(oppositeX)
        if (close(oppositeX, start.x) || close(oppositeY, start.y) || !close(endX, start.x)) return null

        val rect =
            Rect(
                id = null,
                foreign = mutableMapOf(),
                x = minOf(start.x, oppositeX),
                y = minOf(start.y, oppositeY),
                width = abs(oppositeX - start.x),
                height = abs(oppositeY - start.y),
                rx = 0f,
                ry = 0f,
                fill = path.fill,
                fillRule = path.fillRule,
                stroke = path.stroke,
                strokeWidth = path.strokeWidth,
                strokeLineCap = path.strokeLineCap,
                strokeLineJoin = path.strokeLineJoin,
                strokeMiterLimit = path.strokeMiterLimit,
            )
        return Match(rect, index + 5, start)
    }

    private fun matchLine(
        path: Path,
        index: Int,
        start: Point,
    ): Match? {
        val command = path.commands.getOrNull(index + 1) ?: return null
        val end =
            when (command) {
                is LineTo -> {
                    if (command.parameters.size != 1) return null
                    if (command.variant == CommandVariant.ABSOLUTE) command.parameters.single() else start + command.parameters.single()
                }

                is HorizontalLineTo -> {
                    if (command.parameters.size != 1) return null
                    start.copy(x = command.endpointFrom(start.x))
                }

                is VerticalLineTo -> {
                    if (command.parameters.size != 1) return null
                    start.copy(y = command.endpointFrom(start.y))
                }

                else -> {
                    return null
                }
            }
        val nextIndex = index + 2
        if (nextIndex < path.commands.size && path.commands[nextIndex] !is MoveTo) return null

        val line =
            Line(
                null,
                mutableMapOf(),
                start.x,
                start.y,
                end.x,
                end.y,
                path.fill,
                path.fillRule,
                path.stroke,
                path.strokeWidth,
                path.strokeLineCap,
                path.strokeLineJoin,
                path.strokeMiterLimit,
            )
        return Match(line, nextIndex, end)
    }

    private data class Match(
        val shape: Shape,
        val nextIndex: Int,
        val end: Point,
    )

    private fun EllipticalArcCurve.Parameter.isEllipseHalf(): Boolean =
        close(angle, 0f) &&
            arc == EllipticalArcCurve.ArcFlag.LARGE &&
            sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE

    private fun EllipticalArcCurve.Parameter.endpointFrom(
        start: Point,
        variant: CommandVariant,
    ): Point = if (variant == CommandVariant.ABSOLUTE) end else start + end

    private fun HorizontalLineTo.endpointFrom(start: Float): Float =
        if (variant == CommandVariant.ABSOLUTE) parameters.single() else start + parameters.single()

    private fun VerticalLineTo.endpointFrom(start: Float): Float =
        if (variant == CommandVariant.ABSOLUTE) parameters.single() else start + parameters.single()

    private fun close(
        first: Float,
        second: Float,
    ): Boolean = abs(first - second) <= 1e-3

    @Suppress("DEPRECATION")
    private fun shapesAreShorterThanPath(
        path: Path,
        shapes: List<Shape>,
    ): Boolean {
        if (shapes.all { it is Circle }) return true

        val pathLength = path.commands.sumOf { commandPrinter.print(it).length } + 12
        val shapeLength =
            shapes.sumOf { shape ->
                when (shape) {
                    is Circle -> {
                        "<circle cx=\"${formatter.format(
                            shape.cx,
                        )}\" cy=\"${formatter.format(shape.cy)}\" r=\"${formatter.format(shape.r)}\"/>".length
                    }

                    is Ellipse -> {
                        "<ellipse cx=\"${formatter.format(
                            shape.cx,
                        )}\" cy=\"${formatter.format(
                            shape.cy,
                        )}\" rx=\"${formatter.format(shape.rx)}\" ry=\"${formatter.format(shape.ry)}\"/>".length
                    }

                    is Rect -> {
                        "<rect height=\"${formatter.format(
                            shape.height,
                        )}\" width=\"${formatter.format(
                            shape.width,
                        )}\" x=\"${formatter.format(shape.x)}\" y=\"${formatter.format(shape.y)}\"/>".length
                    }

                    is Line -> {
                        "<line x1=\"${formatter.format(
                            shape.x1,
                        )}\" x2=\"${formatter.format(
                            shape.x2,
                        )}\" y1=\"${formatter.format(shape.y1)}\" y2=\"${formatter.format(shape.y2)}\"/>".length
                    }

                    else -> {
                        Int.MAX_VALUE
                    }
                }
            }
        return shapeLength < pathLength
    }
}
