package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.optimization.*
import com.jzbrooks.vgo.svg.optimization.BakeTransformations

class SvgOptimizationRegistry : OptimizationRegistry(emptyList(), topDownOptimizations, emptyList(), wholeGraphicOptimizations) {

    companion object {
        private val topDownOptimizations: List<TopDownOptimization> = listOf(
                BakeTransformations(),
                BreakoutImplicitCommands(),
                CommandVariant(CommandVariant.Mode.Relative),
                SimplifyLineCommands(1e-3f),
                SimplifyBezierCurveCommands(1e-3f),
                RemoveRedundantCommands(),
                CommandVariant(CommandVariant.Mode.Compact(ScalableVectorGraphicCommandPrinter(3)))
        )

        private val wholeGraphicOptimizations = listOf(
                CollapseGroups(),
                RemoveEmptyGroups(),
                MergePaths()
        )
    }
}