package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
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
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Surveyor
import com.jzbrooks.vgo.core.util.math.intersects

/**
 * Merges multiple paths into a single path where possible
 */
class MergePaths(
    private val constraints: Constraints = Constraints.None,
) : BottomUpTransformer {
    private val surveyor = Surveyor()

    override fun visit(graphic: Graphic) = merge(graphic)

    override fun visit(group: Group) = merge(group)

    override fun visit(clipPath: ClipPath) = merge(clipPath)

    override fun visit(extra: Extra) {}

    override fun visit(path: Path) {}

    private fun merge(element: ContainerElement) {
        // merge consecutive path elements of the same type
        val elements = mutableListOf<Element>()
        val currentChunk = mutableListOf<Path>()

        for (item in element.elements) {
            if (item is Path) {
                currentChunk.add(item)
            } else {
                // merge previous (back to chunk start)
                if (currentChunk.isNotEmpty()) elements.addAll(merge(currentChunk))

                // add the current item
                elements.add(item)

                currentChunk.clear()
            }
        }

        if (currentChunk.isNotEmpty()) {
            elements.addAll(merge(currentChunk))
        }

        element.elements = elements
    }

    private fun merge(paths: List<Path>): List<Path> =
        when (constraints) {
            is Constraints.PathLength -> mergeConstrained(paths, constraints)
            Constraints.None -> mergeUnconstrained(paths)
        }

    private fun mergeUnconstrained(paths: List<Path>): List<Path> {
        if (paths.isEmpty()) return emptyList()

        val mergedPaths = ArrayList<Path>(paths.size)
        mergedPaths.add(paths.first())

        for (current in paths.drop(1)) {
            val previous = mergedPaths.last()

            if (unableToMerge(previous, current)) {
                mergedPaths.add(current)
            } else {
                previous.commands += makeFirstCommandAbsolute(current.commands)
            }
        }

        return mergedPaths
    }

    private fun mergeConstrained(
        paths: List<Path>,
        constraints: Constraints.PathLength,
    ): List<Path> {
        if (paths.isEmpty()) return emptyList()

        val mergedPaths = ArrayList<Path>(paths.size)
        mergedPaths.add(paths.first())

        var pathLength =
            paths
                .first()
                .commands
                .joinToString("", transform = constraints.commandPrinter::print)
                .length

        for (current in paths.drop(1)) {
            val previous = mergedPaths.last()

            val mergeableCommands = makeFirstCommandAbsolute(current.commands)
            val currentLength = mergeableCommands.joinToString("", transform = constraints.commandPrinter::print).length
            val accumulatedLength = pathLength + currentLength

            if (accumulatedLength > constraints.maxLength || unableToMerge(previous, current)) {
                mergedPaths.add(current)
                pathLength = currentLength
            } else {
                previous.commands += mergeableCommands
                pathLength = accumulatedLength
            }
        }

        return mergedPaths
    }

    // Paths must have the same visual parameters to be merged
    // Intersecting paths can cause problems with path fill rules and with transparency
    // If constraints exist on a path, they must be updated
    private fun unableToMerge(
        previous: Path,
        current: Path,
    ): Boolean {
        if (!haveSameAttributes(current, previous)) {
            return true
        }

        val previousBounds = surveyor.findBoundingBox(previous.commands)
        val currentBounds = surveyor.findBoundingBox(current.commands)

        // Cheap bounding box check: if boxes don't intersect, paths definitely don't overlap
        if (!(previousBounds intersects currentBounds)) {
            return false
        }

        // Bounding boxes intersect, use GJK on convex hulls for precise check
        val previousHull = surveyor.sampleConvexHull(previous.commands)
        val currentHull = surveyor.sampleConvexHull(current.commands)

        return intersects(previousHull, currentHull)
    }

    private fun haveSameAttributes(
        first: Path,
        second: Path,
    ): Boolean =
        first.id == second.id &&
            first.foreign == second.foreign &&
            first.fill == second.fill &&
            first.fillRule == second.fillRule &&
            first.stroke == second.stroke &&
            first.strokeWidth == second.strokeWidth &&
            first.strokeLineCap == second.strokeLineCap &&
            first.strokeLineJoin == second.strokeLineJoin &&
            first.strokeMiterLimit == second.strokeMiterLimit

    private fun makeFirstCommandAbsolute(commands: List<Command>): List<Command> {
        val firstCommand = commands.firstOrNull() as? ParameterizedCommand<*> ?: return commands

        if (firstCommand.variant == CommandVariant.RELATIVE) {
            var currentPoint = Point.ZERO

            when (firstCommand) {
                is MoveTo, is LineTo, is SmoothQuadraticBezierCurve -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { point ->
                            (point + currentPoint).also { point -> currentPoint = point }
                        }
                }

                is HorizontalLineTo -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { x ->
                            (x + currentPoint.x).also { x -> currentPoint = currentPoint.copy(x = x) }
                        }
                }

                is VerticalLineTo -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { x ->
                            (x + currentPoint.x).also { x -> currentPoint = currentPoint.copy(x = x) }
                        }
                }

                is CubicBezierCurve -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { parameter ->
                            val newEnd = parameter.end + currentPoint
                            parameter
                                .copy(
                                    startControl = parameter.startControl + currentPoint,
                                    endControl = parameter.endControl + currentPoint,
                                    end = newEnd,
                                ).also { currentPoint = newEnd }
                        }
                }

                is SmoothCubicBezierCurve -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { parameter ->
                            val newEnd = parameter.end + currentPoint
                            parameter
                                .copy(
                                    endControl = parameter.endControl + currentPoint,
                                    end = newEnd,
                                ).also { currentPoint = newEnd }
                        }
                }

                is QuadraticBezierCurve -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { parameter ->
                            val newEnd = parameter.end + currentPoint
                            parameter
                                .copy(
                                    control = parameter.control + currentPoint,
                                    end = newEnd,
                                ).also { currentPoint = newEnd }
                        }
                }

                is EllipticalArcCurve -> {
                    firstCommand.parameters =
                        firstCommand.parameters.map { parameter ->
                            val newEnd = parameter.end + currentPoint
                            parameter
                                .copy(
                                    end = newEnd,
                                ).also { currentPoint = newEnd }
                        }
                }
            }

            firstCommand.variant = CommandVariant.ABSOLUTE
        }

        return commands
    }

    sealed interface Constraints {
        /** Constraints the optimization by preventing merging paths beyond a given maximum length */
        data class PathLength(
            val commandPrinter: CommandPrinter,
            /** The maximum length of a single path */
            val maxLength: Int,
        ) : Constraints

        data object None : Constraints
    }
}
