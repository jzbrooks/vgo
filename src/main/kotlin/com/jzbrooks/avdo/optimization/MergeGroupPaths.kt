package com.jzbrooks.avdo.optimization

import com.jzbrooks.avdo.graphic.Group
import com.jzbrooks.avdo.graphic.PathElement

class MergeGroupPaths : Optimization<Group> {
    private val mergedPaths = mutableMapOf<Int, PathElement>()
    private val removedPaths = mutableSetOf<PathElement>()

    override fun visit(element: Group) {
        for ((index, current) in element.paths.withIndex()) {
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

        element.paths = element.paths.asSequence()
                .mapIndexed { index, existing -> mergedPaths[index] ?: existing }
                .filter { pathElement -> !removedPaths.contains(pathElement) }
                .toList()
    }
}