package com.jzbrooks.avdo.optimization

import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.Group
import com.jzbrooks.avdo.graphic.PathElement

class MergeGroupPaths : MergePaths(), Optimization<Group> {
    override fun visit(element: Group) {
        element.paths = merge(element.paths)
    }
}

class MergeGraphicPaths : MergePaths(), Optimization<Graphic> {
    override fun visit(element: Graphic) {
        // todo: we should apply these transformations in-place and only if the elements are adjacent in the list
        element.elements = merge(element.elements.filterIsInstance<PathElement>())
    }
}

abstract class MergePaths {
    private val mergedPaths = mutableMapOf<Int, PathElement>()
    private val removedPaths = mutableSetOf<PathElement>()

    protected fun merge(paths: List<PathElement>): List<PathElement> {
        for ((index, current) in paths.withIndex()) {
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

        return paths.asSequence()
                .mapIndexed { index, existing -> mergedPaths[index] ?: existing }
                .filter { pathElement -> !removedPaths.contains(pathElement) }
                .toList()
    }
}
