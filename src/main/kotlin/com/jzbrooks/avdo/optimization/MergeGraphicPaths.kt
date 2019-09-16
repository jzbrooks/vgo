package com.jzbrooks.avdo.optimization

import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.PathElement

class MergeGraphicPaths : Optimization<Graphic> {
    private val mergedPaths = mutableMapOf<Int, PathElement>()
    private val removedPaths = mutableSetOf<PathElement>()

    override fun visit(element: Graphic) {
        for ((index, current) in element.elements.withIndex()) {
            if (current !is PathElement) continue

            if (mergedPaths.isEmpty()) {
                mergedPaths[index] = current
                continue
            }

            val previous = mergedPaths.values.last()
            if (current::class != previous::class || current.metadata != previous.metadata) {
                mergedPaths[index] = current
                continue
            }

            previous.commands += current.commands
            removedPaths.add(current)
        }

        element.elements = element.elements.asSequence()
                .mapIndexed { index, existing -> mergedPaths[index] ?: existing }
                .filter { pathElement -> !removedPaths.contains(pathElement) }
                .toList()
    }
}