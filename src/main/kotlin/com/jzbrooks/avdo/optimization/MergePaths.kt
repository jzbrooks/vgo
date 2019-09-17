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
    protected fun merge(paths: List<PathElement>): List<PathElement> {
        val mergedPaths = paths.toMutableList()

        var removedPathElementCount = 0
        for (index in 1 until paths.size) {
            val current = paths[index]
            val previous = paths[index - 1]

            if (current::class != previous::class || current.metadata != previous.metadata) {
                continue
            }

            previous.commands += current.commands
            mergedPaths.removeAt(index - removedPathElementCount)
            removedPathElementCount++
        }

        return mergedPaths
    }
}
