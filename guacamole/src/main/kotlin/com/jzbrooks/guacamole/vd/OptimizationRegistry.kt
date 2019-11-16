package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.optimization.*
import com.jzbrooks.guacamole.core.optimization.OptimizationRegistry
import com.jzbrooks.guacamole.vd.optimization.BakeTransformations

class OptimizationRegistry : OptimizationRegistry(topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(),
                RemoveRedundantCommands(),
                SimplifyLineCommands(0.00001f)
        )

        private val wholeGraphicOptimizations = listOf(
                CollapseGroups(),
                MergePaths(),
                RemoveEmptyGroups()
        )
    }
}