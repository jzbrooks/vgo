package com.jzbrooks.guacamole.optimization

import com.jzbrooks.guacamole.graphic.ContainerElement
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Graphic
import com.jzbrooks.guacamole.graphic.PathElement

class MergePaths : Optimization {
    override fun visit(graphic: Graphic) {
        topDownVisit(graphic)
    }

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
        var currentChunk = mutableListOf<PathElement>()

        for (item in element.elements) {
            if (item is PathElement) {
                currentChunk.add(item)
            } else {
                // merge previous (back to chunk start)
                if (currentChunk.isNotEmpty()) elements.addAll(merge(currentChunk))

                // add the current item
                elements.add(item)

                currentChunk = mutableListOf()
            }
        }

        if (currentChunk.isNotEmpty()) {
            elements.addAll(merge(currentChunk))
        }

        return element.apply { this.elements = elements }
    }

    private fun merge(paths: List<PathElement>): List<PathElement> {
        val mergedPaths = paths.toMutableList()

        var removedPathElementCount = 0
        for (index in 1 until paths.size) {
            val current = paths[index]
            val previous = paths[index - 1]

            if (current::class != previous::class || current.attributes != previous.attributes) {
                continue
            }

            previous.commands += current.commands
            mergedPaths.removeAt(index - removedPathElementCount)
            removedPathElementCount++
        }

        return mergedPaths
    }
}
