package com.jzbrooks.guacamole.svg

import com.jzbrooks.guacamole.core.graphic.command.CommandPrinter
import com.jzbrooks.guacamole.core.optimization.*
import com.jzbrooks.guacamole.core.optimization.OptimizationRegistry
import com.jzbrooks.guacamole.svg.optimization.BakeTransformations

class SvgOptimizationRegistry : OptimizationRegistry(topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandPrinter(3)),
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