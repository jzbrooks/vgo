package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Point
import kotlin.math.abs

/** Recovers native shapes from paths after path-only optimizations have run. */
class ConvertPathsToShapes(
    private val tolerance: Float = 1e-3f,
) : BottomUpTransformer {
    override fun visit(graphic: Graphic) = convertPaths(graphic)

    override fun visit(group: Group) = convertPaths(group)

    override fun visit(extra: Extra) {}

    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    private fun convertPaths(container: ContainerElement) {
        container.elements =
            container.elements.flatMap { element ->
                if (element is Path) circlesOrNull(element) ?: listOf(element) else listOf(element)
            }
    }

    private fun circlesOrNull(path: Path): List<Circle>? {
        if (path.id != null || path.foreign.isNotEmpty()) return null

        val circles = mutableListOf<Circle>()
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
            val first = path.commands.getOrNull(index + 1) as? EllipticalArcCurve ?: return null
            val second = path.commands.getOrNull(index + 2) as? EllipticalArcCurve ?: return null
            if (first.parameters.size != 1 || second.parameters.size != 1) return null

            val firstParameter = first.parameters.single()
            val secondParameter = second.parameters.single()
            if (!firstParameter.isCircleHalf() || !secondParameter.isCircleHalf()) return null
            val opposite = firstParameter.endpointFrom(start, first.variant)
            val end = secondParameter.endpointFrom(opposite, second.variant)
            val radius = firstParameter.radiusX
            if (!close(firstParameter.radiusY, radius) ||
                !close(secondParameter.radiusX, radius) ||
                !close(secondParameter.radiusY, radius) ||
                !close(opposite.x - start.x, radius * 2f) ||
                !close(opposite.y, start.y) ||
                !close(end.x, start.x) ||
                !close(end.y, start.y)
            ) {
                return null
            }

            circles +=
                Circle(
                    id = null,
                    foreign = mutableMapOf(),
                    cx = start.x + radius,
                    cy = start.y,
                    r = radius,
                    fill = path.fill,
                    fillRule = path.fillRule,
                    stroke = path.stroke,
                    strokeWidth = path.strokeWidth,
                    strokeLineCap = path.strokeLineCap,
                    strokeLineJoin = path.strokeLineJoin,
                    strokeMiterLimit = path.strokeMiterLimit,
                )
            current = end
            index += if (path.commands.getOrNull(index + 3) is ClosePath) 4 else 3
        }

        return circles.takeIf { it.isNotEmpty() }
    }

    private fun EllipticalArcCurve.Parameter.isCircleHalf(): Boolean =
        close(angle, 0f) &&
            arc == EllipticalArcCurve.ArcFlag.LARGE &&
            sweep == EllipticalArcCurve.SweepFlag.CLOCKWISE

    private fun EllipticalArcCurve.Parameter.endpointFrom(
        start: Point,
        variant: CommandVariant,
    ): Point = if (variant == CommandVariant.ABSOLUTE) end else start + end

    private fun close(
        first: Float,
        second: Float,
    ): Boolean = abs(first - second) <= tolerance
}
