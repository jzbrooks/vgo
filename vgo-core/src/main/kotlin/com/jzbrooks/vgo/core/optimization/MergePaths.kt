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
class MergePaths : Optimization {
    override fun visit(graphic: Graphic) {
        topDownVisit(graphic)
    }

    override fun visit(clipPath: ClipPath) {}
    override fun visit(group: Group) {}
    override fun visit(extra: Extra) {}
    override fun visit(path: Path) {}

    private fun topDownVisit(element: Element): Element {
        return if (element is ContainerElement) {
            for (child in element.elements) {
                topDownVisit(child)
            }
            merge(element)
        } else {
            element
        }
    }

    private fun merge(element: ContainerElement): Element {
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

        return element.apply { this.elements = elements }
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
