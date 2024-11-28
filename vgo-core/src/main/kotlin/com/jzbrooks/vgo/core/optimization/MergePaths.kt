package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandPrinter
import com.jzbrooks.vgo.core.util.math.Surveyor
import com.jzbrooks.vgo.core.util.math.intersects

/**
 * Merges multiple paths into a single path where possible
 */
class MergePaths(
    private val constraints: Constraints,
) : BottomUpOptimization {
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
                previous.commands += current.commands
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

            val currentLength = current.commands.joinToString("", transform = constraints.commandPrinter::print).length
            val accumulatedLength = pathLength + currentLength

            if (accumulatedLength >= constraints.maxLength || unableToMerge(previous, current)) {
                mergedPaths.add(current)
                pathLength = currentLength
            } else {
                previous.commands += current.commands
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
    ): Boolean =
        !haveSameAttributes(current, previous) ||
            surveyor.findBoundingBox(previous.commands) intersects surveyor.findBoundingBox(current.commands)

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

    sealed interface Constraints {
        data class PathLength(
            val commandPrinter: CommandPrinter,
            val maxLength: Int,
        ) : Constraints

        data object None : Constraints
    }
}
