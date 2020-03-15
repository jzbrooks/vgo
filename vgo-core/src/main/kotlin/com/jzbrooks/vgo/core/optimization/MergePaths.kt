package com.jzbrooks.vgo.core.optimization

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.PathElement
import java.util.*

class MergePaths : Optimization {
    override fun optimize(graphic: Graphic) {
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
        val currentChunk = mutableListOf<PathElement>()

        for (item in element.elements) {
            if (item is PathElement) {
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

    private fun merge(paths: List<PathElement>): List<PathElement> {
        if (paths.isEmpty()) return emptyList()

        val mergedPaths = Stack<PathElement>().apply {
            add(paths.first())
        }

        for (item in paths.slice(1 until paths.size)) {
            val previous = mergedPaths.peek()
            if (item::class != previous::class || item.attributes != previous.attributes) {
                mergedPaths.push(item)
            } else {
                previous.commands += item.commands
            }
        }

        return mergedPaths
    }
}
