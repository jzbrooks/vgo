package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path

/**
 * Merges multiple paths into a single path where possible
 */
class MergePaths : BottomUpOptimization {
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

    private fun merge(paths: List<Path>): List<Path> {
        if (paths.isEmpty()) return emptyList()

        val mergedPaths = ArrayList<Path>(paths.size)
        mergedPaths.add(paths.first())

        for (current in paths.drop(1)) {
            val previous = mergedPaths.last()

            // Avoid merging paths with even odd fill rule, because
            // the merge might cause some paths to be considered 'interior'
            // according to those rules when they were previously exterior
            // in their own paths.
            //
            // There might be a reasonable way to deduce that situation more
            // specifically, which could enable merging of some even odd paths.
            if (!haveSameAttributes(current, previous) || current.fillRule == Path.FillRule.EVEN_ODD) {
                mergedPaths.add(current)
            } else {
                previous.commands += current.commands
            }
        }

        return mergedPaths
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
}
