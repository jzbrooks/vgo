package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import java.util.Stack

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

        val mergedPaths = Stack<Path>()
        mergedPaths.add(paths.first())

        for (item in paths.drop(1)) {
            val previous = mergedPaths.peek()

            if (!item.hasSameAttributes(previous)) {
                mergedPaths.push(item)
            } else {
                previous.commands += item.commands
            }
        }

        return mergedPaths
    }
}
